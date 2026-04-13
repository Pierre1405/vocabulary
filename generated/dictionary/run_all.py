"""
run_all.py — Lance le pipeline complet de génération de dictionary.db.

Usage:
    python run_all.py

Étapes :
    1. extract.py  (DE + FR en parallèle)
    2. normalize.py (DE + FR en parallèle)
    3. generate_dictionary_db.py
"""

import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

BASE = Path(__file__).parent
SOURCE = BASE / "source"


def run(label, cmd):
    """Exécute une commande et affiche sa sortie en temps réel."""
    print(f"\n[{label}] Démarrage...")
    start = time.time()
    result = subprocess.run(
        [sys.executable] + cmd,
        cwd=BASE,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace"
    )
    elapsed = time.time() - start
    if result.returncode != 0:
        print(f"[{label}] ERREUR ({elapsed:.0f}s)")
        print(result.stderr)
        sys.exit(1)
    # Affiche uniquement les lignes de résumé (Termine / Taille / Total)
    for line in result.stdout.splitlines():
        if any(k in line for k in ("Termine", "Taille", "entrees", "formes", "lignes lues", "ERREUR")):
            print(f"[{label}] {line.strip()}")
    print(f"[{label}] OK ({elapsed:.0f}s)")
    return label


def run_parallel(tasks):
    """Exécute plusieurs tâches en parallèle."""
    with ThreadPoolExecutor() as executor:
        futures = {executor.submit(run, label, cmd): label for label, cmd in tasks}
        for future in as_completed(futures):
            future.result()  # propage les exceptions


def main():
    total_start = time.time()

    # Vérification des dumps
    for dump in (SOURCE / "de-extract.jsonl.gz", SOURCE / "fr-extract.jsonl.gz"):
        if not dump.exists():
            print(f"ERREUR : dump introuvable : {dump}")
            sys.exit(1)

    print("=" * 60)
    print("  Pipeline dictionary.db")
    print("=" * 60)

    # Étape 1 — Extraction (parallèle)
    print("\n--- Étape 1 : Extraction ---")
    run_parallel([
        ("extract DE", ["extract.py",
            "--dump",   str(SOURCE / "de-extract.jsonl.gz"),
            "--locale", "de",
            "--target", "fr",
            "--output", "extract_de.json"]),
        ("extract FR", ["extract.py",
            "--dump",   str(SOURCE / "fr-extract.jsonl.gz"),
            "--locale", "fr",
            "--target", "de",
            "--output", "extract_fr.json"]),
    ])

    # Étape 2 — Normalisation (parallèle)
    print("\n--- Étape 2 : Normalisation ---")
    run_parallel([
        ("normalize DE", ["normalize.py",
            "--input",  "extract_de.json",
            "--locale", "de",
            "--output", "normalized_de.json"]),
        ("normalize FR", ["normalize.py",
            "--input",  "extract_fr.json",
            "--locale", "fr",
            "--output", "normalized_fr.json"]),
    ])

    # Étape 3 — Génération de la DB
    print("\n--- Étape 3 : Génération dictionary.db ---")
    run("generate DB", ["generate_dictionary_db.py",
        "--inputs", "normalized_de.json", "normalized_fr.json",
        "--output", "dictionary.db"])

    total = time.time() - total_start
    print(f"\n{'=' * 60}")
    print(f"  Terminé en {total/60:.1f} min -> dictionary.db")
    print(f"{'=' * 60}")


if __name__ == "__main__":
    main()
