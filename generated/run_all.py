#!/usr/bin/env python3
"""
Script pour exécuter tous les scripts de génération dans l'ordre.
"""

import subprocess
import sys
import os

def run_script(script_path, description):
    """
    Exécute un script Python et affiche son résultat.
    
    Args:
        script_path (str): Chemin vers le script Python.
        description (str): Description de l'étape.
    """
    print(f"\n{'='*60}")
    print(f"Étape : {description}")
    print(f"{'='*60}")
    
    try:
        result = subprocess.run([sys.executable, script_path], check=True, text=True, capture_output=True)
        print(result.stdout)
        if result.stderr:
            print(f"Avertissements : {result.stderr}")
    except subprocess.CalledProcessError as e:
        print(f"Erreur lors de l'exécution du script : {script_path}")
        print(f"Erreur : {e.stderr}")
        sys.exit(1)

def main():
    """
    Exécute tous les scripts de génération dans l'ordre.
    """
    print("Démarrage du processus de génération...")
    
    # Chemin vers les scripts
    scripts = [
        ("generated/step 0.5 add_id/add_id_column.py", "Ajout des IDs aux phrases"),
        ("generated/step 1 chunk/split_tsv.py", "Division en chunks"),
        ("generated/step 2 sqlite/generate_sqlite.py", "Génération de la base de données SQLite")
    ]
    
    # Exécuter chaque script
    for script_path, description in scripts:
        run_script(script_path, description)
    
    print("\n" + "="*60)
    print("Tous les scripts ont été exécutés avec succès !")
    print("="*60)

if __name__ == "__main__":
    main()
