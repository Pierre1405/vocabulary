# Génération des ressources pour l'application

Ce répertoire contient les outils et scripts pour générer les ressources nécessaires à l'application d'apprentissage de phrases en allemand.

## Structure du répertoire

```
generated/
├── step 0 source/          # Fichiers texte sources (français)
├── step 1 translation/     # Scripts de traduction (Google Cloud Translation)
├── step 2 generate_tsv/    # Génération du fichier TSV
├── step 3 chunk/           # Division en chunks
├── step 4 sqlite/          # Génération de la base de données SQLite
├── step 5 audio/           # Génération des fichiers audio
└── README.md               # Ce fichier
```

## Étapes de génération

### Étape 0 : Fichiers sources
- **Répertoire** : `step 0 source/`
- Fichiers texte contenant des phrases en français
- Format : `category_nom_fr.txt`

### Étape 1 : Traduction des phrases
- **Répertoire** : `step 1 translation/`
- **Script** : `translate_text.py`
- Traduit les phrases du français vers l'allemand via l'API Google Cloud Translation
- **Prérequis** : `pip install google-cloud-translate`
- **Utilisation** :
  ```bash
  cd "step 1 translation"
  python translate_text.py
  ```

### Étape 2 : Génération du fichier TSV
- **Répertoire** : `step 2 generate_tsv/`
- **Script** : `generate_tsv.py`
- Génère un TSV avec les colonnes : `id`, `francais`, `allemand`, `categorie`, `nom`
- **Utilisation** :
  ```bash
  cd "step 2 generate_tsv"
  python generate_tsv.py
  ```

### Étape 3 : Division en chunks
- **Répertoire** : `step 3 chunk/`
- **Script** : `split_tsv.py`
- **Utilisation** :
  ```bash
  cd "step 3 chunk"
  python split_tsv.py
  ```

### Étape 4 : Génération de la base de données SQLite
- **Répertoire** : `step 4 sqlite/`
- **Script** : `generate_sqlite.py`
- Génère `vocabulary.db` avec les tables : `category`, `story`, `phrases`, `story_category`
- **Destination** : `app/src/main/assets/vocabulary.db`
- **Utilisation** :
  ```bash
  cd "step 4 sqlite"
  python generate_sqlite.py
  ```

### Étape 5 : Génération des fichiers audio
- **Répertoire** : `step 5 audio/`
- **Script** : `generate_audio.py`
- Génère des fichiers MP3 via Google Cloud Text-to-Speech
- Nommage : `phrase_{id}_de.mp3` (allemand) et `phrase_{id}_fr.mp3` (français)
- **Destination** : `app/src/main/res/raw/`
- **Prérequis** : `pip install google-cloud-texttospeech`
- **Utilisation** :
  ```bash
  cd "step 5 audio"
  python generate_audio.py
  ```

## Workflow complet

```bash
cd "step 1 translation" && python translate_text.py
cd "../step 2 generate_tsv" && python generate_tsv.py
cd "../step 3 chunk" && python split_tsv.py
cd "../step 4 sqlite" && python generate_sqlite.py
cd "../step 5 audio" && python generate_audio.py
```

## Intégration dans l'application

| Ressource | Destination | Accès dans l'app |
|-----------|-------------|-----------------|
| `vocabulary.db` | `app/src/main/assets/` | SQLDelight via `DatabaseDriverFactory` |
| `phrase_{id}_*.mp3` | `app/src/main/res/raw/` | `MediaPlayer` (Android) / `AVAudioPlayer` (iOS) |

## Notes

- Le schéma SQLite doit correspondre exactement aux fichiers `.sq` dans `shared/commonMain/sqldelight/`
- Tables attendues : `category`, `story`, `phrases`, `story_category`
- Sur iOS, `vocabulary.db` doit également être ajouté au bundle Xcode
