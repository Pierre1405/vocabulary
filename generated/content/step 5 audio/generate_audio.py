#!/usr/bin/env python3
"""
Script pour générer des fichiers audio à partir des chunks TSV.
Utilise l'API Google Cloud Text-to-Speech.
"""

import os
import sys
import argparse
import configparser
from google.cloud import texttospeech

sys.stdout.reconfigure(line_buffering=True)

# Chemins par défaut
DEFAULT_CHUNKS_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\content\\step 3 chunk\\"
DEFAULT_OUTPUT_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\app\\src\\main\\res\\raw\\"
DEFAULT_CONFIG_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\content\\step 0 source\\"

def load_voices(config_dir):
    config = configparser.ConfigParser()
    config.read(os.path.join(config_dir, "config.properties"), encoding='utf-8')
    locales = [l.strip() for l in config.get('languages', 'locales').split(',')]

    voices = {}
    for locale in locales:
        voice_str = config.get('voices', locale).strip()
        language_code, voice_name = voice_str.split(':')
        voices[locale] = (language_code, voice_name)

    return voices

def generate_audio(chunks_dir, output_dir, config_dir):
    voices = load_voices(config_dir)
    print(f"Voix configurées : {voices}")

    os.makedirs(output_dir, exist_ok=True)

    client = texttospeech.TextToSpeechClient()

    audio_config = texttospeech.AudioConfig(
        audio_encoding=texttospeech.AudioEncoding.MP3
    )

    chunk_files = sorted([f for f in os.listdir(chunks_dir) if f.startswith("chunk_") and f.endswith(".tsv")])

    sentence_index_by_file = {}

    for chunk_file in chunk_files:
        chunk_path = os.path.join(chunks_dir, chunk_file)
        print(f"Traitement du chunk : {chunk_file}")

        with open(chunk_path, 'r', encoding='utf-8') as f:
            header = f.readline().strip().split('\t')
            file_name_idx = header.index('file_name')
            locale_columns = header[1:file_name_idx]

            for line in f:
                parts = line.strip().split('\t')
                if len(parts) < len(header):
                    continue

                file_name = parts[file_name_idx]
                sentence_index_by_file[file_name] = sentence_index_by_file.get(file_name, -1) + 1
                sentence_key = f"{file_name}_{sentence_index_by_file[file_name]}"

                for i, locale in enumerate(locale_columns):
                    text = parts[1 + i]
                    audio_file = os.path.join(output_dir, f"sentence_{sentence_key}_{locale}.mp3")

                    if os.path.exists(audio_file):
                        print(f"Fichier audio déjà existant : {audio_file}")
                        continue

                    if locale not in voices:
                        print(f"Aucune voix configurée pour la locale : {locale}, ignoré")
                        continue

                    language_code, voice_name = voices[locale]
                    voice_params = texttospeech.VoiceSelectionParams(
                        language_code=language_code,
                        name=voice_name
                    )
                    synthesis_input = texttospeech.SynthesisInput(text=text)
                    response = client.synthesize_speech(
                        input=synthesis_input,
                        voice=voice_params,
                        audio_config=audio_config
                    )
                    with open(audio_file, "wb") as out:
                        out.write(response.audio_content)
                    print(f"Fichier audio généré : {audio_file}")

    print(f"Génération des fichiers audio terminée. Répertoire de sortie : {output_dir}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Générer des fichiers audio à partir des chunks TSV.')
    parser.add_argument('--chunks_dir', type=str, default=DEFAULT_CHUNKS_DIR)
    parser.add_argument('--output_dir', type=str, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument('--config_dir', type=str, default=DEFAULT_CONFIG_DIR)

    args = parser.parse_args()
    generate_audio(args.chunks_dir, args.output_dir, args.config_dir)
