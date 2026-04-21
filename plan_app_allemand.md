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
- **Liste des phrases** : phrases d'une histoire (mots cliquables → dictionnaire)
- **Sélection révision** : section Phrases (3 modes) + section Mots (2 directions)
- **Révision phrases** : masquer/révéler la traduction, noter 1-5
- **Révision mots** : carte mot↔traduction, réponse floutée, noter 1-5 (grade 5 = maîtrisé)
- **Dictionnaire** : recherche bilingue de↔fr avec debounce, exact en premier
- **Détail dictionnaire** : traductions notables (SwipeableGradeCard) + exemples + tableau de conjugaison
- **Écran de configuration** : choisir les langues native et apprise

### Fonctionnalités

- Lecture audio par phrase et par locale
- Suivi de progression par phrase et par mot (grade dans tables `learning` / `word_learning`)
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
- Tables : `category`, `category_translation`, `story`, `story_translation`, `sentence`, `translation`, `configuration`
- `sentence_key TEXT PK` = `{nom_fichier}_{index_ligne}` — clé stable entre régénérations

### ✅ Étape 3 : UI partagée
- Thème Material 3 dans `shared/commonMain`
- Navigation Compose type-safe entre écrans (8 routes)
- `HomeScreen` : écran d'accueil avec 3 boutons (Lecture, Révision, Dictionnaire)
- `StoryListScreen` : liste des histoires avec titres traduits + TopAppBar
- `ReviewSelectionScreen` : section Phrases (3 boutons) + section Mots (2 boutons)
- `SentenceListScreen` : phrases avec traductions, mots cliquables et SwipeableGradeCard

### ✅ Étape 4 : Multi-langues
- `config.properties` pilote la génération (source_locale, target_locales, voix)
- Plus de hardcoding fr/de dans l'app : langues lues depuis la table `configuration`
- Pipeline de génération entièrement dynamique

### ✅ Étape 5 : Lecture audio
- `expect/actual` pour le lecteur audio
  - Android : `MediaPlayer`
  - iOS : `AVAudioPlayer`
- Bouton "▶" sur chaque phrase pour chaque locale
- Fichiers audio nommés `sentence_{sentence_key}_{locale}.mp3`

### ✅ Étape 6 : Dictionnaire bilingue
- `DictionaryScreen` : recherche bilingue de↔fr (debounce 300ms, exact en premier, puis %q%)
  - Recherche d'abord dans les lemmes (exact case-insensitive), puis dans les formes fléchies
  - Résultats dédupliqués, exacts prioritaires sur les partiels
- `DictionaryDetailScreen` : détail d'une entrée
  - Traductions avec gloss source (italique) et exemples
  - `SwipeableGradeCard` : glisser à gauche pour noter 1-5, note sauvegardée mise en valeur
  - Tableau de formes groupé par temps/cas (ordre grammatical logique)
- `DictionaryRepository` : `getById`, `getByLemma`, `searchExactByForm`, `searchByFormPattern`

### ✅ Étape 7 : Suivi de progression
- **Phrases** : table `learning` (sentence_key, source_locale, target_locale, grade)
  - Grade affiché dans `SentenceListScreen` via `SwipeableGradeCard`
  - Mode révision avec notes 1-5 dans `ReviewScreen`
- **Mots** : table `word_learning` (entry_id, translation_id, grade)
  - Notation dans `DictionaryDetailScreen` via `SwipeableGradeCard`
  - Mode révision dédié `WordReviewScreen` avec carte mot↔traduction
  - Grade 5 = maîtrisé, retire le mot de la liste de révision
  - Compteur affiché dans `ReviewSelectionScreen`
- `LearningDriverFactory` en `expect/actual` avec migration `onUpgrade` pour `word_learning`

### ✅ Étape 8 : Clic mot → dictionnaire
- Dans `SentenceListScreen`, les mots de la traduction sont cliquables (`AnnotatedString` + `ClickableText`)
- Clic sur un mot navigue vers `DictionaryScreen` avec le mot pré-rempli (`DictionaryRoute(initialQuery)`)

### 🔲 Étape 9 : Catégories
- Afficher les catégories avec leurs traductions
- Navigation catégorie → histoires → phrases

### 🔲 Étape 10 : Projet iOS
- Créer `iosApp/` (projet Xcode)
- Tester sur simulateur iOS (nécessite macOS)

### 🔲 Étape 11 : Tests
- Tests unitaires dans `shared/commonTest`
- Tests sur émulateur Android
- Tests sur simulateur iOS

## Pipeline de génération

```
step 0 source/        → fichiers texte sources (2 premières lignes = catégorie + titre)
step 1 translation/   → traduction via Google Cloud Translation (toutes les locales)
step 2 generate_tsv/  → TSV : sentence_key | locale1 | locale2 | ... | file_name
step 3 chunk/         → découpage en chunks
step 4 sqlite/        → vocabulary.db (sentence_key PK, DB_VERSION auto-incrémentée)
step 5 audio/         → sentence_{sentence_key}_{locale}.mp3 via Google Cloud TTS
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

- Nommage : `sentence_{sentence_key}_{locale}.mp3`
- `sentence_key` = `{nom_fichier}_{index_ligne}` → stable entre régénérations
- Stockage Android : `app/src/main/res/raw/`
- Le script vérifie `os.path.exists` avant d'appeler l'API TTS (pas de re-génération inutile)

## Notes techniques

- `Dispatchers.Default` utilisé dans `commonMain` (pas de `Dispatchers.IO` en KMP)
- SQLDelight : table à 1 colonne → retourne `Long` directement (pas de data class)
- Build iOS nécessite macOS — code KMP prêt mais non testable sur Windows
- Références cross-DB (word_learning → dict_entry/dict_translation) gérées au niveau applicatif (pas de FK SQLite entre fichiers différents)
- `SwipeableGradeCard` : le `.background()` doit être placé **après** `.offset{}` dans le modifier pour suivre le mouvement de la carte
