"""
extract.py — Extrait les entrées d'un dump kaikki.org (JSONL gzippé).

Usage:
    python extract.py --dump <path_or_url> --locale <de|fr> --target <fr|de> --output <path>

Exemple:
    python extract.py --dump https://kaikki.org/dictionary/downloads/de/de-extract.jsonl.gz --locale de --target fr --output extract_de.json
    python extract.py --dump https://kaikki.org/dictionary/downloads/fr/fr-extract.jsonl.gz --locale fr --target de --output extract_fr.json
"""

import argparse
import gzip
import json
import urllib.request
from pathlib import Path


def load_forms_config(locale, pos):
    """Charge les combinaisons de tags essentiels pour une locale et un POS donnés."""
    config_path = Path(__file__).parent / "forms_config.json"
    with open(config_path, encoding="utf-8") as f:
        config = json.load(f)
    locale_config = config.get(locale, {})
    # Retire les clés commençant par _ (commentaires)
    return [
        set(tags)
        for key, tags_list in locale_config.items()
        if not key.startswith("_")
        for tags in (tags_list if key == pos else [])
    ]


EXCLUDED_TAGS = {"processual-passive", "multiword-construction", "anterior", "predicative",
                 "pluperfect", "future-i", "future-ii", "statal-passive"}
ACCEPTED_POS  = {"noun", "verb", "adj", "adv", "pron", "prep", "conj", "intj", "det", "num", "particle"}

def is_essential_form(form_tags, essential_tag_sets):
    """Retourne True si la forme contient tous les tags d'au moins un groupe essentiel,
    et ne contient aucun tag exclu (voix passive DE, formes analytiques FR)."""
    form_tag_set = set(form_tags)
    if form_tag_set & EXCLUDED_TAGS:
        return False
    return any(essential <= form_tag_set for essential in essential_tag_sets)


def extract_forms(entry, locale, pos):
    """Extrait les formes essentielles d'une entrée."""
    essential_tag_sets = load_forms_config(locale, pos)
    if not essential_tag_sets:
        return []

    result = []
    for form in entry.get("forms", []):
        tags = form.get("tags", [])
        if is_essential_form(tags, essential_tag_sets):
            result.append({
                "form": form.get("form", ""),
                "tags": tags,
                "pronouns": form.get("pronouns")
            })
    return result


def extract_gender(entry):
    """Extrait le genre depuis les tags de l'entrée."""
    tags = entry.get("tags", [])
    for tag in tags:
        if tag in ("masculine", "feminine", "neuter"):
            return tag
    return None


def build_sense_gloss_map(entry):
    """
    Construit un mapping sense_index -> gloss depuis les senses de l'entrée.
    Utilisé pour associer le bon gloss à chaque traduction.
    """
    sense_map = {}
    for sense in entry.get("senses", []):
        idx = sense.get("sense_index")
        glosses = sense.get("glosses", [])
        if idx is not None and glosses:
            sense_map[str(idx)] = glosses[0]
    return sense_map


def build_sense_example_map(entry):
    """
    Construit un mapping sense_index -> premier exemple depuis les senses de l'entrée.
    Retourne aussi le meilleur exemple global (premier disponible, pour le fallback).
    """
    example_map = {}
    best_example = None
    for sense in entry.get("senses", []):
        idx = sense.get("sense_index")
        for ex in sense.get("examples", []):
            text = ex.get("text", "").strip()
            if text:
                if best_example is None:
                    best_example = text
                if idx is not None:
                    example_map.setdefault(str(idx), text)
                break
    return example_map, best_example


def extract_translations(entry, target_locale):
    """
    Extrait les traductions vers la langue cible avec les données brutes.
    La résolution du gloss_source est déléguée à normalize.py.
    Retourne (translations, best_example) où best_example est le fallback global.
    """
    sense_gloss_map = build_sense_gloss_map(entry)
    sense_example_map, best_example = build_sense_example_map(entry)
    result = []

    for t in entry.get("translations", []):
        if t.get("lang_code") != target_locale:
            continue

        word = t.get("word")
        if not word:
            continue

        sense_index = t.get("sense_index")
        gloss_source = sense_gloss_map.get(str(sense_index)) if sense_index is not None else None
        example = sense_example_map.get(str(sense_index)) if sense_index is not None else None

        result.append({
            "text": word,
            "target_locale": target_locale,
            "sense_index": sense_index,
            "sense": t.get("sense"),         # champ brut fr-extract
            "gloss_source": gloss_source,    # résolu via senses[] pour de-extract
            "example": example               # exemple lié au sens via sense_index
        })

    return result, best_example


def open_dump(path_or_url):
    """Ouvre un dump local ou distant (gzippé)."""
    if path_or_url.startswith("http"):
        print(f"Streaming depuis {path_or_url} ...")
        response = urllib.request.urlopen(path_or_url)
        return gzip.GzipFile(fileobj=response)
    else:
        print(f"Lecture de {path_or_url} ...")
        return gzip.open(path_or_url, "rb")


def main():
    parser = argparse.ArgumentParser(description="Extrait les entrees d'un dump kaikki.org.")
    parser.add_argument("--dump",   required=True, help="URL ou chemin vers le dump .jsonl.gz")
    parser.add_argument("--locale", required=True, choices=["de", "fr"], help="Langue a extraire")
    parser.add_argument("--target", required=True, choices=["de", "fr"], help="Langue cible des traductions")
    parser.add_argument("--output", required=True, help="Fichier JSON de sortie")
    parser.add_argument("--limit",  type=int, default=None, help="Limite du nombre d'entrees extraites (debug)")
    args = parser.parse_args()

    entries = []
    total_read = 0
    total_skipped = 0

    with open_dump(args.dump) as f:
        for line in f:
            total_read += 1

            if total_read % 10000 == 0:
                print(f"  {total_read} lignes lues, {len(entries)} entrees extraites...")

            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                total_skipped += 1
                continue

            if entry.get("lang_code") != args.locale:
                continue

            pos = entry.get("pos")
            if pos not in ACCEPTED_POS:
                continue

            translations, best_example = extract_translations(entry, args.target)
            if not translations:
                continue

            forms = extract_forms(entry, args.locale, pos)

            entries.append({
                "lemma": entry.get("word", ""),
                "locale": args.locale,
                "pos": pos,
                "gender": extract_gender(entry),
                "forms": forms,
                "translations": translations,
                "best_example": best_example  # fallback si aucun sens résolu
            })

            if args.limit and len(entries) >= args.limit:
                print(f"  Limite de {args.limit} entrees atteinte.")
                break

    print(f"\nTermine : {total_read} lignes lues, {len(entries)} entrees extraites, {total_skipped} ignorees.")

    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(entries, f, ensure_ascii=False, indent=2)

    print(f"Sauvegarde -> {args.output}")

    # Apercu
    print("\n--- Apercu (3 premieres entrees) ---")
    for entry in entries[:3]:
        print(f"\n  lemma: {entry['lemma']} | pos: {entry['pos']} | gender: {entry['gender']}")
        for form in entry["forms"][:3]:
            print(f"    form: {form['form']!r:25} tags: {form['tags']}")
        for t in entry["translations"][:2]:
            print(f"    -> {t['target_locale']}: {t['text']!r:20} gloss: {str(t['gloss_source'])[:60]}")


if __name__ == "__main__":
    main()
