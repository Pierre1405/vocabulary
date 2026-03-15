#!/usr/bin/env python3
"""
Script pour nettoyer les fichiers générés dans le répertoire `generated`.
"""

import os
import glob
import argparse

def clean_generated():
    """
    Supprime les fichiers générés (chunks, TSV avec IDs, base de données).
    """
    # Supprimer les chunks
    chunk_files = glob.glob("generated/step 1 chunk/chunk_*.tsv")
    for chunk_file in chunk_files:
        if os.path.exists(chunk_file):
            print(f"Suppression du fichier : {chunk_file}")
            os.remove(chunk_file)
            print(f"Fichier supprimé : {chunk_file}")
        else:
            print(f"Fichier non trouvé : {chunk_file}")
    
    # Supprimer le fichier TSV avec IDs
    tsv_file = "generated/step 0.5 add_id/Sentence pairs with ID - 2026-03-13.tsv"
    if os.path.exists(tsv_file):
        print(f"Suppression du fichier : {tsv_file}")
        os.remove(tsv_file)
        print(f"Fichier supprimé : {tsv_file}")
    else:
        print(f"Fichier non trouvé : {tsv_file}")
    
    # Supprimer la base de données
    db_file = "app/src/main/assets/vocabulary.db"
    if os.path.exists(db_file):
        print(f"Suppression du fichier : {db_file}")
        os.remove(db_file)
        print(f"Fichier supprimé : {db_file}")
    else:
        print(f"Fichier non trouvé : {db_file}")
    
    print("Nettoyage terminé.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Nettoyer les fichiers générés.')
    parser.add_argument('--confirm', action='store_true', 
                        help='Confirmer la suppression des fichiers.')
    
    args = parser.parse_args()
    
    if args.confirm:
        clean_generated()
    else:
        print("Utilisez --confirm pour supprimer les fichiers générés.")
        print("Exemple : python clean_generated.py --confirm")
