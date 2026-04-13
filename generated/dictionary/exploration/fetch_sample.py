"""
Télécharge les 1000 premières lignes du dump allemand de kaikki.org
et sauvegarde un échantillon JSON lisible pour inspecter la structure.
"""

import urllib.request
import gzip
import json
import os

URL = "https://kaikki.org/dictionary/downloads/de/de-extract.jsonl.gz"
OUTPUT_RAW = "sample_de.jsonl"
OUTPUT_PRETTY = "sample_de_pretty.json"
N_LINES = 1000

print(f"Téléchargement en streaming depuis {URL} ...")

entries = []
with urllib.request.urlopen(URL) as response:
    with gzip.GzipFile(fileobj=response) as f:
        for i, line in enumerate(f):
            if i >= N_LINES:
                break
            try:
                entries.append(json.loads(line))
            except json.JSONDecodeError:
                continue

print(f"{len(entries)} entrées récupérées.")

# Sauvegarde brute
with open(OUTPUT_RAW, "w", encoding="utf-8") as f:
    for entry in entries:
        f.write(json.dumps(entry, ensure_ascii=False) + "\n")

# Sauvegarde pretty (5 premières entrées complètes)
with open(OUTPUT_PRETTY, "w", encoding="utf-8") as f:
    json.dump(entries[:5], f, ensure_ascii=False, indent=2)

print(f"Fichiers générés :")
print(f"  {OUTPUT_RAW}  ({len(entries)} lignes)")
print(f"  {OUTPUT_PRETTY}  (5 premières entrées, format lisible)")

# Résumé des champs présents
print("\n--- Champs présents dans les 1000 entrées ---")
all_keys = {}
for e in entries:
    for k in e.keys():
        all_keys[k] = all_keys.get(k, 0) + 1

for k, count in sorted(all_keys.items(), key=lambda x: -x[1]):
    print(f"  {k}: {count}/{len(entries)}")

# Exemples d'entrées avec traductions vers fr
print("\n--- Exemples avec traduction vers 'fr' ---")
found = 0
for e in entries:
    translations = e.get("translations", [])
    fr_translations = [t for t in translations if t.get("lang_code") == "fr"]
    if fr_translations:
        print(f"\n  word: {e.get('word')} | pos: {e.get('pos')} | lang: {e.get('lang_code')}")
        for t in fr_translations[:3]:
            print(f"    → fr: {t.get('word', t.get('translation', '?'))}")
        found += 1
    if found >= 5:
        break

if found == 0:
    print("  Aucune entrée avec traduction fr dans cet échantillon.")
