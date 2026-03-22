# État actuel du code

## Architecture

Le projet est une application **Kotlin Multiplatform (KMP)** ciblant Android et iOS, avec une UI partagée via **Compose Multiplatform**.

## Structure du projet

```
vocabulary/
├── app/                                         # Module Android (point d'entrée)
│   └── src/main/java/com/example/myapplication/
│       └── MainActivity.kt                      # Initialise le repository et lance l'UI
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
│   │   └── kotlin/com/example/myapplication/
│   │       ├── data/
│   │       │   ├── DatabaseDriverFactory.kt     # expect (abstraction plateforme)
│   │       │   ├── DatabaseVersion.kt           # Constante DB_VERSION (auto-incrémentée)
│   │       │   ├── AudioPlayer.kt               # expect (abstraction plateforme)
│   │       │   └── VocabularyRepository.kt      # Accès aux données (suspend)
│   │       └── ui/
│   │           ├── AppNavigation.kt             # Navigation entre écrans
│   │           ├── StoryListScreen.kt           # Liste des histoires
│   │           ├── StoryViewModel.kt            # ViewModel histoires
│   │           ├── StoryWithTranslations.kt     # Data class histoire + traductions
│   │           ├── PhraseListScreen.kt          # Liste des phrases
│   │           ├── PhraseViewModel.kt           # ViewModel phrases
│   │           ├── PhraseWithTranslations.kt    # Data class phrase + traductions
│   │           └── theme/
│   │               ├── Color.kt
│   │               ├── Type.kt
│   │               └── Theme.kt
│   ├── androidMain/
│   │   ├── DatabaseDriverFactory.kt             # actual Android (copie assets avec versionning)
│   │   └── AudioPlayer.kt                       # actual Android (MediaPlayer)
│   └── iosMain/
│       ├── DatabaseDriverFactory.kt             # actual iOS (copie bundle avec versionning)
│       └── AudioPlayer.kt                       # actual iOS (AVAudioPlayer)
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

## Base de données

- Fichier `vocabulary.db` pré-rempli (SQLite)
- Chargé depuis les **assets** Android / **bundle** iOS au démarrage
- Recopié automatiquement si `DB_VERSION` a changé (SharedPreferences / NSUserDefaults)
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

## Fonctionnalités implémentées

- [x] Liste des histoires avec titres traduits (langue native + apprise)
- [x] Liste des phrases par histoire avec traductions
- [x] Lecture audio (`expect/actual` MediaPlayer / AVAudioPlayer)
- [x] Navigation entre écrans (Navigation Compose)
- [x] Langues configurables via table `configuration` (plus de hardcoding fr/de)
- [x] Architecture KMP (logique + UI partagées Android + iOS)
- [x] Thème Material 3 (clair/sombre)
- [x] Pipeline de génération multi-langues (config.properties)

## Fonctionnalités à implémenter

- [ ] Mode quiz (masquer/révéler la traduction)
- [ ] Suivi de progression via table `learning`
- [ ] Affichage des catégories avec traductions
- [ ] Filtrer les phrases apprises / non apprises
- [ ] Projet Xcode (`iosApp/`) pour tester sur iOS

## Notes plateforme

- **Android** : build et tests fonctionnels
- **iOS** : code prêt dans `iosMain/`, build nécessite macOS + Xcode
