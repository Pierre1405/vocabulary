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
│   │   │   ├── Story.sq                         # Schéma + requêtes histoires
│   │   │   ├── Phrase.sq                        # Schéma + requêtes phrases
│   │   │   └── StoryCategory.sq                 # Schéma + requêtes liaison
│   │   └── kotlin/com/example/myapplication/
│   │       ├── data/
│   │       │   ├── DatabaseDriverFactory.kt     # expect (abstraction plateforme)
│   │       │   └── VocabularyRepository.kt      # Accès aux données (suspend)
│   │       └── ui/
│   │           ├── CategoryViewModel.kt          # ViewModel KMP
│   │           ├── CategoryListScreen.kt         # Écran Compose partagé
│   │           └── theme/
│   │               ├── Color.kt
│   │               ├── Type.kt
│   │               └── Theme.kt
│   ├── androidMain/
│   │   └── DatabaseDriverFactory.kt             # actual Android (copie assets)
│   └── iosMain/
│       └── DatabaseDriverFactory.kt             # actual iOS (copie bundle)
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

- Fichier `vocabulary.db` pré-rempli (SQLite, 28 Ko)
- Chargé depuis les **assets** Android / **bundle** iOS au premier démarrage
- 4 tables : `category`, `story`, `phrases`, `story_category`
- Accès via **SQLDelight** (génération de code à la compilation)

## Fonctionnalités implémentées

- [x] Affichage de la liste des catégories
- [x] Architecture KMP (logique + UI partagées)
- [x] Base de données SQLite pré-remplie
- [x] Thème Material 3 (clair/sombre)
- [x] Fichiers audio pré-générés (`res/raw/phrase_{id}_{lang}.mp3`)

## Fonctionnalités à implémenter

- [ ] Écran liste des phrases par catégorie
- [ ] Mode quiz (masquer/révéler la traduction)
- [ ] Lecture audio (`expect/actual` MediaPlayer / AVAudioPlayer)
- [ ] Suivi des progrès (marquer une phrase comme apprise)
- [ ] Projet Xcode (`iosApp/`) pour tester sur iOS

## Notes plateforme

- **Android** : build et tests fonctionnels
- **iOS** : code prêt dans `iosMain/`, build nécessite macOS + Xcode
