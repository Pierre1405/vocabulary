#!/usr/bin/env python3
"""
Script pour générer une base de données SQLite à partir des chunks TSV.
"""

import os
import sys
import sqlite3
import argparse

sys.stdout.reconfigure(line_buffering=True)

# Chemins par défaut
DEFAULT_CHUNKS_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 3 chunk\\"
DEFAULT_OUTPUT_DB = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\app\\src\\main\\assets\\vocabulary.db"

def create_database(chunks_dir, output_db):
    os.makedirs(os.path.dirname(output_db), exist_ok=True)

    conn = sqlite3.connect(output_db)
    cursor = conn.cursor()

    cursor.execute("DROP TABLE IF EXISTS configuration")
    cursor.execute("DROP TABLE IF EXISTS learning")
    cursor.execute("DROP TABLE IF EXISTS translation")
    cursor.execute("DROP TABLE IF EXISTS sentence")
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
        CREATE TABLE sentence (
            id INTEGER NOT NULL PRIMARY KEY,
            category_id INTEGER NOT NULL,
            story_id INTEGER NOT NULL,
            FOREIGN KEY (story_id) REFERENCES story(id) ON DELETE CASCADE,
            FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
        )
    """)

    cursor.execute("""
        CREATE TABLE translation (
            sentence_id INTEGER NOT NULL,
            locale TEXT NOT NULL,
            translation TEXT NOT NULL,
            PRIMARY KEY (sentence_id, locale),
            FOREIGN KEY (sentence_id) REFERENCES sentence(id) ON DELETE CASCADE
        )
    """)

    cursor.execute("""
        CREATE TABLE configuration (
            key TEXT NOT NULL PRIMARY KEY,
            value TEXT NOT NULL
        )
    """)
    cursor.execute("INSERT INTO configuration (key, value) VALUES ('native_language', 'fr')")
    cursor.execute("INSERT INTO configuration (key, value) VALUES ('learned_language', 'de')")

    cursor.execute("""
        CREATE TABLE learning (
            sentence_id INTEGER NOT NULL,
            source_locale TEXT NOT NULL,
            target_locale TEXT NOT NULL,
            grade INTEGER NOT NULL,
            PRIMARY KEY (sentence_id, source_locale, target_locale),
            FOREIGN KEY (sentence_id) REFERENCES sentence(id) ON DELETE CASCADE
        )
    """)

    chunk_files = sorted([f for f in os.listdir(chunks_dir) if f.startswith("chunk_") and f.endswith(".tsv")])

    category_ids = {}
    story_ids = {}

    for chunk_file in chunk_files:
        chunk_path = os.path.join(chunks_dir, chunk_file)
        print(f"Traitement du chunk : {chunk_file}")

        with open(chunk_path, 'r', encoding='utf-8') as f:
            header = f.readline().strip().split('\t')
            # Header format: id, {locale1}, {locale2}, ..., categorie, story
            categorie_idx = header.index('categorie')
            story_idx = header.index('story')
            locale_columns = header[1:categorie_idx]
            print(f"Locales détectées : {locale_columns}")

            for line in f:
                parts = line.strip().split('\t')
                if len(parts) < len(header):
                    continue

                sentence_id = int(parts[0])
                categorie = parts[categorie_idx]
                story = parts[story_idx]

                if categorie not in category_ids:
                    cursor.execute("INSERT INTO category (name) VALUES (?)", (categorie,))
                    category_ids[categorie] = cursor.lastrowid
                    print(f"Catégorie ajoutée : {categorie} (ID: {category_ids[categorie]})")

                if story not in story_ids:
                    cursor.execute("INSERT INTO story (name) VALUES (?)", (story,))
                    story_ids[story] = cursor.lastrowid
                    print(f"Story ajoutée : {story} (ID: {story_ids[story]})")

                cursor.execute(
                    "INSERT INTO sentence (id, category_id, story_id) VALUES (?, ?, ?)",
                    (sentence_id, category_ids[categorie], story_ids[story])
                )

                for i, locale in enumerate(locale_columns):
                    translation = parts[1 + i]
                    cursor.execute(
                        "INSERT INTO translation (sentence_id, locale, translation) VALUES (?, ?, ?)",
                        (sentence_id, locale, translation)
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
