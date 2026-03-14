# Plan d'action pour l'application d'apprentissage de phrases en allemand

## Objectif
Créer une application Android simple pour apprendre des phrases en allemand avec leurs traductions en français.

## Structure du projet

### 1. Activités principales
- **MainActivity** : Activité principale pour afficher les phrases et leurs traductions.
- **SettingsActivity** : (Optionnel) Pour gérer les paramètres de l'application.

### 2. Base de données
- Utiliser **Room** ou **SQLite** pour stocker les phrases et leurs traductions.
- Alternative : Utiliser un fichier JSON pour stocker les phrases initialement.

### 3. Interface utilisateur
- **Écran principal** :
  - Afficher la phrase en allemand.
  - Afficher la traduction en français (masquable pour un quiz).
  - Boutons pour naviguer entre les phrases (suivant/précédent).
  - Bouton pour marquer une phrase comme apprise.

### 4. Fonctionnalités
- **Affichage des phrases** : Afficher une phrase en allemand et sa traduction.
- **Navigation** : Boutons pour passer à la phrase suivante ou précédente.
- **Quiz** : Option pour masquer la traduction et la révéler après un clic.
- **Suivi des progrès** : Marquer les phrases comme apprises et les exclure du quiz.

## Étapes de développement

### Étape 1 : Configuration du projet
- Créer un nouveau projet Android dans Android Studio.
- Configurer les dépendances nécessaires (Room, ViewModel, etc.).

### Étape 2 : Création de la base de données
- Définir une entité `Phrase` avec les champs suivants :
  - `id` : Identifiant unique.
  - `allemand` : Phrase en allemand.
  - `francais` : Traduction en français.
  - `apprise` : Booléen pour indiquer si la phrase est apprise.
- Créer un DAO (Data Access Object) pour interagir avec la base de données.

### Étape 3 : Création de l'interface utilisateur
- Concevoir l'interface dans `activity_main.xml`.
- Ajouter des boutons pour la navigation et un texte pour afficher les phrases.

### Étape 4 : Logique de l'application
- Implémenter la logique pour afficher les phrases.
- Ajouter des listeners pour les boutons de navigation.
- Implémenter la fonctionnalité de quiz (masquer/révéler la traduction).

### Étape 5 : Tests
- Tester l'application sur un émulateur ou un appareil physique.
- Vérifier que toutes les fonctionnalités fonctionnent correctement.

## Ressources nécessaires
- Android Studio
- Kotlin ou Java
- Connaissances de base en développement Android
- Liste de phrases en allemand avec leurs traductions en français

## Fonctionnalités supplémentaires

### Lecture audio préenregistrée
Étant donné que les phrases sont prédéfini, il est préférable de pré-générer des fichiers audio pour garantir une qualité constante et une utilisation hors ligne.

### Étapes pour implémenter la lecture audio
1. **Générer les fichiers audio** :
   - Utiliser un outil comme **Amazon Polly** ou **Google Text-to-Speech** pour générer des fichiers audio (MP3 ou WAV) pour chaque phrase.
   - **Coûts estimés** :
     - Amazon Polly : ~4 $ pour 1000 phrases (0,004 $ par phrase de 100 caractères).
     - Google Text-to-Speech : ~4 $ pour 1000 phrases (0,004 $ par phrase de 100 caractères).
   - Enregistrer les fichiers dans le dossier `res/raw/` du projet Android.

2. **Stocker les fichiers audio** :
   - Placer les fichiers audio dans `res/raw/` (par exemple, `phrase1_de.mp3` pour l'allemand et `phrase1_fr.mp3` pour le français).

3. **Lire les fichiers audio** :
   - Utiliser `MediaPlayer` pour lire les fichiers audio :
     ```kotlin
     val mediaPlayer = MediaPlayer.create(this, R.raw.phrase1_de)
     mediaPlayer.start()
     ```

4. **Mettre à jour la base de données** :
   - Ajouter des champs `audio_file_de` et `audio_file_fr` dans l'entité `Phrase` pour stocker les noms des fichiers audio.

5. **Ajouter un bouton "Écouter" dans l'interface utilisateur** :
   - Permettre à l'utilisateur de déclencher la lecture audio.

### Avantages de cette approche
- **Qualité audio** : Contrôle total sur la prononciation et la qualité.
- **Performance** : Pas besoin d'attendre la synthèse vocale à chaque fois.
- **Hors ligne** : L'application fonctionne sans connexion internet.

## Prochaines étapes
- [ ] Créer le projet dans Android Studio.
- [ ] Configurer la base de données.
- [ ] Concevoir l'interface utilisateur.
- [ ] Implémenter la logique de l'application.
- [ ] Ajouter la synthèse vocale.
- [ ] Tester l'application.
