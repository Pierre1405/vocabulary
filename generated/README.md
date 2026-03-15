# Génération des ressources pour l'application

Ce répertoire contient les outils et scripts pour générer les ressources nécessaires à l'application d'apprentissage de phrases en allemand. Les ressources incluent des fichiers SQLite et des données pour l'application.

## Structure du répertoire

```
generated/
├── step 0 source/                  # Fichiers sources (TSV, etc.)
├── step 1 translation/            # Scripts pour traduire les phrases
├── step 2 add_id/                  # Scripts pour ajouter des IDs aux phrases
├── step 3 chunk/                  # Scripts pour diviser les fichiers en chunks
├── step 4 sqlite/                 # Scripts pour générer la base de données SQLite
├── step 5 audio/                  # Scripts pour générer les fichiers audio
└── README.md                      # Ce fichier
```

## Étapes de génération

### Étape 0 : Fichiers sources
- **Répertoire** : `step 0 source/`
- **Contenu** :
  - Fichiers TSV contenant les paires de phrases en allemand et français.
  - Exemple : `Sentence pairs in German-French - 2026-03-13.tsv`

### Étape 1 : Traduction des phrases
- **Répertoire** : `step 1 translation/`
- **Script** : `translate_text.py`
- **Description** :
  - Traduit les phrases de fichiers texte (format `category_nom_fr.txt`) en allemand en utilisant l'API Google Cloud Translation.
  - Génère des fichiers texte traduits (format : `category_nom_de.txt`).
- **Prérequis** :
  - Activer l'API Google Cloud Translation et configurer l'authentification.
  - Installer la bibliothèque cliente : `pip install google-cloud-translate`.
- **Utilisation** :
  ```bash
  python translate_text.py --input_dir "chemin/vers/dossier" --output_dir "chemin/vers/sortie"
  ```
  Ou simplement :
  ```bash
  python translate_text.py
  ```
  (utilise les valeurs par défaut)

### Étape 2 : Génération du fichier TSV
- **Répertoire** : `step 2 generate_tsv/`
- **Script** : `generate_tsv.py`
- **Description** :
  - Génère un fichier TSV à partir des fichiers texte en français et en allemand.
  - Le fichier TSV contient 5 colonnes : `id`, `francais`, `allemand`, `categorie`, et `nom`.
  - La catégorie et le nom sont extraits du nom du fichier (format : `category_nom_fr.txt`).
- **Utilisation** :
  ```bash
  python generate_tsv.py --fr_dir "chemin/vers/francais" --de_dir "chemin/vers/allemand" --output_file "chemin/vers/sortie.tsv"
  ```
  Ou simplement :
  ```bash
  python generate_tsv.py
  ```
  (utilise les valeurs par défaut)

### Étape 3 : Division en chunks
- **Répertoire** : `step 3 chunk/`
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

### Étape 4 : Génération de la base de données SQLite
- **Répertoire** : `step 4 sqlite/`
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

### Étape 5 : Génération des fichiers audio
- **Répertoire** : `step 5 audio/`
- **Script** : `generate_audio.py`
- **Description** :
  - Génère des fichiers audio MP3 pour chaque phrase en allemand et en français.
  - Utilise l'API Google Cloud Text-to-Speech pour synthétiser la voix.
  - Les fichiers audio sont nommés `phrase_{id}_de.mp3` (allemand) et `phrase_{id}_fr.mp3` (français), et placés dans `app/src/main/res/raw/`.
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
   - Placer les fichiers texte (format `category_nom_fr.txt`) dans `step 0 source/`.

2. **Traduire les phrases (si nécessaire)** :
   ```bash
   cd generated\step 1 translation
   python translate_text.py
   ```

3. **Générer le fichier TSV** :
   ```bash
   cd ..\step 2 generate_tsv
   python generate_tsv.py
   ```

4. **Diviser en chunks** :
   ```bash
   cd ..\step 3 chunk
   python split_tsv.py
   ```

5. **Générer la base de données SQLite** :
   ```bash
   cd ..\step 4 sqlite
   python generate_sqlite.py
   ```

6. **Générer les fichiers audio** :
   ```bash
   cd ..\step 5 audio
   python generate_audio.py
   ```

7. **Intégrer les ressources dans l'application** :
   - Le fichier `vocabulary.db` est généré dans `app/src/main/assets/`.
   - Les fichiers audio sont générés dans `app/src/main/res/raw/`.
   - Utiliser Room pour accéder à la base de données et `MediaPlayer` pour lire les fichiers audio dans votre application Android.

## Notes
- Assurez-vous que les chemins dans les scripts sont corrects pour votre environnement.
- Les valeurs par défaut dans les scripts peuvent être modifiées selon vos besoins.
- Vérifiez les données à chaque étape pour garantir leur intégrité.
