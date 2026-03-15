# Génération des ressources pour l'application

Ce répertoire contient les outils et scripts pour générer les ressources nécessaires à l'application d'apprentissage de phrases en allemand. Les ressources incluent des fichiers SQLite et des données pour l'application.

## Structure du répertoire

```
generated/
├── step 0 source/                  # Fichiers sources (TSV, etc.)
├── step 1 chunk/                  # Scripts pour diviser les fichiers en chunks
├── step 2 process/                # Scripts pour traiter les chunks
├── step 3 generate_sqlite/        # Scripts pour générer la base de données SQLite
└── README.md                      # Ce fichier
```

## Étapes de génération

### Étape 0 : Fichiers sources
- **Répertoire** : `step 0 source/`
- **Contenu** :
  - Fichiers TSV contenant les paires de phrases en allemand et français.
  - Exemple : `Sentence pairs in German-French - 2026-03-13.tsv`

### Étape 0.5 : Ajout des IDs
- **Répertoire** : `step 0.5 add_id/`
- **Script** : `add_id_column.py`
- **Description** :
  - Ajoute une colonne d'ID auto-incrémenté au fichier TSV source.
  - Cela permet d'avoir un identifiant unique pour chaque phrase.
- **Utilisation** :
  ```bash
  python add_id_column.py --input_file "chemin/vers/fichier.tsv" --output_file "chemin/vers/sortie.tsv"
  ```
  Ou simplement :
  ```bash
  python add_id_column.py
  ```
  (utilise les valeurs par défaut)

### Étape 1 : Division en chunks
- **Répertoire** : `step 1 chunk/`
- **Script** : `split_tsv.py`
- **Description** :
  - Divise le fichier TSV (avec IDs) en plusieurs chunks pour faciliter le traitement.
  - Utilise des valeurs par défaut pour les chemins et la taille des chunks.
- **Utilisation** :
  ```bash
  python split_tsv.py --input_file "chemin/vers/fichier.tsv" --output_dir "chemin/vers/sortie" --chunk_size 1000
  ```
  Ou simplement :
  ```bash
  python split_tsv.py
  ```
  (utilise les valeurs par défaut)

### Étape 2 : Génération de la base de données SQLite
- **Répertoire** : `step 2 sqlite/`
- **Script** : `generate_sqlite.py`
- **Description** :
  - Génère une base de données SQLite à partir des chunks TSV.
  - Crée une table `phrases` avec les colonnes : `id`, `allemand`, `francais`, et `apprise`.
  - Utilise les IDs présents dans les fichiers TSV pour garantir l'unicité.
- **Utilisation** :
  ```bash
  python generate_sqlite.py --chunks_dir "chemin/vers/chunks" --output_db "chemin/vers/base.db"
  ```
  Ou simplement :
  ```bash
  python generate_sqlite.py
  ```
  (utilise les valeurs par défaut)

### Étape 3 : Génération des fichiers audio
- **Répertoire** : `step 3 audio/`
- **Script** : `generate_audio.py`
- **Description** :
  - Génère des fichiers audio MP3 pour chaque phrase en allemand.
  - Utilise l'API Google Cloud Text-to-Speech pour synthétiser la voix.
  - Les fichiers audio sont nommés `phrase_{id}_de.mp3` et placés dans `app/src/main/res/raw/`.
- **Prérequis** :
  - Activer l'API Google Cloud Text-to-Speech et configurer l'authentification.
  - Installer la bibliothèque cliente : `pip install google-cloud-texttospeech`.
- **Utilisation** :
  ```bash
  python generate_audio.py --chunks_dir "chemin/vers/chunks" --output_dir "chemin/vers/sortie" --language_code "de-DE" --voice_name "de-DE-Wavenet-A"
  ```
  Ou simplement :
  ```bash
  python generate_audio.py
  ```
  (utilise les valeurs par défaut)

## Exemple de workflow complet

1. **Préparer les fichiers sources** :
   - Placer les fichiers TSV dans `step 0 source/`.

2. **Ajouter des IDs aux phrases** :
   ```bash
   cd generated\step 0.5 add_id
   python add_id_column.py
   ```

3. **Diviser en chunks** :
   ```bash
   cd ..\step 1 chunk
   python split_tsv.py
   ```

4. **Générer la base de données SQLite** :
   ```bash
   cd ..\step 2 sqlite
   python generate_sqlite.py
   ```

5. **Générer les fichiers audio** :
   ```bash
   cd ..\step 3 audio
   python generate_audio.py
   ```

6. **Intégrer les ressources dans l'application** :
   - Le fichier `vocabulary.db` est généré dans `app/src/main/assets/`.
   - Les fichiers audio sont générés dans `app/src/main/res/raw/`.
   - Utiliser Room pour accéder à la base de données et `MediaPlayer` pour lire les fichiers audio dans votre application Android.

## Notes
- Assurez-vous que les chemins dans les scripts sont corrects pour votre environnement.
- Les valeurs par défaut dans les scripts peuvent être modifiées selon vos besoins.
- Vérifiez les données à chaque étape pour garantir leur intégrité.
