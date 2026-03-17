# Plan d'action — Application d'apprentissage de phrases en allemand

## Objectif

Application **Kotlin Multiplatform (Android + iOS)** pour apprendre des phrases en allemand avec leurs traductions en français. UI partagée via Compose Multiplatform.

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

- **Liste des catégories** : point d'entrée de l'application
- **Liste des phrases** : phrases d'une catégorie (allemand + français)
- **Mode quiz** : masquer la traduction, la révéler après un clic
- **Écran phrase** : affichage détaillé avec bouton audio

### Fonctionnalités

- Navigation entre les phrases (suivant/précédent)
- Marquer une phrase comme apprise
- Filtrer les phrases apprises / non apprises
- Lecture audio pré-générée (allemand et français)

## Étapes de développement

### ✅ Étape 1 : Configuration du projet
- Projet Android créé avec Kotlin + Jetpack Compose
- Migration vers **Kotlin Multiplatform** (Android + iOS)
- Plugin AGP `com.android.kotlin.multiplatform.library`

### ✅ Étape 2 : Base de données
- Base SQLite pré-remplie (`vocabulary.db` dans assets)
- Migration de **Room** vers **SQLDelight** (compatible KMP)
- 4 tables : `category`, `story`, `phrases`, `story_category`
- `DatabaseDriverFactory` en `expect/actual` par plateforme

### ✅ Étape 3 : UI partagée
- Thème Material 3 dans `shared/commonMain`
- `CategoryViewModel` KMP avec `StateFlow`
- `CategoryListScreen` en Compose Multiplatform
- `MainActivity` simplifié (point d'entrée uniquement)

### 🔲 Étape 4 : Écran phrases
- `PhraseListScreen` dans `shared/commonMain`
- `PhraseViewModel` avec filtrage par catégorie
- Navigation entre écrans

### 🔲 Étape 5 : Mode quiz
- Masquer/révéler la traduction
- Marquer comme apprise (mise à jour BDD via SQLDelight)

### 🔲 Étape 6 : Lecture audio
- `expect/actual` pour le lecteur audio
  - Android : `MediaPlayer`
  - iOS : `AVAudioPlayer`
- Bouton "Écouter" sur chaque phrase

### 🔲 Étape 7 : Projet iOS
- Créer `iosApp/` (projet Xcode)
- Tester sur simulateur iOS (nécessite macOS)

### 🔲 Étape 8 : Tests
- Tests unitaires dans `shared/commonTest`
- Tests sur émulateur Android
- Tests sur simulateur iOS

## Ressources audio

Les fichiers audio sont pré-générés via Google Cloud Text-to-Speech :
- Nommage : `phrase_{id}_de.mp3` et `phrase_{id}_fr.mp3`
- Stockage Android : `app/src/main/res/raw/`
- Stockage iOS : à intégrer dans le bundle Xcode

## Notes techniques

- `Dispatchers.Default` utilisé dans `commonMain` (pas de `Dispatchers.IO` en KMP)
- Les classes SQLDelight sont nommées d'après les tables SQL (`Phrases`, `Story_category`)
- Build iOS nécessite macOS — code KMP prêt mais non testable sur Windows
