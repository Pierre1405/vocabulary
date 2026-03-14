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

## Prochaines étapes
- [ ] Créer le projet dans Android Studio.
- [ ] Configurer la base de données.
- [ ] Concevoir l'interface utilisateur.
- [ ] Implémenter la logique de l'application.
- [ ] Tester l'application.
