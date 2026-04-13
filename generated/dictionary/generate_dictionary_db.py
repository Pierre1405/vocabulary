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
    pronouns    TEXT
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


def insert_forms(cursor, features_cache, entry_id, forms, lemma):
    seen = set()
    for f in forms:
        form = f.get("form")
        if not form:
            continue
        # Forme identique au lemme : inutile pour le lookup
        if form.lower() == lemma.lower():
            continue
        features = f.get("features") or ""
        pronouns = f.get("pronouns")
        # Exclure les formes multi-mots redondantes :
        # - pronoms sujets intégrés (doublons : "ich aale", "wir aalen")
        # - temps composés analytiques (futur antérieur FR : "aurai lu")
        if " " in form and any(p in features for p in ("first-person", "second-person", "third-person", "perfect")):
            continue
        features = features or None
        # Dédoublonnage par (form, features, pronouns) pour conserver toutes les combinaisons
        key = (form, features, pronouns)
        if key in seen:
            continue
        seen.add(key)
        features_id = get_or_create_features(cursor, features_cache, features)
        cursor.execute(
            "INSERT INTO dict_form (entry_id, form, features_id, pronouns) VALUES (?, ?, ?, ?)",
            (entry_id, form, features_id, pronouns)
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
            entry_id = insert_entry(cursor, entry)
            insert_translations(cursor, entry_id, entry.get("translations", []), entry.get("best_example"))
            insert_forms(cursor, features_cache, entry_id, entry.get("forms", []), entry.get("lemma", ""))

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
