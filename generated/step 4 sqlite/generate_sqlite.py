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
    os.makedirs(os.path.dirname(output_db), exist_ok=True)

    conn = sqlite3.connect(output_db)
    cursor = conn.cursor()

    cursor.execute("DROP TABLE IF EXISTS translation")
    cursor.execute("DROP TABLE IF EXISTS phrases")
    cursor.execute("DROP TABLE IF EXISTS story_category")
    cursor.execute("DROP TABLE IF EXISTS category")
    cursor.execute("DROP TABLE IF EXISTS story")

    cursor.execute("""
        CREATE TABLE category (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE
        )
    """)

    cursor.execute("""
        CREATE TABLE story (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE
        )
    """)

    cursor.execute("""
        CREATE TABLE phrases (
            id INTEGER NOT NULL PRIMARY KEY,
            category_id INTEGER NOT NULL,
            story_id INTEGER NOT NULL,
            learned INTEGER NOT NULL,
            FOREIGN KEY (story_id) REFERENCES story(id) ON DELETE CASCADE,
            FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
        )
    """)

    cursor.execute("""
        CREATE TABLE translation (
            phrase_id INTEGER NOT NULL,
            locale TEXT NOT NULL,
            translation TEXT NOT NULL,
            PRIMARY KEY (phrase_id, locale),
            FOREIGN KEY (phrase_id) REFERENCES phrases(id) ON DELETE CASCADE
        )
    """)

    chunk_files = [f for f in os.listdir(chunks_dir) if f.startswith("chunk_") and f.endswith(".tsv")]
    chunk_files.sort()

    category_ids = {}
    story_ids = {}

    for chunk_file in chunk_files:
        chunk_path = os.path.join(chunks_dir, chunk_file)
        print(f"Traitement du chunk : {chunk_file}")

        with open(chunk_path, 'r', encoding='utf-8') as f:
            f.readline()  # skip header
            for line in f:
                parts = line.strip().split('\t')
                if len(parts) >= 5:
                    phrase_id = int(parts[0])
                    french    = parts[1]
                    german    = parts[2]
                    categorie = parts[3]
                    story     = parts[4]

                    if categorie not in category_ids:
                        cursor.execute("INSERT INTO category (name) VALUES (?)", (categorie,))
                        category_ids[categorie] = cursor.lastrowid
                        print(f"Catégorie ajoutée : {categorie} (ID: {category_ids[categorie]})")

                    if story not in story_ids:
                        cursor.execute("INSERT INTO story (name) VALUES (?)", (story,))
                        story_ids[story] = cursor.lastrowid
                        print(f"Story ajoutée : {story} (ID: {story_ids[story]})")

                    cursor.execute(
                        "INSERT INTO phrases (id, category_id, story_id, learned) VALUES (?, ?, ?, ?)",
                        (phrase_id, category_ids[categorie], story_ids[story], 0)
                    )
                    cursor.execute(
                        "INSERT INTO translation (phrase_id, locale, translation) VALUES (?, ?, ?)",
                        (phrase_id, "fr", french)
                    )
                    cursor.execute(
                        "INSERT INTO translation (phrase_id, locale, translation) VALUES (?, ?, ?)",
                        (phrase_id, "de", german)
                    )

    conn.commit()
    conn.close()
    print(f"Base de données générée avec succès : {output_db}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Générer une base de données SQLite à partir des chunks TSV.')
    parser.add_argument('--chunks_dir', type=str, default=DEFAULT_CHUNKS_DIR)
    parser.add_argument('--output_db', type=str, default=DEFAULT_OUTPUT_DB)
    args = parser.parse_args()
    create_database(args.chunks_dir, args.output_db)
