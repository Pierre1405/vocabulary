#!/usr/bin/env python3
"""
Script pour traduire les phrases de fichiers texte vers les langues définies dans config.properties.
Utilise l'API Google Cloud Translation.
"""

import html
import os
import sys
import configparser
import argparse
from google.cloud import translate_v2 as translate

sys.stdout.reconfigure(line_buffering=True)

# Chemins par défaut
DEFAULT_INPUT_DIR  = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\content\\step 0 source\\"
DEFAULT_OUTPUT_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\content\\step 1 translation\\"
DEFAULT_CONFIG     = os.path.join(DEFAULT_INPUT_DIR, "config.properties")


def load_config(config_path):
    config = configparser.ConfigParser()
    config.read(config_path, encoding="utf-8")
    locales = [l.strip() for l in config["languages"]["locales"].split(",")]
    return locales


def extract_locale(filename):
    """Extrait la locale depuis un nom de fichier de la forme 'nom_XX.txt'."""
    return filename[:-4].rsplit("_", 1)[-1]


def translate_line(text, target_locale):
    client = translate.Client()
    result = client.translate(text, target_language=target_locale)
    return html.unescape(result["translatedText"])


def translate_files(input_dir, output_dir, config_path):
    locales = load_config(config_path)
    print(f"Locales : {', '.join(locales)}")

    os.makedirs(output_dir, exist_ok=True)

    source_files = [f for f in os.listdir(input_dir) if f.endswith(".txt")]

    for source_file in source_files:
        source_locale = extract_locale(source_file)
        if source_locale not in locales:
            print(f"Locale inconnue pour {source_file}, ignoré.")
            continue

        target_locales = [l for l in locales if l != source_locale]
        input_path = os.path.join(input_dir, source_file)

        print(f"\nStart translate {input_path} (source: {source_locale})")

        for target_locale in target_locales:
            output_file = source_file.replace(f"_{source_locale}.txt", f"_{target_locale}.txt")
            output_path = os.path.join(output_dir, output_file)

            print(f"\n[{target_locale}] {source_file} -> {output_file}")

            with open(input_path, "r", encoding="utf-8") as f_in, \
                 open(output_path, "w", encoding="utf-8") as f_out:
                for line in f_in:
                    line = line.strip()
                    if line:
                        translation = translate_line(line, target_locale)
                        f_out.write(f"{translation}\n")
                        print(f"  {line} -> {translation}")

    print("\nTraduction terminée.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Traduire les fichiers source vers les langues définies dans config.properties.")
    parser.add_argument("--input_dir",  type=str, default=DEFAULT_INPUT_DIR)
    parser.add_argument("--output_dir", type=str, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--config",     type=str, default=DEFAULT_CONFIG)
    args = parser.parse_args()
    translate_files(args.input_dir, args.output_dir, args.config)
