# Répétition espacée (SM-2)

## Principe

Chaque mot ou phrase appris possède un **intervalle en heures** qui grandit à chaque révision réussie. Plus la note est élevée, plus l'intervalle s'allonge. Un échec remet l'intervalle à 0 ou 1 heure.

La prochaine révision est calculée à chaque fois qu'une note est attribuée :

```
prochaine_révision = maintenant + intervalle (en heures)
```

## Notes

| Note | Signification         | Résultat        |
|------|-----------------------|-----------------|
| 1    | Pas du tout su        | Échec — revu immédiatement |
| 2    | Difficile, hésitation | Échec — revu dans 1 heure  |
| 3    | Su avec effort        | Succès — intervalle × 1.5  |
| 4    | Su facilement         | Succès — intervalle × 2.0  |
| 5    | Parfait               | Succès — intervalle × 2.5  |

## Calcul de l'intervalle

```
note 1  →  0 h   (dû immédiatement)
note 2  →  1 h
note 3  →  intervalle actuel × 1.5  (défaut : 24 h si premier succès)
note 4  →  intervalle actuel × 2.0  (défaut : 24 h si premier succès)
note 5  →  intervalle actuel × 2.5  (défaut : 24 h si premier succès)
```

## Premier ajout

Quand un mot ou une phrase est noté pour la première fois (depuis le dictionnaire ou la liste de phrases), il est immédiatement disponible en révision (`next_review = maintenant`). L'intervalle commence à s'accumuler à partir de la **première révision** dans l'écran dédié.

## Exemples

### Avec note 4 constante

| Révision | Intervalle actuel | Nouvel intervalle | Prochaine révision |
|----------|-------------------|-------------------|--------------------|
| 1        | 0                 | 24 h (défaut)     | +1 jour            |
| 2        | 24 h              | 48 h              | +2 jours           |
| 3        | 48 h              | 96 h              | +4 jours           |
| 4        | 96 h              | 192 h             | +8 jours           |
| 5        | 192 h             | 384 h             | +16 jours          |

### Avec notes variables

| Révision | Note | Intervalle actuel | Nouvel intervalle |
|----------|------|-------------------|-------------------|
| 1        | 4    | 0                 | 24 h (défaut)     |
| 2        | 3    | 24 h              | 36 h              |
| 3        | 5    | 36 h              | 90 h              |
| 4        | 2    | 90 h              | 1 h  ← échec      |
| 5        | 4    | 1 h               | 2 h               |
| 6        | 4    | 2 h               | 4 h               |

## Comportement dans l'application

- Les écrans de révision ne montrent que les items dont `prochaine_révision ≤ maintenant`.
- Les compteurs sur l'écran de sélection affichent le nombre d'items dus à l'instant.
- Les boutons sont désactivés s'il n'y a rien à réviser.
- La liste d'une session est figée à l'ouverture de l'écran — les items ne disparaissent pas en cours de révision.
- Il n'y a pas de suppression définitive : même une note 5 replanifie la révision dans le futur.

## Stockage

Les données sont stockées dans la table `learning` :

| Colonne          | Type    | Description                              |
|------------------|---------|------------------------------------------|
| `key`            | TEXT    | Clé de la phrase ou ID de la traduction  |
| `source_locale`  | TEXT    | Langue source (ex: `fr`)                 |
| `target_locale`  | TEXT    | Langue cible (ex: `de`)                  |
| `grade`          | INTEGER | Dernière note (1–5)                      |
| `type`           | TEXT    | `sentence` ou `word`                     |
| `interval_hours` | INTEGER | Intervalle actuel en heures              |
| `next_review`    | INTEGER | Epoch heures de la prochaine révision    |
