#!/usr/bin/env python3
"""
Script pour traduire les phrases de fichiers texte en allemand.
Utilise l'API Google Cloud Translation.
"""

import os
import argparse
from google.cloud import translate_v2 as translate

# Chemins par défaut
DEFAULT_INPUT_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 0 source\\"
DEFAULT_OUTPUT_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 1 translation\\"

def translate_text(text, target_language="de"):
    """
    Traduit un texte vers la langue cible en utilisant Google Cloud Translation.
    
    Args:
        text (str): Texte à traduire.
        target_language (str): Langue cible (par défaut : "de" pour l'allemand).
    
    Returns:
        str: Texte traduit.
    """
    translate_client = translate.Client()
    result = translate_client.translate(text, target_language=target_language)
    return result['translatedText']

def translate_text_files(input_dir, output_dir):
    """
    Traduit les phrases de fichiers texte en allemand et génère des fichiers texte traduits.
    
    Args:
        input_dir (str): Répertoire contenant les fichiers texte d'entrée.
        output_dir (str): Répertoire de sortie pour les fichiers texte traduits.
    """
    # Créer le répertoire de sortie si nécessaire
    os.makedirs(output_dir, exist_ok=True)
    
    # Lister les fichiers texte dans le répertoire d'entrée
    text_files = [f for f in os.listdir(input_dir) if f.endswith('.txt')]
    
    # Parcourir les fichiers texte
    for text_file in text_files:
        input_path = os.path.join(input_dir, text_file)
        output_file = text_file.replace('_fr.txt', '_de.txt')  # Remplacer _fr par _de
        output_path = os.path.join(output_dir, output_file)
        
        print(f"Traitement du fichier : {text_file}")
        
        with open(input_path, 'r', encoding='utf-8') as f_in, open(output_path, 'w', encoding='utf-8') as f_out:
            for line in f_in:
                line = line.strip()
                if line:  # Ignorer les lignes vides
                    # Traduire la phrase en allemand
                    allemand_traduction = translate_text(line, target_language="de")
                    
                    # Écrire la phrase traduite dans le fichier de sortie
                    f_out.write(f"{allemand_traduction}\n")
                    print(f"Traduction : {line} -> {allemand_traduction}")
    
    print(f"Traduction terminée. Fichiers de sortie dans : {output_dir}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Traduire les phrases de fichiers texte en allemand.')
    parser.add_argument('--input_dir', type=str, 
                        default=DEFAULT_INPUT_DIR,
                        help=f'Répertoire contenant les fichiers texte d\'entrée. Par défaut : {DEFAULT_INPUT_DIR}')
    parser.add_argument('--output_dir', type=str, 
                        default=DEFAULT_OUTPUT_DIR,
                        help=f'Répertoire de sortie pour les fichiers texte traduits. Par défaut : {DEFAULT_OUTPUT_DIR}')
    
    args = parser.parse_args()
    translate_text_files(args.input_dir, args.output_dir)
