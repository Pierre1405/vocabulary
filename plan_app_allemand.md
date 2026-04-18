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

- **Accueil** : point d'entrée, 3 boutons (Lecture, Révision, Dictionnaire)
- **Liste des histoires** : titres en langue native + apprise
- **Liste des phrases** : phrases d'une histoire (langue native + apprise)
- **Sélection révision** : choix du mode parmi les 3 directions/modes disponibles
- **Mode quiz** : masquer la traduction, la révéler après un clic
- **Dictionnaire** : recherche bilingue de↔fr avec debounce
- **Détail dictionnaire** : traductions + exemples + tableau de conjugaison/déclinaison
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
- Navigation Compose type-safe entre écrans (7 routes)
- `HomeScreen` : écran d'accueil avec 3 boutons (Lecture, Révision, Dictionnaire)
- `StoryListScreen` : liste des histoires avec titres traduits + TopAppBar
- `ReviewSelectionScreen` : sélection du mode de révision
- `SentenceListScreen` / `SentenceDetailScreen` : phrases avec traductions et boutons audio

### ✅ Étape 4 : Multi-langues
- `config.properties` pilote la génération (source_locale, target_locales, voix)
- Plus de hardcoding fr/de dans l'app : langues lues depuis la table `configuration`
- Pipeline de génération entièrement dynamique

### ✅ Étape 5 : Lecture audio
- `expect/actual` pour le lecteur audio
  - Android : `MediaPlayer`
  - iOS : `AVAudioPlayer`
- Bouton "▶" sur chaque phrase pour chaque locale

### ✅ Étape 6 : Dictionnaire bilingue
- `DictionaryScreen` : recherche bilingue de↔fr (debounce 300ms, 10 résultats triés)
- `DictionaryDetailScreen` : détail d'une entrée
  - Traductions avec gloss source (italique) et exemples
  - Tableau de formes groupé par temps/cas (ordre grammatical logique)
  - `DictionaryDetailViewModel` : `FormGroup`/`FormRow` calculés sur `Dispatchers.Default`
- `DictionaryRepository.getById` ajouté

### 🔲 Étape 7 : Mode quiz
- Masquer/révéler la traduction
- Enregistrer le grade dans la table `learning`

### 🔲 Étape 8 : Catégories
- Afficher les catégories avec leurs traductions
- Navigation catégorie → histoires → phrases

### 🔲 Étape 9 : Projet iOS
- Créer `iosApp/` (projet Xcode)
- Tester sur simulateur iOS (nécessite macOS)

### 🔲 Étape 10 : Tests
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
