#!/usr/bin/env python3
"""
Script pour générer une base de données SQLite à partir des chunks TSV.
"""

import os
import sys
import re
import sqlite3
import argparse

sys.stdout.reconfigure(line_buffering=True)

# Chemins par défaut
DEFAULT_CHUNKS_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 3 chunk\\"
DEFAULT_OUTPUT_DB = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\app\\src\\main\\assets\\vocabulary.db"
DEFAULT_VERSION_FILE = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\shared\\src\\commonMain\\kotlin\\com\\example\\myapplication\\data\\DatabaseVersion.kt"
DEFAULT_SOURCE_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 0 source\\"
DEFAULT_TRANSLATION_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 1 translation\\"

def increment_db_version(version_file):
    with open(version_file, 'r', encoding='utf-8') as f:
        content = f.read()
    match = re.search(r'DB_VERSION\s*=\s*(\d+)', content)
    if not match:
        print("Impossible de trouver DB_VERSION dans le fichier.")
        return
    current = int(match.group(1))
    new_version = current + 1
    content = re.sub(r'(DB_VERSION\s*=\s*)\d+', f'\\g<1>{new_version}', content)
    with open(version_file, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"DB_VERSION incrementee : {current} -> {new_version}")

def read_first_two_lines(file_path):
    """Lit les 2 premières lignes non-vides d'un fichier."""
    lines = []
    with open(file_path, 'r', encoding='utf-8') as f:
        for line in f:
            stripped = line.strip()
            if stripped:
                lines.append(stripped)
            if len(lines) == 2:
                break
    return lines[0] if len(lines) > 0 else "", lines[1] if len(lines) > 1 else ""

def load_file_metadata(file_name, locale_columns, source_dir, translation_dir):
    """Retourne {locale: (category_name, story_name)} pour un file_name donné."""
    source_locale = locale_columns[0]
    target_locales = locale_columns[1:]
    metadata = {}

    source_path = os.path.join(source_dir, f"{file_name}_{source_locale}.txt")
    if os.path.exists(source_path):
        metadata[source_locale] = read_first_two_lines(source_path)

    for locale in target_locales:
        target_path = os.path.join(translation_dir, f"{file_name}_{locale}.txt")
        if os.path.exists(target_path):
            metadata[locale] = read_first_two_lines(target_path)

    return metadata

def create_database(chunks_dir, output_db, source_dir, translation_dir):
    os.makedirs(os.path.dirname(output_db), exist_ok=True)

    conn = sqlite3.connect(output_db)
    cursor = conn.cursor()

    cursor.execute("DROP TABLE IF EXISTS configuration")
    cursor.execute("DROP TABLE IF EXISTS learning")
    cursor.execute("DROP TABLE IF EXISTS translation")
    cursor.execute("DROP TABLE IF EXISTS sentence")
    cursor.execute("DROP TABLE IF EXISTS story_category")
    cursor.execute("DROP TABLE IF EXISTS category_translation")
    cursor.execute("DROP TABLE IF EXISTS story_translation")
    cursor.execute("DROP TABLE IF EXISTS category")
    cursor.execute("DROP TABLE IF EXISTS story")

    cursor.execute("""
        CREATE TABLE category (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT
        )
    """)

    cursor.execute("""
        CREATE TABLE story (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT
        )
    """)

    cursor.execute("""
        CREATE TABLE category_translation (
            category_id INTEGER NOT NULL,
            locale TEXT NOT NULL,
            translation TEXT NOT NULL,
            PRIMARY KEY (category_id, locale),
            FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
        )
    """)

    cursor.execute("""
        CREATE TABLE story_translation (
            story_id INTEGER NOT NULL,
            locale TEXT NOT NULL,
            translation TEXT NOT NULL,
            PRIMARY KEY (story_id, locale),
            FOREIGN KEY (story_id) REFERENCES story(id) ON DELETE CASCADE
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
            # Header format: id, {locale1}, {locale2}, ..., file_name
            file_name_idx = header.index('file_name')
            locale_columns = header[1:file_name_idx]
            print(f"Locales détectées : {locale_columns}")

            for line in f:
                parts = line.strip().split('\t')
                if len(parts) < len(header):
                    continue

                sentence_id = int(parts[0])
                file_name = parts[file_name_idx]
                # category key = première partie du file_name (avant le premier _)
                category_key = file_name.split('_')[0]

                if file_name not in story_ids:
                    metadata = load_file_metadata(file_name, locale_columns, source_dir, translation_dir)

                    # Insertion catégorie
                    if category_key not in category_ids:
                        cursor.execute("INSERT INTO category DEFAULT VALUES")
                        category_ids[category_key] = cursor.lastrowid
                        for locale, (cat_name, _) in metadata.items():
                            if cat_name:
                                cursor.execute(
                                    "INSERT OR IGNORE INTO category_translation (category_id, locale, translation) VALUES (?, ?, ?)",
                                    (category_ids[category_key], locale, cat_name)
                                )
                        print(f"Catégorie ajoutée : {category_key} (ID: {category_ids[category_key]})")

                    # Insertion story
                    cursor.execute("INSERT INTO story DEFAULT VALUES")
                    story_ids[file_name] = cursor.lastrowid
                    for locale, (_, story_name) in metadata.items():
                        if story_name:
                            cursor.execute(
                                "INSERT OR IGNORE INTO story_translation (story_id, locale, translation) VALUES (?, ?, ?)",
                                (story_ids[file_name], locale, story_name)
                            )
                    print(f"Story ajoutée : {file_name} (ID: {story_ids[file_name]})")

                cursor.execute(
                    "INSERT INTO sentence (id, category_id, story_id) VALUES (?, ?, ?)",
                    (sentence_id, category_ids[category_key], story_ids[file_name])
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
    increment_db_version(DEFAULT_VERSION_FILE)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Générer une base de données SQLite à partir des chunks TSV.')
    parser.add_argument('--chunks_dir', type=str, default=DEFAULT_CHUNKS_DIR)
    parser.add_argument('--output_db', type=str, default=DEFAULT_OUTPUT_DB)
    parser.add_argument('--source_dir', type=str, default=DEFAULT_SOURCE_DIR)
    parser.add_argument('--translation_dir', type=str, default=DEFAULT_TRANSLATION_DIR)
    args = parser.parse_args()
    create_database(args.chunks_dir, args.output_db, args.source_dir, args.translation_dir)
