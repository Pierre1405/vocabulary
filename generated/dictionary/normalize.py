"""
normalize.py — Normalise le fichier JSON intermédiaire produit par extract.py.

Usage:
    python normalize.py --input <path> --locale <de|fr> --output <path>

Transformations :
    - Pronoms : extrait depuis le champ `pronouns` (DE) ou depuis la forme (FR)
    - features : liste de tags jointe en string ("indicative,present")
    - sense_index : uniformisé en string
"""

import argparse
import json
import re

# Pronoms sujets français, ordonnés du plus long au plus court pour éviter
# les faux positifs (ex: "il" avant "il/elle/on")
FR_PRONOUNS = [
    "ils/elles", "il/elle/on",
    "je", "tu", "il", "elle", "on", "nous", "vous", "ils", "elles"
]
FR_PRONOUN_RE = re.compile(
    r"^(" + "|".join(re.escape(p) for p in FR_PRONOUNS) + r")\s+",
    re.IGNORECASE
)
# "j'" est collé au mot suivant sans espace (ex: "j'ai lu")
# Gère les deux types d'apostrophe : droit (U+0027) et typographique (U+2019)
FR_JELISION_RE = re.compile(r"^(j['\u2019])", re.IGNORECASE)
# Préfixe subjonctif : "que " ou "qu'" (ex: "que je lise", "qu'il/elle/on lise")
FR_QUE_RE = re.compile(r"^qu['\u2019e]\s*", re.IGNORECASE)


def extract_fr_pronoun(form):
    """
    Extrait le pronom sujet d'une forme française.
    Ex: "je lis"           -> ("je", "lis")
        "il/elle/on lit"   -> ("il/elle/on", "lit")
        "j'ai lu"          -> ("j'", "ai lu")
        "que je lise"      -> ("je", "lise")
        "qu'il/elle/on lise" -> ("il/elle/on", "lise")
        "lu"               -> (None, "lu")
    """
    # Cas subjonctif : strip "que " ou "qu'" puis extraction normale
    que_match = FR_QUE_RE.match(form)
    if que_match:
        form = form[que_match.end():]

    # Cas "j'" collé sans espace
    match = FR_JELISION_RE.match(form)
    if match:
        return match.group(1), form[match.end():]

    match = FR_PRONOUN_RE.match(form)
    if match:
        return match.group(1), form[match.end():]

    return None, form


def normalize_form_de(raw_form):
    """Normalise une forme allemande (pronoms dans un champ séparé)."""
    tags = raw_form.get("tags", [])
    pronouns = raw_form.get("pronouns", None)
    return {
        "form": raw_form.get("form", ""),
        "features": ",".join(tags),
        "pronouns": ",".join(pronouns) if pronouns else None
    }


def normalize_form_fr(raw_form):
    """Normalise une forme française (pronoms extraits de la forme)."""
    tags = raw_form.get("tags", [])
    form = raw_form.get("form", "")
    pronoun, clean_form = extract_fr_pronoun(form)
    return {
        "form": clean_form,
        "features": ",".join(tags),
        "pronouns": pronoun
    }


def normalize_translation(raw_translation):
    """
    Normalise une traduction :
    - sense_index uniformisé en string
    - gloss_source résolu :
        - de-extract : déjà résolu via senses[].glosses dans extract.py
        - fr-extract : champ `sense` de la traduction (gloss en français)
    """
    sense_index = raw_translation.get("sense_index")

    # Priorité 1 : gloss_source déjà résolu par extract.py (de-extract)
    gloss_source = raw_translation.get("gloss_source")

    # Priorité 2 : champ `sense` brut de la traduction (fr-extract)
    if not gloss_source:
        gloss_source = raw_translation.get("sense")

    return {
        "text": raw_translation.get("text", ""),
        "target_locale": raw_translation.get("target_locale", ""),
        "sense_index": str(sense_index) if sense_index is not None else None,
        "gloss_source": gloss_source,
        "example": raw_translation.get("example")
    }


def normalize_entry(entry, locale):
    """Normalise une entrée complète."""
    normalize_form = normalize_form_de if locale == "de" else normalize_form_fr

    forms = [normalize_form(f) for f in entry.get("forms", [])]
    translations = [normalize_translation(t) for t in entry.get("translations", [])]

    return {
        "lemma": entry.get("lemma", ""),
        "locale": entry.get("locale", locale),
        "pos": entry.get("pos"),
        "gender": entry.get("gender"),
        "forms": forms,
        "translations": translations,
        "best_example": entry.get("best_example")
    }


def main():
    parser = argparse.ArgumentParser(description="Normalise un fichier JSON intermédiaire du dictionnaire.")
    parser.add_argument("--input",  required=True, help="Fichier JSON intermédiaire (sortie de extract.py)")
    parser.add_argument("--locale", required=True, choices=["de", "fr"], help="Langue du dump source")
    parser.add_argument("--output", required=True, help="Fichier JSON normalisé en sortie")
    args = parser.parse_args()

    print(f"Lecture de {args.input} ...")
    with open(args.input, encoding="utf-8") as f:
        entries = json.load(f)

    print(f"{len(entries)} entrees a normaliser...")
    normalized = [normalize_entry(e, args.locale) for e in entries]

    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(normalized, f, ensure_ascii=False, indent=2)

    print(f"Termine : {len(normalized)} entrees normalisees -> {args.output}")

    # Apercu
    print("\n--- Apercu (3 premieres entrees) ---")
    for entry in normalized[:3]:
        print(f"\n  lemma: {entry['lemma']} | pos: {entry['pos']} | locale: {entry['locale']}")
        for form in entry["forms"][:3]:
            print(f"    form: {form['form']!r:20} features: {form['features']:30} pronouns: {form['pronouns']}")
        for t in entry["translations"][:2]:
            print(f"    -> {t['target_locale']}: {t['text']!r:20} gloss: {str(t['gloss_source'])[:50]}")


if __name__ == "__main__":
    main()
