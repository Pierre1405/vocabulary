#!/usr/bin/env python3
"""
Script pour générer une base de données SQLite à partir des chunks TSV.
"""

import os
import sqlite3
import argparse

# Chemins par défaut
DEFAULT_CHUNKS_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 3 chunk\\"
DEFAULT_OUTPUT_DB = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\app\\src\\main\\assets\\vocabulary.db"

def create_database(chunks_dir, output_db):
    """
    Crée une base de données SQLite à partir des chunks TSV.
    
    Args:
        chunks_dir (str): Répertoire contenant les chunks TSV.
        output_db (str): Chemin vers le fichier SQLite de sortie.
    """
    # Créer le répertoire de sortie si nécessaire
    os.makedirs(os.path.dirname(output_db), exist_ok=True)
    
    # Connexion à la base de données
    conn = sqlite3.connect(output_db)
    cursor = conn.cursor()
    
    # Supprimer la table si elle existe déjà
    cursor.execute("DROP TABLE IF EXISTS phrases")
    
    # Créer la table pour stocker les phrases
    cursor.execute("""
        CREATE TABLE phrases (
            id INTEGER PRIMARY KEY,
            francais TEXT NOT NULL,
            allemand TEXT NOT NULL,
            categorie TEXT NOT NULL,
            nom TEXT NOT NULL,
            apprise INTEGER DEFAULT 0
        )
    """)
    
    # Parcourir les chunks et insérer les données
    chunk_files = [f for f in os.listdir(chunks_dir) if f.startswith("chunk_") and f.endswith(".tsv")]
    chunk_files.sort()  # Trier pour garantir l'ordre
    
    for chunk_file in chunk_files:
        chunk_path = os.path.join(chunks_dir, chunk_file)
        print(f"Traitement du chunk : {chunk_file}")
        
        with open(chunk_path, 'r', encoding='utf-8') as f:
            header = f.readline()  # Lire l'en-tête
            for line in f:
                parts = line.strip().split('\t')
                if len(parts) >= 5:
                    id = int(parts[0])
                    francais = parts[1]
                    allemand = parts[2]
                    categorie = parts[3]
                    nom = parts[4]
                    cursor.execute("INSERT INTO phrases (id, francais, allemand, categorie, nom) VALUES (?, ?, ?, ?, ?)", (id, francais, allemand, categorie, nom))
    
    # Valider les changements et fermer la connexion
    conn.commit()
    conn.close()
    
    print(f"Base de données générée avec succès : {output_db}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Générer une base de données SQLite à partir des chunks TSV.')
    parser.add_argument('--chunks_dir', type=str, 
                        default=DEFAULT_CHUNKS_DIR,
                        help=f'Répertoire contenant les chunks TSV. Par défaut : {DEFAULT_CHUNKS_DIR}')
    parser.add_argument('--output_db', type=str, 
                        default=DEFAULT_OUTPUT_DB,
                        help=f'Chemin vers le fichier SQLite de sortie. Par défaut : {DEFAULT_OUTPUT_DB}')
    
    args = parser.parse_args()
    create_database(args.chunks_dir, args.output_db)
