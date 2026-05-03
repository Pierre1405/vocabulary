# Ajouter une nouvelle langue

Ce guide décrit les étapes pour ajouter une langue au-delà de `de` et `fr`.
Les exemples utilisent `es` (espagnol) comme illustration.

---

## Vue d'ensemble de l'architecture

```
Source Wiktionary (kaikki.org dump)
    ↓  extract.py
extract_<locale>.json
    ↓  normalize.py
normalized_<locale>.json
    ↓  generate_dictionary_db.py
dictionary.db   ←  lu par l'app Android (SQLDelight)
    ↓
FormsConfig<Locale>.kt  →  FormGroupBuilder  →  UI
```

---

## Étape 1 — Source de données

Télécharger le dump Wiktionary depuis [kaikki.org](https://kaikki.org/dictionary/) pour la langue cible (ex: `es-extract.jsonl.gz`) et le placer dans `generated/dictionary/source/`.

La langue cible de traduction est la langue native de l'utilisateur (actuellement `fr`).

---

## Étape 2 — Pipeline de génération Python

### 2a. `forms_config.json`

Ajouter une section pour la nouvelle locale dans `generated/dictionary/forms_config.json` :

```json
"es": {
  "skip_tag_combinations": [
    ["archaic"], ["obsolete"]
  ],
  "skip_tags_by_pos": {
    "verb": ["reflexive", "transitive", "intransitive"]
  },
  "pronoun_canonical": {
    "él":    "él/ella/usted",
    "ella":  "él/ella/usted",
    "usted": "él/ella/usted",
    "ellos":  "ellos/ellas/ustedes",
    "ellas":  "ellos/ellas/ustedes",
    "ustedes":"ellos/ellas/ustedes"
  },
  "groups": {
    "verb": [
      { "key": "indicative_present",   "tags": ["indicative", "present"] },
      { "key": "indicative_imperfect", "tags": ["indicative", "imperfect"] },
      { "key": "indicative_past",      "tags": ["indicative", "past"] },
      { "key": "indicative_future",    "tags": ["indicative", "future"] },
      { "key": "subjunctive_present",  "tags": ["subjunctive", "present"] },
      { "key": "imperative",           "tags": ["imperative"] },
      { "key": "participle_present",   "tags": ["participle", "present"] },
      { "key": "participle_past",      "tags": ["participle", "past"] }
    ],
    "noun": [
      { "key": "plural", "tags": ["plural"] }
    ],
    "adj": [
      { "key": "masculine_singular", "tags": ["masculine", "singular"] },
      { "key": "masculine_plural",   "tags": ["masculine", "plural"] },
      { "key": "feminine_singular",  "tags": ["feminine", "singular"] },
      { "key": "feminine_plural",    "tags": ["feminine", "plural"] }
    ]
  }
}
```

Points à vérifier :
- Les **tags** correspondent aux valeurs présentes dans le dump Wiktionary pour cette langue (les inspecter via `exploration/sample_<locale>.json`).
- L'ordre des groupes est **du plus spécifique au plus général** (first-match wins).
- Si la langue utilise un auxiliaire pour les temps composés, ajouter `"auxiliary_detection"` comme pour le français.

### 2b. `run_all.py`

Ajouter l'extraction et la normalisation de la nouvelle locale :

```python
# Dans la section d'extraction parallèle
extract("es", target="fr")   # ou target="en" selon la langue native visée

# Dans la section de normalisation
normalize("es")

# Dans l'appel à generate_dictionary_db.py
--inputs normalized_de.json normalized_fr.json normalized_es.json
```

### 2c. Régénérer la base

```bash
python run_all.py
# ou manuellement :
python extract.py   --dump source/es-extract.jsonl.gz --locale es --target fr --output extract_es.json
python normalize.py --input extract_es.json --locale es --output normalized_es.json
python generate_dictionary_db.py --inputs normalized_de.json normalized_fr.json normalized_es.json --output ../../app/src/main/assets/dictionary.db
```

---

## Étape 3 — Configuration Kotlin des formes

### 3a. Créer `FormsConfig<Locale>.kt`

Créer `shared/src/commonMain/kotlin/.../data/forms/FormsConfigEs.kt` sur le modèle de `FormsConfigFr.kt` ou `FormsConfigDe.kt`.

Structure minimale :

```kotlin
val FormsConfigEs = FormsConfig(
    groups = listOf(
        GroupConfig("indicative_present",   "Presente"),
        GroupConfig("indicative_imperfect", "Pretérito imperfecto"),
        GroupConfig("indicative_past",      "Pretérito indefinido"),
        GroupConfig("indicative_future",    "Futuro"),
        // Temps composés → derive lambda si non stockés dans la DB
        GroupConfig("subjunctive_present",  "Subjuntivo presente"),
        GroupConfig("imperative",           "Imperativo"),
        GroupConfig("participle_present",   "Gerundio"),
        GroupConfig("participle_past",      "Participio"),
    ),
    pronounOrder = listOf("yo", "tú", "él/ella/usted", "nosotros", "vosotros", "ellos/ellas/ustedes")
)
```

Pour les temps composés absents de la DB, ajouter des **lambdas `derive`** (cf. `FormsConfigFr.kt` pour le modèle avec `compoundWith`).

### 3b. `FormsConfigRegistry.kt`

Ajouter le cas dans `getFormsConfig()` :

```kotlin
// shared/src/commonMain/kotlin/.../data/forms/FormsConfigRegistry.kt
fun getFormsConfig(locale: String): FormsConfig = when (locale) {
    "fr" -> FormsConfigFr
    "es" -> FormsConfigEs   // ← ajouter
    else -> FormsConfigDe
}
```

---

## Étape 4 — UI et recherche

### `DictionaryViewModel.kt`

Ajouter `"es"` dans les listes de locales interrogées lors de la recherche :

```kotlin
// Recherche par lemme
listOf("de", "fr", "es").map { locale -> ... }

// Recherche par forme
listOf("de", "fr", "es").map { locale -> ... }
```

### `LocaleFlag.kt`

Le fichier mappe déjà `"es"` → `"🇪🇸"`, aucune modification nécessaire.

### `StoryViewModel.kt` / `SentenceViewModel.kt`

Les valeurs par défaut `native_language = "fr"` et `learned_language = "de"` sont stockées dans `Configuration.sq`. Aucune modification nécessaire si l'utilisateur peut choisir sa paire de langues en UI. Sinon, mettre à jour les defaults.

---

## Checklist récapitulative

| Étape | Fichier | Action |
|-------|---------|--------|
| 1 | `generated/dictionary/source/` | Télécharger le dump kaikki.org |
| 2a | `forms_config.json` | Ajouter la section `"es"` |
| 2b | `run_all.py` | Ajouter extraction + normalisation |
| 2c | — | Régénérer `dictionary.db` |
| 3a | `FormsConfigEs.kt` | Créer le fichier avec groupes et pronounOrder |
| 3b | `FormsConfigRegistry.kt` | Ajouter `"es" -> FormsConfigEs` |
| 4a | `DictionaryViewModel.kt` | Ajouter `"es"` dans les listes de recherche |
| 4b | `LocaleFlag.kt` | Vérifier (déjà mappé pour les langues courantes) |

---

## Notes

- **Tags Wiktionary** : les tags varient d'une langue à l'autre. Inspecter `exploration/sample_<locale>.json` pour identifier les tags réels avant de remplir `forms_config.json`.
- **Langues agglutinantes** (turc, finnois…) : le nombre de formes peut être très élevé — prévoir un filtrage plus agressif dans `skip_tags_by_pos`.
- **Langues sans genre grammatical** (anglais…) : les groupes `adj` peuvent être simplifiés à un seul `GroupConfig`.
- **Temps composés** : si Wiktionary stocke les formes composées avec un auxiliaire, utiliser `auxiliary_detection` comme pour le français. Sinon, générer via lambda `derive`.
