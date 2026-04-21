# État actuel du code

## Architecture

Le projet est une application **Kotlin Multiplatform (KMP)** ciblant Android et iOS, avec une UI partagée via **Compose Multiplatform**.

## Structure du projet

```
vocabulary/
├── app/                                         # Module Android (point d'entrée)
│   └── src/main/java/com/example/myapplication/
│       └── MainActivity.kt                      # Initialise les repositories et lance l'UI
│
├── shared/                                      # Module KMP partagé (Android + iOS)
│   ├── commonMain/
│   │   ├── sqldelight/com/example/myapplication/db/
│   │   │   ├── Category.sq                      # Schéma + requêtes catégories
│   │   │   ├── CategoryTranslation.sq           # Traductions des noms de catégories
│   │   │   ├── Story.sq                         # Schéma + requêtes histoires
│   │   │   ├── StoryTranslation.sq              # Traductions des titres d'histoires
│   │   │   ├── StoryCategory.sq                 # Liaison story ↔ category
│   │   │   ├── Sentence.sq                      # Schéma + requêtes phrases (PK = sentence_key)
│   │   │   ├── Translation.sq                   # Traductions des phrases (FK sentence_key)
│   │   │   └── Configuration.sq                 # Paramètres app (native_language, learned_language)
│   │   ├── sqldelight-dictionary/com/example/myapplication/db/dictionary/
│   │   │   ├── DictEntry.sq                     # Schéma + requêtes (getById, getByLemma case-insensitive, searchByPrefix)
│   │   │   ├── DictTranslation.sq               # Traductions des entrées
│   │   │   └── DictForm.sq                      # Formes fléchies + searchByFormExact + searchByFormPrefix
│   │   ├── sqldelight-learning/com/example/myapplication/db/learning/
│   │   │   ├── Learning.sq                      # Suivi progression par phrase (sentence_key, source_locale, target_locale, grade)
│   │   │   └── WordLearning.sq                  # Suivi progression par mot (entry_id, translation_id, grade)
│   │   └── kotlin/com/example/myapplication/
│   │       ├── data/
│   │       │   ├── DatabaseDriverFactory.kt     # expect (abstraction plateforme)
│   │       │   ├── DatabaseVersion.kt           # Constante DB_VERSION (auto-incrémentée)
│   │       │   ├── DictionaryDriverFactory.kt   # expect (abstraction plateforme dictionnaire)
│   │       │   ├── LearningDriverFactory.kt     # expect (abstraction plateforme learning)
│   │       │   ├── AudioPlayer.kt               # expect (abstraction plateforme)
│   │       │   ├── SpeechRecognizer.kt          # expect (abstraction plateforme)
│   │       │   ├── VocabularyRepository.kt      # Accès aux données vocabulaire (suspend)
│   │       │   ├── DictionaryRepository.kt      # Accès aux données dictionnaire (bloquant, Dispatchers.Default)
│   │       │   └── LearningRepository.kt        # Accès progression phrases + mots (suspend)
│   │       └── ui/
│   │           ├── AppNavigation.kt             # Navigation + routes (HomeRoute, StoriesRoute, ReviewSelectionRoute,
│   │           │                                #   DictionaryRoute(initialQuery), DictionaryDetailRoute, SentencesRoute,
│   │           │                                #   ReviewRoute, WordReviewRoute(reversed))
│   │           ├── HomeScreen.kt                # Écran d'accueil (3 boutons : Lecture, Révision, Dictionnaire)
│   │           ├── StoryListScreen.kt           # Liste des histoires (avec TopAppBar + retour)
│   │           ├── StoryViewModel.kt            # ViewModel histoires + langues + compteurs révision + mots
│   │           ├── StoryWithTranslations.kt     # Data class histoire + traductions
│   │           ├── ReviewSelectionScreen.kt     # Sélection du mode (section Phrases 3 boutons + section Mots 2 boutons)
│   │           ├── ReviewScreen.kt              # Écran de révision phrases
│   │           ├── ReviewViewModel.kt           # ViewModel révision phrases
│   │           ├── ReviewPlayer.kt              # Lecteur audio pour la révision
│   │           ├── WordReviewScreen.kt          # Révision des mots du dictionnaire (carte + révélation + notes 1-5)
│   │           ├── WordReviewViewModel.kt       # ViewModel révision mots
│   │           ├── SentenceScreen.kt            # Conteneur écran phrases
│   │           ├── SentenceListScreen.kt        # Liste des phrases (mots cliquables → dictionnaire)
│   │           ├── SentenceDetailScreen.kt      # Détail d'une phrase
│   │           ├── SentenceViewModel.kt         # ViewModel phrases + grades chargés
│   │           ├── SentenceWithTranslations.kt  # Data class phrase + traductions (sentenceKey: String)
│   │           ├── DictionaryScreen.kt          # Recherche dictionnaire (initialQuery, debounce, résultats bilingues)
│   │           ├── DictionaryViewModel.kt       # ViewModel recherche (exact lemma+forme d'abord, puis partiel %q%)
│   │           ├── DictionaryDetailScreen.kt    # Détail d'une entrée (SwipeableGradeCard par traduction)
│   │           ├── DictionaryDetailViewModel.kt # ViewModel détail (wordGrades, saveWordGrade)
│   │           ├── LocaleFlag.kt               # localeToFlag() + gradeColor()
│   │           └── theme/
│   │               ├── Color.kt
│   │               ├── Type.kt
│   │               └── Theme.kt
│   ├── androidMain/
│   │   ├── DatabaseDriverFactory.kt             # actual Android (copie assets avec versionning)
│   │   ├── DictionaryDriverFactory.kt           # actual Android (copie assets avec versionning)
│   │   ├── LearningDriverFactory.kt             # actual Android (AndroidSqliteDriver + migration word_learning)
│   │   ├── AudioPlayer.kt                       # actual Android (MediaPlayer)
│   │   └── SpeechRecognizer.kt                  # actual Android
│   └── iosMain/
│       ├── DatabaseDriverFactory.kt             # actual iOS (copie bundle avec versionning)
│       ├── DictionaryDriverFactory.kt           # actual iOS
│       ├── LearningDriverFactory.kt             # actual iOS
│       ├── AudioPlayer.kt                       # actual iOS (AVAudioPlayer)
│       └── SpeechRecognizer.kt                  # actual iOS
│
└── generated/                                   # Scripts de génération des ressources
    ├── content/
    │   ├── step 4 sqlite/generate_sqlite.py     # Génère vocabulary.db (sentence_key comme PK)
    │   └── step 5 audio/generate_audio.py       # Génère les MP3 (nommés sentence_{key}_{locale}.mp3)
    └── dictionary/                              # Scripts de génération du dictionnaire
```

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Langage | Kotlin 2.2.10 |
| UI | Compose Multiplatform 1.7.3 |
| Base de données | SQLite via SQLDelight 2.0.2 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Multiplateforme | Kotlin Multiplatform (KMP) |
| Build | Gradle avec AGP 9.1.0 |

## Bases de données

### vocabulary.db

- Fichier SQLite pré-rempli, chargé depuis les **assets** Android / **bundle** iOS au démarrage
- Recopié automatiquement si `DB_VERSION` a changé
- Tables :

| Table | Description |
|-------|-------------|
| `category` | Catégories (id uniquement) |
| `category_translation` | Noms de catégorie par locale |
| `story` | Histoires (id uniquement) |
| `story_translation` | Titres d'histoire par locale |
| `story_category` | Liaison story ↔ category |
| `sentence` | Phrases (**sentence_key TEXT PK**, category_id, story_id) |
| `translation` | Traductions des phrases (**sentence_key**, locale, text) |
| `configuration` | Paramètres app (native_language=fr, learned_language=de) |

> `sentence_key` est une clé stable de la forme `{nom_fichier}_{index_ligne}`, indépendante des régénérations.

### dictionary.db (91.8 MB)

- Dictionnaire bilingue de↔fr avec morphologie complète (kaikki.org / Wiktionnaire CC BY-SA)
- 100 150 entrées, 173 386 traductions, ~1 079 963 formes fléchies
- Tables :

| Table | Description |
|-------|-------------|
| `dict_entry` | Entrées (lemma, locale, pos, gender, example fallback) |
| `dict_features` | Features dédupliquées (4 806 chaînes uniques) |
| `dict_translation` | Traductions (text, gloss_source, example précis) |
| `dict_form` | Formes fléchies (form, features_id, pronouns) |

### learning.db

- Base de données locale créée à l'exécution (pas dans les assets)
- Migration via `AndroidSqliteDriver.Callback.onUpgrade` (CREATE TABLE IF NOT EXISTS)
- Tables :

| Table | Description |
|-------|-------------|
| `learning` | Progression phrases (sentence_key, source_locale, target_locale, grade) |
| `word_learning` | Progression mots (entry_id, translation_id, grade — PK composite) |

## Navigation

```
HomeScreen
├── Lecture      → StoryListScreen → SentenceScreen (mots cliquables)
│                                         └── mot cliqué → DictionaryScreen → DictionaryDetailScreen
├── Révision     → ReviewSelectionScreen
│                    ├── [Phrases] → ReviewScreen
│                    └── [Mots]    → WordReviewScreen
└── Dictionnaire → DictionaryScreen → DictionaryDetailScreen
```

## Fonctionnalités implémentées

- [x] Écran d'accueil avec 3 boutons (Lecture, Révision, Dictionnaire)
- [x] Liste des histoires avec titres traduits (langue native + apprise)
- [x] Liste des phrases par histoire avec traductions
- [x] Lecture audio (`expect/actual` MediaPlayer / AVAudioPlayer)
- [x] Mode révision phrases avec sélection du mode (3 directions/modes)
- [x] Mode révision mots (carte + réponse floutée, notes 1-5, grade 5 = maîtrisé retiré)
- [x] Navigation Compose entre écrans (type-safe routes)
- [x] Langues configurables via table `configuration`
- [x] Architecture KMP (logique + UI partagées Android + iOS)
- [x] Thème Material 3 (clair/sombre)
- [x] Pipeline de génération multi-langues (config.properties)
- [x] Recherche dictionnaire bilingue (de↔fr, debounce 300ms, exact en premier, puis partiel)
- [x] Recherche dans les formes fléchies (exact case-insensitive prioritaire, puis LIKE %q%)
- [x] Détail d'une entrée dictionnaire (traductions + exemples + tableau de formes)
- [x] Sauvegarde de mots à apprendre avec grade (SwipeableGradeCard dans DictionaryDetailScreen)
- [x] Mise en valeur de la note sauvegardée (fond coloré + texte en gras)
- [x] Clic sur un mot dans les phrases → ouverture du dictionnaire avec le mot pré-rempli
- [x] Suivi de progression phrases (`learning` table) avec grade affiché dans SentenceListScreen
- [x] Suivi de progression mots (`word_learning` table) avec compteur dans ReviewSelectionScreen

## Fonctionnalités à implémenter

- [ ] Filtrer les phrases apprises / non apprises
- [ ] Filtrage dictionnaire par fréquence (Hermit Dave wordlists)
- [ ] Affichage des catégories avec traductions
- [ ] Écran de configuration (choisir les langues)
- [ ] Projet Xcode (`iosApp/`) pour tester sur iOS

## Notes plateforme

- **Android** : build et tests fonctionnels
- **iOS** : code prêt dans `iosMain/`, build nécessite macOS + Xcode
