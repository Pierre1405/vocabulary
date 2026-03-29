#!/usr/bin/env python3
"""
Script pour générer un fichier TSV à partir de fichiers texte.
"""

import os
import sys
import argparse
import configparser

sys.stdout.reconfigure(line_buffering=True)

# Chemins par défaut
DEFAULT_SOURCE_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\content\\step 0 source\\"
DEFAULT_TRANSLATION_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\content\\step 1 translation\\"
DEFAULT_OUTPUT_FILE = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\content\\step 2 generate_tsv\\Sentence pairs with ID.tsv"

def load_config(source_dir):
    config = configparser.ConfigParser()
    config.read(os.path.join(source_dir, "config.properties"), encoding='utf-8')
    locales = [l.strip() for l in config.get('languages', 'locales').split(',')]
    return locales


def extract_locale(filename):
    """Extrait la locale depuis un nom de fichier de la forme 'nom_XX.txt'."""
    return filename[:-4].rsplit("_", 1)[-1]


def generate_tsv_from_text_files(source_dir, translation_dir, output_file):
    locales = load_config(source_dir)
    print(f"Locales : {locales}")

    os.makedirs(os.path.dirname(output_file), exist_ok=True)

    source_files = sorted([f for f in os.listdir(source_dir) if f.endswith(".txt")])

    with open(output_file, 'w', encoding='utf-8') as f_out:
        f_out.write("id\t" + "\t".join(locales) + "\tfile_name\n")

        id_counter = 1

        for source_file in source_files:
            source_locale = extract_locale(source_file)
            if source_locale not in locales:
                print(f"Locale inconnue pour {source_file}, ignoré.")
                continue

            base = source_file[: -(len(source_locale) + 5)]  # retire _XX.txt
            other_locales = [l for l in locales if l != source_locale]

            # Vérifier que les fichiers de traduction existent
            translation_paths = {}
            missing = False
            for locale in other_locales:
                path = os.path.join(translation_dir, f"{base}_{locale}.txt")
                if not os.path.exists(path):
                    print(f"Fichier manquant, ignoré : {path}")
                    missing = True
                    break
                translation_paths[locale] = path

            if missing:
                continue

            print(f"Traitement du fichier : {source_file} (source: {source_locale})")

            with open(os.path.join(source_dir, source_file), 'r', encoding='utf-8') as f:
                source_lines = [l.strip() for l in f.readlines() if l.strip()][2:]

            translation_lines = {}
            for locale, path in translation_paths.items():
                with open(path, 'r', encoding='utf-8') as f:
                    lines = [l.strip() for l in f.readlines() if l.strip()]
                    translation_lines[locale] = lines[2:]

            for i, source_text in enumerate(source_lines):
                row_data = {source_locale: source_text}
                valid = True
                for locale, lines in translation_lines.items():
                    if i < len(lines):
                        row_data[locale] = lines[i]
                    else:
                        valid = False
                        break

                if not valid:
                    continue

                row = [str(id_counter)] + [row_data[l] for l in locales] + [base]
                f_out.write("\t".join(row) + "\n")
                id_counter += 1
                print(f"Ligne ajoutée : {source_text}")

    print(f"Fichier TSV généré avec succès. Fichier de sortie : {output_file}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Générer un fichier TSV à partir de fichiers texte.')
    parser.add_argument('--source_dir', type=str, default=DEFAULT_SOURCE_DIR)
    parser.add_argument('--translation_dir', type=str, default=DEFAULT_TRANSLATION_DIR)
    parser.add_argument('--output_file', type=str, default=DEFAULT_OUTPUT_FILE)

    args = parser.parse_args()
    generate_tsv_from_text_files(args.source_dir, args.translation_dir, args.output_file)
