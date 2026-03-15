#!/usr/bin/env python3
"""
Script pour générer des fichiers audio à partir des chunks TSV.
Utilise l'API Google Cloud Text-to-Speech.
"""

import os
import argparse
from google.cloud import texttospeech

# Chemins par défaut
DEFAULT_CHUNKS_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\generated\\step 1 chunk\\"
DEFAULT_OUTPUT_DIR = "C:\\Users\\Pierre corbel\\Desktop\\code\\Android app\\vocabulary\\app\\src\\main\\res\\raw\\"

def generate_audio(chunks_dir, output_dir, language_code="de-DE", voice_name="de-DE-Wavenet-A"):
    """
    Génère des fichiers audio à partir des chunks TSV.
    
    Args:
        chunks_dir (str): Répertoire contenant les chunks TSV.
        output_dir (str): Répertoire de sortie pour les fichiers audio.
        language_code (str): Code de la langue (par défaut : "de-DE" pour l'allemand).
        voice_name (str): Nom de la voix à utiliser (par défaut : "de-DE-Wavenet-A").
    """
    # Créer le répertoire de sortie si nécessaire
    os.makedirs(output_dir, exist_ok=True)
    
    # Initialiser le client Google Cloud Text-to-Speech
    client = texttospeech.TextToSpeechClient()
    
    # Configurer la voix
    voice = texttospeech.VoiceSelectionParams(
        language_code=language_code,
        name=voice_name
    )
    
    audio_config = texttospeech.AudioConfig(
        audio_encoding=texttospeech.AudioEncoding.MP3
    )
    
    # Parcourir les chunks et générer les fichiers audio
    chunk_files = [f for f in os.listdir(chunks_dir) if f.startswith("chunk_") and f.endswith(".tsv")]
    chunk_files.sort()
    
    for chunk_file in chunk_files:
        chunk_path = os.path.join(chunks_dir, chunk_file)
        print(f"Traitement du chunk : {chunk_file}")
        
        with open(chunk_path, 'r', encoding='utf-8') as f:
            header = f.readline()  # Lire l'en-tête
            for line in f:
                parts = line.strip().split('\t')
                if len(parts) >= 3:
                    id = int(parts[0])
                    text = parts[1]  # Texte en allemand
                    
                    # Générer le nom du fichier audio
                    audio_file = os.path.join(output_dir, f"phrase_{id}_de.mp3")
                    
                    # Vérifier si le fichier existe déjà
                    if os.path.exists(audio_file):
                        print(f"Fichier audio déjà existant : {audio_file}")
                        continue
                    
                    # Synthétiser la voix
                    synthesis_input = texttospeech.SynthesisInput(text=text)
                    response = client.synthesize_speech(
                        input=synthesis_input,
                        voice=voice,
                        audio_config=audio_config
                    )
                    
                    # Sauvegarder le fichier audio
                    with open(audio_file, "wb") as out:
                        out.write(response.audio_content)
                    
                    print(f"Fichier audio généré : {audio_file}")
    
    print(f"Génération des fichiers audio terminée. Répertoire de sortie : {output_dir}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Générer des fichiers audio à partir des chunks TSV.')
    parser.add_argument('--chunks_dir', type=str, 
                        default=DEFAULT_CHUNKS_DIR,
                        help=f'Répertoire contenant les chunks TSV. Par défaut : {DEFAULT_CHUNKS_DIR}')
    parser.add_argument('--output_dir', type=str, 
                        default=DEFAULT_OUTPUT_DIR,
                        help=f'Répertoire de sortie pour les fichiers audio. Par défaut : {DEFAULT_OUTPUT_DIR}')
    parser.add_argument('--language_code', type=str, 
                        default="de-DE",
                        help='Code de la langue (par défaut : "de-DE" pour l\'allemand).')
    parser.add_argument('--voice_name', type=str, 
                        default="de-DE-Wavenet-A",
                        help='Nom de la voix à utiliser (par défaut : "de-DE-Wavenet-A").')
    
    args = parser.parse_args()
    generate_audio(args.chunks_dir, args.output_dir, args.language_code, args.voice_name)
