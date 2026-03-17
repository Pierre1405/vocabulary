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
    
    # Supprimer les tables si elles existent déjà
    cursor.execute("DROP TABLE IF EXISTS phrases")
    cursor.execute("DROP TABLE IF EXISTS category")
    cursor.execute("DROP TABLE IF EXISTS story")
    
    # Créer la table pour stocker les catégories
    cursor.execute("""
        CREATE TABLE category (
            id INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE
        )
    """)
    
    # Créer la table pour stocker les histoires
    cursor.execute("""
        CREATE TABLE story (
            id INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE
        )
    """)
    
    # Créer la table pour stocker les phrases
    cursor.execute("""
        CREATE TABLE phrases (
            id INTEGER  NOT NULL PRIMARY KEY,
            francais TEXT NOT NULL,
            allemand TEXT NOT NULL,
            category_id INTEGER NOT NULL,
            story_id INTEGER NOT NULL,
            apprise INTEGER NOT NULL,
            FOREIGN KEY (story_id) REFERENCES story(id) ON DELETE CASCADE,
            FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
        )
    """)
    
    # Parcourir les chunks et insérer les données
    chunk_files = [f for f in os.listdir(chunks_dir) if f.startswith("chunk_") and f.endswith(".tsv")]
    chunk_files.sort()  # Trier pour garantir l'ordre
    
    # Dictionnaire pour mapper les noms de catégories à leurs IDs
    category_ids = {}
    story_ids = {}

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
                    story = parts[4]

                    # Vérifier si la catégorie existe déjà dans le dictionnaire
                    if categorie not in category_ids:
                        # Insérer la catégorie dans la table category et récupérer son ID
                        cursor.execute("INSERT INTO category (name) VALUES (?)", (categorie,))
                        category_ids[categorie] = cursor.lastrowid
                        print(f"Catégorie ajoutée : {categorie} (ID: {category_ids[categorie]})")

                    # Vérifier si la catégorie existe déjà dans le dictionnaire
                    if story not in story_ids:
                        # Insérer la catégorie dans la table category et récupérer son ID
                        cursor.execute("INSERT INTO story (name) VALUES (?)", (story,))
                        story_ids[story] = cursor.lastrowid
                        print(f"Story ajoutée : {story} (ID: {story_ids[story]})")
                    
                    # Insérer la phrase avec l'ID de la catégorie
                    cursor.execute("INSERT INTO phrases (id, francais, allemand, category_id, story_id, apprise) VALUES (?, ?, ?, ?, ?, ?)", (id, francais, allemand, category_ids[categorie], story_ids[story], 0))
    
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
