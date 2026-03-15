#!/usr/bin/env python3
"""
Script pour diviser un fichier TSV en chunks de N lignes.
"""

import os
import argparse

# Valeurs par défaut
DEFAULT_INPUT_FILE = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 0.5 add_id\\Sentence pairs with ID - 2026-03-13.tsv"
DEFAULT_OUTPUT_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 1 chunk\\"
DEFAULT_CHUNK_SIZE = 1000

def split_tsv(input_file, output_dir, chunk_size):
    """
    Divise un fichier TSV en chunks de taille spécifiée.
    
    Args:
        input_file (str): Chemin vers le fichier TSV d'entrée.
        output_dir (str): Répertoire de sortie pour les chunks.
        chunk_size (int): Nombre de lignes par chunk.
    """
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    
    with open(input_file, 'r', encoding='utf-8') as f:
        header = f.readline()  # Lire l'en-tête
        chunk_count = 0
        lines = []
        
        for line in f:
            lines.append(line)
            if len(lines) == chunk_size:
                chunk_count += 1
                output_file = os.path.join(output_dir, f"chunk_{chunk_count}.tsv")
                with open(output_file, 'w', encoding='utf-8') as chunk:
                    chunk.write(header)
                    chunk.writelines(lines)
                lines = []
        
        # Écrire le dernier chunk s'il reste des lignes
        if lines:
            chunk_count += 1
            output_file = os.path.join(output_dir, f"chunk_{chunk_count}.tsv")
            with open(output_file, 'w', encoding='utf-8') as chunk:
                chunk.write(header)
                chunk.writelines(lines)
    
    print(f"Fichier divisé en {chunk_count} chunks dans {output_dir}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Diviser un fichier TSV en chunks.')
    parser.add_argument('--input_file', type=str, 
                        default=DEFAULT_INPUT_FILE,
                        help=f'Chemin vers le fichier TSV d\'entrée. Par défaut : {DEFAULT_INPUT_FILE}')
    parser.add_argument('--output_dir', type=str, 
                        default=DEFAULT_OUTPUT_DIR,
                        help=f'Répertoire de sortie pour les chunks. Par défaut : {DEFAULT_OUTPUT_DIR}')
    parser.add_argument('--chunk_size', type=int, 
                        default=DEFAULT_CHUNK_SIZE,
                        help=f'Nombre de lignes par chunk. Par défaut : {DEFAULT_CHUNK_SIZE}')
    
    args = parser.parse_args()
    split_tsv(args.input_file, args.output_dir, args.chunk_size)
