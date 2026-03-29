#!/usr/bin/env python3
"""
Script pour nettoyer les fichiers générés dans le répertoire `generated`.
"""

import os
import sys
import glob
import argparse

sys.stdout.reconfigure(line_buffering=True)

def clean_generated():
    """
    Supprime les fichiers générés (chunks, TSV, base de données, fichiers audio).
    """
    # Supprimer les fichiers texte traduits
    text_files = glob.glob("generated/content/step 1 translation/*.txt")
    for text_file in text_files:
        if os.path.exists(text_file):
            print(f"Suppression du fichier : {text_file}")
            os.remove(text_file)
            print(f"Fichier supprimé : {text_file}")
        else:
            print(f"Fichier non trouvé : {text_file}")
    
    # Supprimer les fichiers audio
    audio_files = glob.glob("app/src/main/res/raw/sentence_*.mp3")
    for audio_file in audio_files:
        if os.path.exists(audio_file):
            print(f"Suppression du fichier : {audio_file}")
            os.remove(audio_file)
            print(f"Fichier supprimé : {audio_file}")
        else:
            print(f"Fichier non trouvé : {audio_file}")
    
    # Supprimer les chunks
    chunk_files = glob.glob("generated/content/step 3 chunk/chunk_*.tsv")
    for chunk_file in chunk_files:
        if os.path.exists(chunk_file):
            print(f"Suppression du fichier : {chunk_file}")
            os.remove(chunk_file)
            print(f"Fichier supprimé : {chunk_file}")
        else:
            print(f"Fichier non trouvé : {chunk_file}")
    
    # Supprimer le fichier TSV généré
    tsv_file = "generated/content/step 2 generate_tsv/Sentence pairs with ID.tsv"
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
