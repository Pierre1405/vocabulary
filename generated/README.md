# Génération des ressources pour l'application

Ce répertoire contient les scripts pour générer les ressources nécessaires à l'application. Tout est piloté par `step 0 source/config.properties`.

## Configuration (`step 0 source/config.properties`)

```properties
[languages]
source_locale = fr
target_locales = de,en

[voices]
fr = fr-FR:fr-FR-Wavenet-A
de = de-DE:de-DE-Wavenet-A
en = en-US:en-US-Wavenet-A
```

## Format des fichiers sources

Chaque fichier source suit le format `{categorie}_{nom}_{source_locale}.txt` :
- **Ligne 1** : nom de la catégorie (dans la langue source)
- **Ligne 2** : titre de l'histoire (dans la langue source)
- **Lignes suivantes** : une phrase par ligne

Les fichiers traduits (step 1) suivent le même format avec le suffixe `_{locale}.txt`.

## Structure du répertoire

```
generated/
├── step 0 source/          # Fichiers texte sources + config.properties
├── step 1 translation/     # Fichiers traduits (Google Cloud Translation)
├── step 2 generate_tsv/    # Fichier TSV généré
├── step 3 chunk/           # TSV découpé en chunks
├── step 4 sqlite/          # Base de données SQLite générée
├── step 5 audio/           # Fichiers audio MP3 générés
└── README.md               # Ce fichier
```

## Étapes de génération

### Étape 0 : Fichiers sources
- **Répertoire** : `step 0 source/`
- Fichiers texte contenant les phrases dans la langue source (`source_locale`)
- Format fichier : `{categorie}_{nom}_{locale}.txt`
- Ligne 1 = nom catégorie, ligne 2 = titre histoire, reste = phrases

### Étape 1 : Traduction des phrases
- **Script** : `step 1 translation/translate_text.py`
- Traduit vers toutes les `target_locales` définies dans `config.properties`
- **Prérequis** : `pip install google-cloud-translate`

### Étape 2 : Génération du fichier TSV
- **Script** : `step 2 generate_tsv/generate_tsv.py`
- Colonnes générées : `id | {locale1} | {locale2} | ... | file_name`
- Découpage par saut de ligne (les 2 premières lignes de chaque fichier sont ignorées)

### Étape 3 : Division en chunks
- **Script** : `step 3 chunk/split_tsv.py`
- Divise le TSV en fichiers de 50 lignes max

### Étape 4 : Génération de la base de données SQLite
- **Script** : `step 4 sqlite/generate_sqlite.py`
- Génère `vocabulary.db` avec toutes les tables
- **DB_VERSION** incrémentée automatiquement dans `DatabaseVersion.kt`
- **Destination** : `app/src/main/assets/vocabulary.db`
- Lit les 2 premières lignes de chaque fichier pour peupler `category_translation` et `story_translation`

### Étape 5 : Génération des fichiers audio
- **Script** : `step 5 audio/generate_audio.py`
- Génère des MP3 via Google Cloud Text-to-Speech pour toutes les locales
- Nommage : `phrase_{id}_{locale}.mp3`
- **Destination** : `app/src/main/res/raw/`
- **Prérequis** : `pip install google-cloud-texttospeech`

## Workflow complet

Lancer depuis la racine du projet :
```bash
python run_all.py
```

Ou étape par étape :
```bash
python "generated/step 1 translation/translate_text.py"
python "generated/step 2 generate_tsv/generate_tsv.py"
python "generated/step 3 chunk/split_tsv.py"
python "generated/step 4 sqlite/generate_sqlite.py"
python "generated/step 5 audio/generate_audio.py"
```

## Tables de la base de données

| Table | Description |
|-------|-------------|
| `category` | Catégories (id uniquement) |
| `category_translation` | Noms de catégorie par locale (peuplé depuis ligne 1 des fichiers) |
| `story` | Histoires (id uniquement) |
| `story_translation` | Titres d'histoire par locale (peuplé depuis ligne 2 des fichiers) |
| `sentence` | Phrases (id, category_id, story_id) |
| `translation` | Traductions des phrases (sentence_id, locale, texte) |
| `learning` | Progression (sentence_id, source_locale, target_locale, grade) |
| `configuration` | Paramètres app (native_language=fr, learned_language=de) |

## Intégration dans l'application

| Ressource | Destination | Accès dans l'app |
|-----------|-------------|-----------------|
| `vocabulary.db` | `app/src/main/assets/` | SQLDelight via `DatabaseDriverFactory` |
| `phrase_{id}_{locale}.mp3` | `app/src/main/res/raw/` | `AudioPlayer` (expect/actual) |

## Notes

- Le schéma SQLite doit correspondre exactement aux fichiers `.sq` dans `shared/commonMain/sqldelight/`
- Sur iOS, `vocabulary.db` doit être ajouté au bundle Xcode
- La `DB_VERSION` est auto-incrémentée à chaque génération — l'app recopie automatiquement la nouvelle base au démarrage
