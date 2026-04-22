# Répétition espacée (SM-2)

## Principe

Chaque mot ou phrase appris possède un **intervalle** (en jours) qui grandit à chaque révision réussie. Plus la note est élevée, plus l'intervalle s'allonge. Un échec remet l'intervalle à 1 jour.

La prochaine date de révision est calculée à chaque fois qu'une note est attribuée :

```
prochaine_révision = aujourd'hui + intervalle
```

## Notes

| Note | Signification        |
|------|----------------------|
| 1    | Pas du tout su       |
| 2    | Difficile, hésitation|
| 3    | Su avec effort       |
| 4    | Su facilement        |
| 5    | Parfait              |

## Calcul de l'intervalle

```
note ≤ 2  →  intervalle = 1 jour          (échec, on repart de zéro)
note ≥ 3  →  selon l'intervalle actuel :
    - 1er succès  →  1 jour
    - 2e succès   →  3 jours
    - suivants    →  intervalle × facteur
```

### Facteurs de multiplication

| Note | Facteur |
|------|---------|
| 3    | × 1.5   |
| 4    | × 2.0   |
| 5    | × 2.5   |

## Exemples

### Avec note 4 constante

| Révision | Intervalle | Prochaine révision |
|----------|------------|--------------------|
| 1        | 1 jour     | J+1                |
| 2        | 3 jours    | J+3                |
| 3        | 6 jours    | J+6                |
| 4        | 12 jours   | J+12               |
| 5        | 24 jours   | J+24               |
| 6        | 48 jours   | J+48               |

### Avec notes variables

| Révision | Note | Intervalle actuel | Nouvel intervalle |
|----------|------|-------------------|-------------------|
| 1        | 4    | 0                 | 1 jour            |
| 2        | 3    | 1                 | 3 jours           |
| 3        | 5    | 3                 | 8 jours           |
| 4        | 2    | 8                 | 1 jour  ← échec   |
| 5        | 4    | 1                 | 3 jours           |
| 6        | 4    | 3                 | 6 jours           |

## Comportement dans l'application

- Les écrans de révision ne montrent que les items **dus aujourd'hui** (`prochaine_révision ≤ aujourd'hui`).
- Les compteurs sur l'écran de sélection affichent le nombre d'items dus.
- Les boutons sont désactivés s'il n'y a rien à réviser ce jour-là.
- Il n'y a pas de suppression définitive : même une note 5 replanifie la révision dans le futur.
