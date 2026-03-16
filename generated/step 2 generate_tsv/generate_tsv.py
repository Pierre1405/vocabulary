#!/usr/bin/env python3
"""
Script pour générer un fichier TSV à partir de fichiers texte.
"""

import os
import argparse

# Chemins par défaut
DEFAULT_FR_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 0 source\\"
DEFAULT_DE_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 1 translation\\"
DEFAULT_OUTPUT_FILE = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 2 generate_tsv\\Sentence pairs with ID.tsv"

def generate_tsv_from_text_files(fr_dir, de_dir, output_file):
    """
    Génère un fichier TSV à partir de fichiers texte en français et en allemand.
    
    Args:
        fr_dir (str): Répertoire contenant les fichiers texte en français.
        de_dir (str): Répertoire contenant les fichiers texte en allemand.
        output_file (str): Chemin vers le fichier TSV de sortie.
    """
    # Créer le répertoire de sortie si nécessaire
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    
    # Lister les fichiers texte en français
    fr_files = [f for f in os.listdir(fr_dir) if f.endswith('_fr.txt')]
    
    with open(output_file, 'w', encoding='utf-8') as f_out:
        # Écrire l'en-tête du fichier TSV
        f_out.write("id\tfrancais\tallemand\tcategorie\tstory\n")
        
        # Initialiser le compteur d'ID
        id_counter = 1
        
        # Parcourir les fichiers texte en français
        for fr_file in fr_files:
            # Extraire la catégorie et le nom du fichier
            # Format attendu : category_nom_fr.txt
            parts = fr_file.replace('_fr.txt', '').split('_')
            if len(parts) >= 2:
                categorie = parts[0]
                nom = '_'.join(parts[1:])
            else:
                categorie = "inconnu"
                nom = fr_file.replace('_fr.txt', '')
            
            # Chemin des fichiers en français et en allemand
            fr_path = os.path.join(fr_dir, fr_file)
            de_file = fr_file.replace('_fr.txt', '_de.txt')
            de_path = os.path.join(de_dir, de_file)
            
            print(f"Traitement du fichier : {fr_file}")
            
            # Lire les fichiers en français et en allemand
            with open(fr_path, 'r', encoding='utf-8') as f_fr, open(de_path, 'r', encoding='utf-8') as f_de:
                fr_lines = f_fr.readlines()
                de_lines = f_de.readlines()
                
                # Parcourir les lignes
                for fr_line, de_line in zip(fr_lines, de_lines):
                    fr_text = fr_line.strip()
                    de_text = de_line.strip()
                    
                    if fr_text and de_text:  # Ignorer les lignes vides
                        # Diviser les lignes en phrases en utilisant .!? comme séparateurs
                        import re
                        fr_phrases = [p.strip() for p in re.split(r'(?<=[.!?])\s*', fr_text) if p.strip()]
                        de_phrases = [p.strip() for p in re.split(r'(?<=[.!?])\s*', de_text) if p.strip()]
                        
                        # Vérifier que le nombre de phrases correspond
                        if len(fr_phrases) == len(de_phrases):
                            for fr_phrase, de_phrase in zip(fr_phrases, de_phrases):
                                # Écrire la ligne dans le fichier TSV
                                f_out.write(f"{id_counter}\t{fr_phrase}\t{de_phrase}\t{categorie}\t{nom}\n")
                                id_counter += 1
                                print(f"Ligne ajoutée : {fr_phrase} -> {de_phrase}")
                        else:
                            # Si le nombre de phrases ne correspond pas, écrire la ligne complète
                            f_out.write(f"{id_counter}\t{fr_text}\t{de_text}\t{categorie}\t{nom}\n")
                            id_counter += 1
                            print(f"Ligne ajoutée : {fr_text} -> {de_text}")
    
    print(f"Fichier TSV généré avec succès. Fichier de sortie : {output_file}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Générer un fichier TSV à partir de fichiers texte.')
    parser.add_argument('--fr_dir', type=str, 
                        default=DEFAULT_FR_DIR,
                        help=f'Répertoire contenant les fichiers texte en français. Par défaut : {DEFAULT_FR_DIR}')
    parser.add_argument('--de_dir', type=str, 
                        default=DEFAULT_DE_DIR,
                        help=f'Répertoire contenant les fichiers texte en allemand. Par défaut : {DEFAULT_DE_DIR}')
    parser.add_argument('--output_file', type=str, 
                        default=DEFAULT_OUTPUT_FILE,
                        help=f'Chemin vers le fichier TSV de sortie. Par défaut : {DEFAULT_OUTPUT_FILE}')
    
    args = parser.parse_args()
    generate_tsv_from_text_files(args.fr_dir, args.de_dir, args.output_file)
