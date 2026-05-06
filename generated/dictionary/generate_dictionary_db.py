"""
generate_dictionary_db.py — Fusionne les fichiers normalisés et génère dictionary.db.

Usage:
    python generate_dictionary_db.py --inputs <normalized_de.json> <normalized_fr.json> --output <dictionary.db>

Exemple:
    python generate_dictionary_db.py --inputs normalized_de.json normalized_fr.json --output dictionary.db
"""

import argparse
import json
import sqlite3
from pathlib import Path

FORMS_CONFIG_PATH = Path(__file__).parent / "forms_config.json"


def load_locale_configs() -> dict[str, dict]:
    """Retourne { locale: { skip_combinations_global, skip_tags_by_pos, groups, pronoun_canonical } }"""
    with open(FORMS_CONFIG_PATH, encoding="utf-8") as f:
        config = json.load(f)
    result = {}
    for locale, data in config.items():
        if locale.startswith("_"):
            continue
        groups_by_pos = {}
        for pos, entries in data.get("groups", {}).items():
            groups_by_pos[pos] = [
                {"key": e["key"], "tags": set(e["tags"]), "exclude_tags": set(e.get("exclude_tags", []))}
                for e in entries
            ]
        aux_det = data.get("auxiliary_detection", {})
        result[locale] = {
            "skip_combinations_global": [
                set(combo) for combo in data.get("skip_tag_combinations", [])
            ],
            "skip_tags_by_pos": {
                pos: set(tags)
                for pos, tags in data.get("skip_tags_by_pos", {}).items()
            },
            "groups_by_pos": groups_by_pos,
            "pronoun_canonical": data.get("pronoun_canonical", {}),
            "etre_forms": set(aux_det.get("être_forms", [])),
        }
    return result


def detect_auxiliary(forms_data: list[dict], etre_forms: set) -> str | None:
    """Détecte l'auxiliaire depuis les formes composées (perfect). Retourne 'être', 'avoir' ou None."""
    if not etre_forms:
        return None
    for f in forms_data:
        features = f.get("features") or ""
        if "perfect" not in features.split(","):
            continue
        form = f.get("form") or ""
        first_word = form.split(" ", 1)[0] if " " in form else ""
        if first_word in etre_forms:
            return "être"
    return "avoir"


def assign_group_key(features: str, groups: list[dict]) -> str | None:
    """Retourne la clé du premier groupe dont tous les tags sont présents dans features et aucun exclude_tag."""
    tags = set(features.split(","))
    for group in groups:
        if group["tags"].issubset(tags) and not (group.get("exclude_tags", set()) & tags):
            return group["key"]
    return None


def should_skip(features: str, skip_combinations: list[set]) -> bool:
    tags = set(features.split(","))
    return any(combo.issubset(tags) for combo in skip_combinations)


SCHEMA = """
CREATE TABLE IF NOT EXISTS dict_entry (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    lemma   TEXT NOT NULL,
    locale  TEXT NOT NULL,
    pos     TEXT,
    gender  TEXT,
    example TEXT
);

CREATE TABLE IF NOT EXISTS dict_features (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    features TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS dict_translation (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    entry_id      INTEGER NOT NULL REFERENCES dict_entry(id),
    target_locale TEXT NOT NULL,
    text          TEXT NOT NULL,
    gloss_source  TEXT,
    example       TEXT
);

CREATE TABLE IF NOT EXISTS dict_form (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    entry_id    INTEGER NOT NULL REFERENCES dict_entry(id),
    form        TEXT NOT NULL,
    features_id INTEGER REFERENCES dict_features(id),
    pronouns    TEXT,
    group_key   TEXT
);

CREATE INDEX IF NOT EXISTS idx_entry_lemma    ON dict_entry(lemma, locale);
CREATE INDEX IF NOT EXISTS idx_form_form      ON dict_form(form);
CREATE INDEX IF NOT EXISTS idx_trans_entry    ON dict_translation(entry_id);
"""


def get_or_create_features(cursor, features_cache, features):
    """Retourne l'id de la features string, en la créant si nécessaire."""
    if features is None:
        return None
    if features in features_cache:
        return features_cache[features]
    cursor.execute("INSERT OR IGNORE INTO dict_features (features) VALUES (?)", (features,))
    cursor.execute("SELECT id FROM dict_features WHERE features = ?", (features,))
    fid = cursor.fetchone()[0]
    features_cache[features] = fid
    return fid


def insert_entry(cursor, entry):
    cursor.execute(
        "INSERT INTO dict_entry (lemma, locale, pos, gender) VALUES (?, ?, ?, ?)",
        (entry["lemma"], entry["locale"], entry.get("pos"), entry.get("gender"))
    )
    return cursor.lastrowid


def insert_translations(cursor, entry_id, translations, best_example):
    """
    Insère les traductions avec leurs exemples.
    Si aucune traduction n'a un exemple précis (via sense_index), on utilise le fallback :
    - best_example est stocké dans dict_entry (une seule fois)
    - dict_translation.example reste NULL pour toutes les traductions
    """
    has_precise_example = any(t.get("example") for t in translations if t.get("text"))

    for t in translations:
        if not t.get("text"):
            continue
        # En mode fallback, on ne duplique pas l'exemple dans chaque traduction
        example = t.get("example") if has_precise_example else None
        cursor.execute(
            "INSERT INTO dict_translation (entry_id, target_locale, text, gloss_source, example) VALUES (?, ?, ?, ?, ?)",
            (entry_id, t["target_locale"], t["text"], t.get("gloss_source"), example)
        )

    if not has_precise_example and best_example:
        cursor.execute(
            "UPDATE dict_entry SET example = ? WHERE id = ?",
            (best_example, entry_id)
        )


def insert_forms(cursor, features_cache, entry_id, forms, lemma, skip_global, skip_pos_tags, groups, pronoun_canonical):
    seen = set()
    for f in forms:
        form = f.get("form")
        if not form:
            continue
        features = f.get("features") or ""
        pronouns = f.get("pronouns")

        if form.lower() == lemma.lower() and not pronouns:
            continue

        # Global skips (archaic, outdated, perfect tenses) always apply
        if should_skip(features, skip_global):
            continue

        # Extract pronoun from multi-word forms before pos-specific skip check
        extracted_pronoun = False
        if " " in form:
            has_person = any(p in features for p in ("first-person", "second-person", "third-person"))
            if has_person:
                parts = form.split(" ", 1)
                form = parts[1]
                if not pronouns:
                    pronouns = parts[0]
                extracted_pronoun = True

        # Pos-specific tag skips only for non-extracted forms (clean alternatives always exist)
        if not extracted_pronoun and should_skip(features, skip_pos_tags):
            continue

        # Normalize pronoun to canonical label
        if pronouns:
            pronouns = pronouns.replace('\ufffd', "'")
            pronouns = pronoun_canonical.get(pronouns, pronouns)

        # Assign group key
        group_key = assign_group_key(features, groups)

        key = (form, features or None, pronouns)
        if key in seen:
            continue
        seen.add(key)
        features_id = get_or_create_features(cursor, features_cache, features or None)
        cursor.execute(
            "INSERT INTO dict_form (entry_id, form, features_id, pronouns, group_key) VALUES (?, ?, ?, ?, ?)",
            (entry_id, form, features_id, pronouns, group_key)
        )


def main():
    parser = argparse.ArgumentParser(description="Genere dictionary.db depuis les fichiers normalises.")
    parser.add_argument("--inputs", nargs="+", required=True, help="Fichiers JSON normalises")
    parser.add_argument("--output", required=True, help="Fichier SQLite de sortie")
    args = parser.parse_args()

    output_path = Path(args.output)
    if output_path.exists():
        output_path.unlink()
        print(f"Ancien fichier {args.output} supprime.")

    locale_configs = load_locale_configs()

    conn = sqlite3.connect(args.output)
    conn.executescript(SCHEMA)
    cursor = conn.cursor()
    features_cache = {}

    total_entries = 0
    total_forms = 0
    total_translations = 0

    for input_file in args.inputs:
        print(f"\nTraitement de {input_file} ...")
        with open(input_file, encoding="utf-8") as f:
            entries = json.load(f)

        for entry in entries:
            locale = entry.get("locale", "")
            pos = entry.get("pos", "")
            lc = locale_configs.get(locale, {})
            skip_global = lc.get("skip_combinations_global", [])
            skip_pos_tags = [{t} for t in lc.get("skip_tags_by_pos", {}).get(pos, set())]
            groups = lc.get("groups_by_pos", {}).get(pos, [])
            pronoun_canonical = lc.get("pronoun_canonical", {})
            entry_id = insert_entry(cursor, entry)
            insert_translations(cursor, entry_id, entry.get("translations", []), entry.get("best_example"))
            insert_forms(cursor, features_cache, entry_id, entry.get("forms", []), entry.get("lemma", ""), skip_global, skip_pos_tags, groups, pronoun_canonical)
            if pos == "verb":
                etre_forms = lc.get("etre_forms", set())
                auxiliary = detect_auxiliary(entry.get("forms", []), etre_forms)
                if auxiliary:
                    cursor.execute(
                        "INSERT INTO dict_form (entry_id, form, features_id, pronouns, group_key) VALUES (?, ?, ?, ?, ?)",
                        (entry_id, auxiliary, None, None, "auxiliary")
                    )

            total_entries += 1
            total_forms += len(entry.get("forms", []))
            total_translations += len(entry.get("translations", []))

        print(f"  {len(entries)} entrees traitees.")

    conn.commit()

    # Stats réelles depuis la DB
    c = conn.cursor()
    c.execute("SELECT COUNT(*) FROM dict_form")
    actual_forms = c.fetchone()[0]
    c.execute("SELECT COUNT(*) FROM dict_features")
    unique_features = c.fetchone()[0]
    conn.close()

    size_kb = output_path.stat().st_size / 1024
    print(f"\nTermine :")
    print(f"  {total_entries} entrees")
    print(f"  {total_translations} traductions")
    print(f"  {actual_forms} formes inserees ({unique_features} features uniques)")
    print(f"  Taille : {size_kb:.1f} KB -> {args.output}")

    # Verification rapide
    conn = sqlite3.connect(args.output)
    cursor = conn.cursor()
    print("\n--- Verification ---")
    cursor.execute("SELECT e.lemma, f.form, ft.features, f.pronouns FROM dict_form f JOIN dict_entry e ON e.id = f.entry_id LEFT JOIN dict_features ft ON ft.id = f.features_id LIMIT 5")
    for row in cursor.fetchall():
        print(f"  {row[0]} | {row[1]} | {row[2]} | pronouns: {row[3]}")
    conn.close()


if __name__ == "__main__":
    main()
