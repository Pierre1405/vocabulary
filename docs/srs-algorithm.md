# Algorithme SRS — Planification des révisions

## Principe général

L'application utilise un algorithme de répétition espacée (SRS — Spaced Repetition System). Chaque item révisé reçoit une note de 1 à 5. Cette note détermine le prochain délai de révision selon la table suivante :

| Note | Signification       | Prochain intervalle                     |
|------|---------------------|-----------------------------------------|
| 1    | Pas du tout         | Immédiatement (0h)                      |
| 2    | Très difficile      | 1h                                      |
| 3    | Difficile           | intervalle × 1,5 (première fois : 24h) |
| 4    | Bien                | intervalle × 2,0 (première fois : 24h) |
| 5    | Parfait             | intervalle × 2,5 (première fois : 24h) |

Les intervalles sont stockés en heures dans la colonne `interval_hours` de la table `learning`.

---

## Horloge virtuelle — mécanisme anti-submersion

### Problème

Avec une horloge réelle, une absence de plusieurs jours entraîne l'accumulation de toutes les révisions en attente. L'utilisateur se retrouve submergé d'items à revoir dès sa reconnexion, ce qui nuit à l'expérience d'apprentissage.

### Solution

Les révisions sont planifiées par rapport à une **horloge virtuelle** (`usage_time`) plutôt que par rapport à l'heure réelle.

Cette horloge virtuelle avance d'**au maximum `MAX_DAILY_CATCH_UP_HOURS` heures** (actuellement 24h) par session, quelle que soit la durée de l'absence.

### Fonctionnement

Deux valeurs sont persistées dans la table `learning_config` :

| Clé                    | Description                                          |
|------------------------|------------------------------------------------------|
| `last_connection_time` | Heure réelle (epoch hours) de la dernière session    |
| `usage_time`           | Horloge virtuelle accumulée (epoch hours virtuels)   |

À chaque ouverture de l'écran de révision, `LearningRepository.updateUsageTime()` est appelé :

```
elapsed   = min(now - last_connection_time, MAX_DAILY_CATCH_UP_HOURS)
usage_time += elapsed
last_connection_time = now
```

Toutes les dates `next_review` sont stockées et comparées en **heures virtuelles**.

### Exemple

| Situation                  | Heures réelles écoulées | Avance de usage_time |
|----------------------------|------------------------|----------------------|
| Usage quotidien (24h)      | 24h                    | +24h                 |
| Absence de 3 jours (72h)   | 72h                    | +24h (plafonné)      |
| Absence de 2 semaines      | 336h                   | +24h (plafonné)      |
| Deux sessions en 1h        | 1h                     | +1h                  |

Après 3 jours d'absence, l'utilisateur ne voit que les items qui étaient dus dans les 24 prochaines heures virtuelles, pas les 72 heures réelles.

### Initialisation

Au premier lancement, `usage_time` est initialisé à `currentEpochHours()` (heure réelle). Ceci garantit la compatibilité avec les items déjà enregistrés dont `next_review` est exprimé en epoch hours réels.

### Localisation dans le code

- **`LearningConfig.sq`** — table `learning_config` et requêtes `getConfig` / `setConfig`
- **`LearningRepository.kt`** — `updateUsageTime()`, `currentUsageHours()`, constante `MAX_DAILY_CATCH_UP_HOURS`
- **`StoryViewModel.kt`** — appel de `updateUsageTime()` au début de `refreshCounts()`, déclenché par `LifecycleResumeEffect` sur l'écran de sélection des révisions
