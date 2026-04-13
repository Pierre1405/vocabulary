"""
Récupère les 20 premières entrées allemandes de type 'verb' depuis le dump kaikki.org.
"""

import urllib.request
import gzip
import json

URL = "https://kaikki.org/dictionary/downloads/de/de-extract.jsonl.gz"
OUTPUT = "sample_de_verbs.json"
TARGET = 20

print(f"Streaming depuis {URL} ...")
print(f"Recherche des {TARGET} premiers verbes allemands...\n")

verbs = []
total_read = 0

with urllib.request.urlopen(URL) as response:
    with gzip.GzipFile(fileobj=response) as f:
        for line in f:
            total_read += 1
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue

            if entry.get("lang_code") == "de" and entry.get("pos") == "verb":
                verbs.append(entry)
                print(f"  [{len(verbs)}/{TARGET}] {entry.get('word')}")
                if len(verbs) >= TARGET:
                    break

            if total_read % 10000 == 0:
                print(f"  ... {total_read} lignes lues, {len(verbs)} verbes trouvés")

print(f"\n{len(verbs)} verbes trouvés après {total_read} lignes.")

with open(OUTPUT, "w", encoding="utf-8") as f:
    json.dump(verbs, f, ensure_ascii=False, indent=2)

print(f"Sauvegardé dans {OUTPUT}")

# Analyse des champs forms sur les verbes
print("\n--- Analyse des 'forms' sur les verbes ---")
for v in verbs[:3]:
    print(f"\nverbe: {v.get('word')}")
    forms = v.get("forms", [])
    print(f"  {len(forms)} formes :")
    for form in forms[:10]:
        tags = ", ".join(form.get("tags", []))
        print(f"    {form.get('form')!r:25} [{tags}]")
    if len(forms) > 10:
        print(f"    ... ({len(forms) - 10} formes supplémentaires)")
