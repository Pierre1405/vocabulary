"""
Explore le dump français de kaikki.org :
1. Récupère 5 entrées françaises avec traductions vers DE
2. Cherche spécifiquement "aimer" pour comparer avec "lieben" du dump DE
3. Affiche les sense_index des deux côtés pour valider la cohérence
"""

import urllib.request
import gzip
import json

URL = "https://kaikki.org/dictionary/downloads/fr/fr-extract.jsonl.gz"
OUTPUT = "generated/dictionary/sample_fr.json"
TARGET = 5  # entrées fr avec traductions vers DE

print(f"Streaming depuis {URL} ...")
print(f"Recherche de {TARGET} entrées françaises avec traductions vers DE...\n")

entries_with_de = []
aimer = None
total_read = 0

with urllib.request.urlopen(URL) as response:
    with gzip.GzipFile(fileobj=response) as f:
        for line in f:
            total_read += 1
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue

            if entry.get("lang_code") != "fr":
                continue

            translations = entry.get("translations", [])
            de_translations = [t for t in translations if t.get("lang_code") == "de"]

            if entry.get("word") == "aimer":
                aimer = entry
                print(f"  'aimer' trouvé à la ligne {total_read}")

            if de_translations and len(entries_with_de) < TARGET:
                entries_with_de.append(entry)
                print(f"  [{len(entries_with_de)}/{TARGET}] {entry.get('word')} ({entry.get('pos')})")

            if len(entries_with_de) >= TARGET and aimer:
                break

            if total_read % 10000 == 0:
                print(f"  ... {total_read} lignes lues")

print(f"\n{total_read} lignes lues au total.")

# Sauvegarde
output = {"entries_with_de": entries_with_de, "aimer": aimer}
with open(OUTPUT, "w", encoding="utf-8") as f:
    json.dump(output, f, ensure_ascii=False, indent=2)
print(f"Sauvegardé dans {OUTPUT}")

# --- Analyse des sense_index ---
print("\n=== Analyse des sense_index : 'aimer' (fr-extract) ===")
if aimer:
    senses = aimer.get("senses", [])
    for s in senses:
        idx = s.get("sense_index", "?")
        gloss = s.get("glosses", ["(pas de gloss)"])[0][:80]
        print(f"  sense_index {idx} : {gloss}")

    print("\n  Traductions vers DE :")
    for t in aimer.get("translations", []):
        if t.get("lang_code") == "de":
            print(f"    sense_index {t.get('sense_index', '?')} -> {t.get('word')}")
else:
    print("  'aimer' non trouvé dans cet échantillon.")

print("\n=== Rappel : 'lieben' (de-extract) ===")
print("  Charger sample_de_verbs.json pour comparer...")
try:
    with open("generated/dictionary/sample_de_verbs.json", encoding="utf-8") as f:
        verbs = json.load(f)
    lieben = next((v for v in verbs if v["word"] == "lieben"), None)
    if lieben:
        print("  Senses de 'lieben' :")
        for s in lieben.get("senses", []):
            idx = s.get("sense_index", "?")
            gloss = s.get("glosses", ["(pas de gloss)"])[0][:80]
            print(f"    sense_index {idx} : {gloss}")
        print("  Traductions vers FR :")
        for t in lieben.get("translations", []):
            if t.get("lang_code") == "fr":
                print(f"    sense_index {t.get('sense_index', '?')} -> {t.get('word')}")
except FileNotFoundError:
    print("  sample_de_verbs.json non trouvé.")
