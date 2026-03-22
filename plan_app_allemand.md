# Plan d'action — Application d'apprentissage de phrases en allemand

## Objectif

Application **Kotlin Multiplatform (Android + iOS)** pour apprendre des phrases en plusieurs langues. UI partagée via Compose Multiplatform. Les langues source et cible sont configurables dynamiquement.

## Architecture cible

```
shared/commonMain/     ← logique métier + UI partagées
shared/androidMain/    ← implémentations Android spécifiques
shared/iosMain/        ← implémentations iOS spécifiques
app/                   ← point d'entrée Android
iosApp/                ← point d'entrée iOS (Xcode)
```

## Fonctionnalités

### Écrans principaux

- **Liste des histoires** : point d'entrée, titres en langue native + apprise
- **Liste des phrases** : phrases d'une histoire (langue native + apprise)
- **Mode quiz** : masquer la traduction, la révéler après un clic
- **Écran de configuration** : choisir les langues native et apprise

### Fonctionnalités

- Lecture audio par phrase et par locale
- Suivi de progression par phrase (grade dans table `learning`)
- Filtrer les phrases apprises / non apprises
- Langues entièrement configurables (plus de hardcoding fr/de)

## Étapes de développement

### ✅ Étape 1 : Configuration du projet
- Projet Android créé avec Kotlin + Jetpack Compose
- Migration vers **Kotlin Multiplatform** (Android + iOS)
- Plugin AGP `com.android.kotlin.multiplatform.library`

### ✅ Étape 2 : Base de données
- Base SQLite pré-remplie (`vocabulary.db` dans assets)
- Migration de **Room** vers **SQLDelight** (compatible KMP)
- `DatabaseDriverFactory` en `expect/actual` par plateforme
- Versionning automatique (DB_VERSION auto-incrémentée à la génération)
- Tables : `category`, `category_translation`, `story`, `story_translation`, `sentence`, `translation`, `learning`, `configuration`

### ✅ Étape 3 : UI partagée
- Thème Material 3 dans `shared/commonMain`
- Navigation Compose entre écrans
- `StoryListScreen` : liste des histoires avec titres traduits
- `PhraseListScreen` : liste des phrases avec traductions et boutons audio

### ✅ Étape 4 : Multi-langues
- `config.properties` pilote la génération (source_locale, target_locales, voix)
- Plus de hardcoding fr/de dans l'app : langues lues depuis la table `configuration`
- Pipeline de génération entièrement dynamique

### ✅ Étape 5 : Lecture audio
- `expect/actual` pour le lecteur audio
  - Android : `MediaPlayer`
  - iOS : `AVAudioPlayer`
- Bouton "▶" sur chaque phrase pour chaque locale

### 🔲 Étape 6 : Mode quiz
- Masquer/révéler la traduction
- Enregistrer le grade dans la table `learning`

### 🔲 Étape 7 : Catégories
- Afficher les catégories avec leurs traductions
- Navigation catégorie → histoires → phrases

### 🔲 Étape 8 : Projet iOS
- Créer `iosApp/` (projet Xcode)
- Tester sur simulateur iOS (nécessite macOS)

### 🔲 Étape 9 : Tests
- Tests unitaires dans `shared/commonTest`
- Tests sur émulateur Android
- Tests sur simulateur iOS

## Pipeline de génération

```
step 0 source/        → fichiers texte sources (2 premières lignes = catégorie + titre)
step 1 translation/   → traduction via Google Cloud Translation (toutes les locales)
step 2 generate_tsv/  → TSV : id | locale1 | locale2 | ... | file_name
step 3 chunk/         → découpage en chunks
step 4 sqlite/        → vocabulary.db (DB_VERSION auto-incrémentée)
step 5 audio/         → phrase_{id}_{locale}.mp3 via Google Cloud TTS
```

Tout est piloté par `config.properties` :
```properties
[languages]
source_locale = fr
target_locales = de,en

[voices]
fr = fr-FR:fr-FR-Wavenet-A
de = de-DE:de-DE-Wavenet-A
en = en-US:en-US-Wavenet-A
```

## Ressources audio

- Nommage : `phrase_{id}_{locale}.mp3`
- Stockage Android : `app/src/main/res/raw/`
- Stockage iOS : à intégrer dans le bundle Xcode

## Notes techniques

- `Dispatchers.Default` utilisé dans `commonMain` (pas de `Dispatchers.IO` en KMP)
- SQLDelight : table à 1 colonne → retourne `Long` directement (pas de data class)
- Build iOS nécessite macOS — code KMP prêt mais non testable sur Windows
