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
│   │   │   ├── Sentence.sq                      # Schéma + requêtes phrases
│   │   │   ├── Translation.sq                   # Traductions des phrases (multi-locale)
│   │   │   ├── Learning.sq                      # Suivi de progression par phrase/locale
│   │   │   └── Configuration.sq                 # Paramètres app (native_language, learned_language)
│   │   ├── sqldelight-dictionary/com/example/myapplication/db/dictionary/
│   │   │   ├── DictEntry.sq                     # Schéma + requêtes entrées (getById, getByLemma, searchByPrefix)
│   │   │   ├── DictTranslation.sq               # Traductions des entrées
│   │   │   └── DictForm.sq                      # Formes fléchies + table dict_features
│   │   └── kotlin/com/example/myapplication/
│   │       ├── data/
│   │       │   ├── DatabaseDriverFactory.kt     # expect (abstraction plateforme)
│   │       │   ├── DatabaseVersion.kt           # Constante DB_VERSION (auto-incrémentée)
│   │       │   ├── DictionaryDriverFactory.kt   # expect (abstraction plateforme dictionnaire)
│   │       │   ├── AudioPlayer.kt               # expect (abstraction plateforme)
│   │       │   ├── SpeechRecognizer.kt          # expect (abstraction plateforme)
│   │       │   ├── VocabularyRepository.kt      # Accès aux données vocabulaire (suspend)
│   │       │   └── DictionaryRepository.kt      # Accès aux données dictionnaire (bloquant, appelé sur Dispatchers.Default)
│   │       └── ui/
│   │           ├── AppNavigation.kt             # Navigation + routes (HomeRoute, StoriesRoute, ReviewSelectionRoute,
│   │           │                                #   DictionaryRoute, DictionaryDetailRoute, SentencesRoute, ReviewRoute)
│   │           ├── HomeScreen.kt                # Écran d'accueil (3 boutons : Lecture, Révision, Dictionnaire)
│   │           ├── StoryListScreen.kt           # Liste des histoires (avec TopAppBar + retour)
│   │           ├── StoryViewModel.kt            # ViewModel histoires + langues + compteurs révision
│   │           ├── StoryWithTranslations.kt     # Data class histoire + traductions
│   │           ├── ReviewSelectionScreen.kt     # Sélection du mode de révision (3 boutons)
│   │           ├── ReviewScreen.kt              # Écran de révision
│   │           ├── ReviewViewModel.kt           # ViewModel révision
│   │           ├── ReviewPlayer.kt              # Lecteur audio pour la révision
│   │           ├── SentenceScreen.kt            # Conteneur écran phrases
│   │           ├── SentenceListScreen.kt        # Liste des phrases d'une histoire
│   │           ├── SentenceDetailScreen.kt      # Détail d'une phrase
│   │           ├── SentenceViewModel.kt         # ViewModel phrases
│   │           ├── SentenceWithTranslations.kt  # Data class phrase + traductions
│   │           ├── DictionaryScreen.kt          # Recherche dictionnaire (TextField + debounce + résultats bilingues)
│   │           ├── DictionaryViewModel.kt       # ViewModel recherche (debounce 300ms, recherche de→fr + fr→de)
│   │           ├── DictionaryDetailScreen.kt    # Détail d'une entrée (traductions + exemples + formes)
│   │           ├── DictionaryDetailViewModel.kt # ViewModel détail (FormGroup/FormRow triés grammaticalement)
│   │           ├── LocaleFlag.kt               # localeToFlag() + gradeColor()
│   │           └── theme/
│   │               ├── Color.kt
│   │               ├── Type.kt
│   │               └── Theme.kt
│   ├── androidMain/
│   │   ├── DatabaseDriverFactory.kt             # actual Android (copie assets avec versionning)
│   │   ├── DictionaryDriverFactory.kt           # actual Android (copie assets avec versionning)
│   │   ├── AudioPlayer.kt                       # actual Android (MediaPlayer)
│   │   └── SpeechRecognizer.kt                  # actual Android
│   └── iosMain/
│       ├── DatabaseDriverFactory.kt             # actual iOS (copie bundle avec versionning)
│       ├── DictionaryDriverFactory.kt           # actual iOS
│       ├── AudioPlayer.kt                       # actual iOS (AVAudioPlayer)
│       └── SpeechRecognizer.kt                  # actual iOS
│
└── generated/                                   # Scripts de génération des ressources
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
| `sentence` | Phrases (id, category_id, story_id) |
| `translation` | Traductions des phrases (sentence_id, locale, text) |
| `learning` | Progression (sentence_id, source_locale, target_locale, grade) |
| `configuration` | Paramètres app (native_language=fr, learned_language=de) |

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

## Navigation

```
HomeScreen
├── Lecture      → StoryListScreen → SentenceScreen
├── Révision     → ReviewSelectionScreen → ReviewScreen
└── Dictionnaire → DictionaryScreen → DictionaryDetailScreen
```

## Fonctionnalités implémentées

- [x] Écran d'accueil avec 3 boutons (Lecture, Révision, Dictionnaire)
- [x] Liste des histoires avec titres traduits (langue native + apprise)
- [x] Liste des phrases par histoire avec traductions
- [x] Lecture audio (`expect/actual` MediaPlayer / AVAudioPlayer)
- [x] Mode révision avec sélection du mode (3 directions/modes)
- [x] Navigation Compose entre écrans (type-safe routes)
- [x] Langues configurables via table `configuration`
- [x] Architecture KMP (logique + UI partagées Android + iOS)
- [x] Thème Material 3 (clair/sombre)
- [x] Pipeline de génération multi-langues (config.properties)
- [x] Recherche dictionnaire bilingue (de↔fr, debounce 300ms, 10 résultats)
- [x] Détail d'une entrée dictionnaire (traductions + exemples + tableau de formes)

## Fonctionnalités à implémenter

- [ ] Mode quiz (masquer/révéler la traduction)
- [ ] Suivi de progression via table `learning`
- [ ] Affichage des catégories avec traductions
- [ ] Filtrer les phrases apprises / non apprises
- [ ] Filtrage dictionnaire par fréquence (Hermit Dave wordlists)
- [ ] Projet Xcode (`iosApp/`) pour tester sur iOS

## Notes plateforme

- **Android** : build et tests fonctionnels
- **iOS** : code prêt dans `iosMain/`, build nécessite macOS + Xcode
