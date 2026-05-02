# Plan — Configuration des formes par locale

## Objectif

Remplacer la logique hardcodée dans `DictionaryDetailViewModel` par une configuration
par locale, sous forme d'objets Kotlin. Cela permet :
- un affichage adapté à chaque langue (groupes, labels, ordre)
- d'ajouter une nouvelle locale sans toucher au ViewModel
- une séparation claire entre logique d'affichage et données linguistiques

## Structure

### Data classes de configuration

```
shared/src/commonMain/kotlin/.../data/forms/
    FormsConfig.kt          ← data classes (FormsConfig, GroupConfig, PronounLabelConfig)
    FormsConfigDe.kt        ← config allemand
    FormsConfigFr.kt        ← config français
    FormsConfigRegistry.kt  ← fun getFormsConfig(locale: String): FormsConfig
```

Un groupe matche si **tous** ses `matchTags` sont présents dans les tags de la forme.
Le premier groupe qui matche est sélectionné (l'ordre dans la liste définit la priorité
et l'ordre d'affichage).

### Config allemande — groupes dans l'ordre d'affichage

1. Infinitif
2. Présent indicatif
3. Prétérit (Präteritum)
4. Konjunktiv II
5. Impératif
6. Participe présent
7. Participe passé
8. Auxiliaire
9. Nominatif / Accusatif / Datif / Génitif (noms)
10. Comparatif / Superlatif (adjectifs)

Pronoms : ich / du / er·sie·es / wir / ihr / sie

### Config française — groupes dans l'ordre d'affichage

1. Infinitif
2. Présent indicatif
3. Imparfait
4. Passé simple
5. Futur
6. Conditionnel présent
7. Subjonctif présent
8. Impératif
9. Participe présent
10. Participe passé
11. Masc. Sg. / Masc. Pl. / Fém. Sg. / Fém. Pl. (adjectifs)

Pronoms : je / tu / il·elle·on / nous / vous / ils·elles

## Gestion des doublons

### Tags redondants dans les données Wiktionary (allemand uniquement)

Les données françaises sont propres — pronoms déjà séparés dans le champ `pronouns`,
aucun tag redondant. Le problème est **spécifique à l'allemand**.

Wiktionary répète sur chaque forme des propriétés qui appartiennent au **verbe entier**,
pas à la forme conjuguée. Ces tags sont filtrés à la génération de la DB dans
`generate_dictionary_db.py` via la clé `redundant_tags_by_pos` de `forms_config.json`.

| Tag | Explication | Pourquoi redondant |
|---|---|---|
| `active` | Voix active (*Ich mache*) vs passif (*Es wird gemacht*) | Les verbes sont quasi exclusivement à la voix active |
| `main-clause` | Verbe en 2ème position (*Ich **gehe** heute ins Kino*) | La forme est identique en principale et en subordonnée |
| `subordinate-clause` | Verbe à la fin (*dass ich heute ins Kino **gehe***) | Idem — seule la position dans la phrase change |
| `regular` / `irregular` | Pattern de conjugaison du verbe | Propriété du verbe, pas de la forme |
| `auxiliary verb` | Le verbe utilise un auxiliaire au parfait | Propriété du verbe, pas de la forme |
| `reflexive` / `statal-reflexive` | Verbe réfléchi (sich waschen) | Propriété du verbe, pas de la forme |
| `transitive` / `intransitive` | Prend ou non un complément d'objet | Propriété du verbe, pas de la forme |
| `separable` / `inseparable` | Préfixe séparable (aufmachen) ou non (verstehen) | Propriété du verbe, pas de la forme |
| `archaic` / `outdated` | Forme vieillie | Non pertinent pour l'apprentissage courant |
| `verb` | Littéralement "verbe" | Complètement redondant |

> **Note :** `auxiliary` seul n'est pas dans la liste car il entre dans la combinaison
> `["auxiliary", "perfect"]` qui identifie les vraies formes auxiliaires (haben/sein).

> **Note :** `accusative` et `dative` ne sont pas filtrés sur les verbes car ils sont
> essentiels pour les déclinaisons de noms. Le filtre s'applique par POS (`verb` uniquement).

### Déduplication dans le ViewModel

Après suppression des tags redondants en DB, il peut rester des doublons si deux feature
strings différentes produisent le même triplet `(groupKey, pronounLabel, form)` après
résolution via la config. Un `distinctBy { Triple(groupIndex, pronounLabel, form) }`
dans `buildFormGroups` les élimine.

## Fichiers modifiés / créés

| Fichier | Action | Statut |
|---|---|---|
| `generated/dictionary/forms_config.json` | Ajout `redundant_tags_by_pos` | ✅ |
| `generated/dictionary/generate_dictionary_db.py` | Lecture config + nettoyage des tags | ✅ |
| `data/forms/FormsConfig.kt` | Créer | ✅ |
| `data/forms/FormsConfigDe.kt` | Créer | ✅ |
| `data/forms/FormsConfigFr.kt` | Créer | ✅ |
| `data/forms/FormsConfigRegistry.kt` | Créer | ✅ |
| `ui/dictionary/DictionaryDetailViewModel.kt` | Remplacer companion object par config | ✅ |

## Ce qui ne change pas

- `DictionaryDetailScreen.kt` — aucun changement
- Le schéma de la DB — aucun changement
- Les data classes `FormRow` / `FormGroup` — aucun changement
