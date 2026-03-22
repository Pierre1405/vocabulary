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
DEFAULT_SOURCE_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 0 source\\"
DEFAULT_TRANSLATION_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 1 translation\\"
DEFAULT_OUTPUT_FILE = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 2 generate_tsv\\Sentence pairs with ID.tsv"

def load_config(source_dir):
    config = configparser.ConfigParser()
    config.read(os.path.join(source_dir, "config.properties"), encoding='utf-8')
    source_locale = config.get('languages', 'source_locale').strip()
    target_locales = [l.strip() for l in config.get('languages', 'target_locales').split(',')]
    return source_locale, target_locales

def generate_tsv_from_text_files(source_dir, translation_dir, output_file):
    source_locale, target_locales = load_config(source_dir)
    all_locales = [source_locale] + target_locales
    print(f"Locales : source={source_locale}, cibles={target_locales}")

    os.makedirs(os.path.dirname(output_file), exist_ok=True)

    source_files = sorted([f for f in os.listdir(source_dir) if f.endswith(f'_{source_locale}.txt')])

    with open(output_file, 'w', encoding='utf-8') as f_out:
        f_out.write("id\t" + "\t".join(all_locales) + "\tfile_name\n")

        id_counter = 1

        for source_file in source_files:
            base = source_file.replace(f'_{source_locale}.txt', '')
            source_path = os.path.join(source_dir, source_file)

            # Trouver les fichiers pour chaque locale cible
            target_paths = []
            missing = False
            for locale in target_locales:
                target_file = base + f'_{locale}.txt'
                target_path = os.path.join(translation_dir, target_file)
                if not os.path.exists(target_path):
                    print(f"Fichier manquant, ignoré : {target_path}")
                    missing = True
                    break
                target_paths.append(target_path)

            if missing:
                continue

            print(f"Traitement du fichier : {source_file}")

            with open(source_path, 'r', encoding='utf-8') as f:
                source_lines = [l.strip() for l in f.readlines() if l.strip()][2:]

            target_lines_list = []
            for target_path in target_paths:
                with open(target_path, 'r', encoding='utf-8') as f:
                    lines = [l.strip() for l in f.readlines() if l.strip()]
                    target_lines_list.append(lines[2:])

            for i, source_text in enumerate(source_lines):
                target_texts = []
                valid = True
                for target_lines in target_lines_list:
                    if i < len(target_lines):
                        target_texts.append(target_lines[i])
                    else:
                        valid = False
                        break

                if not valid:
                    continue

                row = [str(id_counter), source_text] + target_texts + [base]
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
