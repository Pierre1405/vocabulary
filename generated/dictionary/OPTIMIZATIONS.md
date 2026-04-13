# Optimisations — dictionary.db

Historique des réductions de taille du fichier SQLite final.

## Résultat global

| Étape | Taille | Formes (dict_form) | Réduction cumulée |
|-------|--------|--------------------|-------------------|
| Baseline (toutes les formes) | ~197 MB | ~1 800 000 | — |
| Après optim 1 + 2 + 3 | 178 MB | 1 713 706 | −10% |
| Après optim 4 + 5 | 72 MB | 844 794 | −63% |
| Après optim 6 | 58 MB | 667 765 | −71% |
| Optim 5 retirée (tableaux complets) | 100 MB | 1 479 262 | −49% |
| Après optim 7 + 8 | 76.5 MB | 1 101 785 | −61% |
| Après optim 9 + 10 | 74.7 MB | 1 079 963 | −62% |
| + Ajout exemples (feature) | **91.8 MB** | **1 079 963** | **−53%** |

---

## Optim 1 — Déduplication des features via table de lookup

**Problème :** La colonne `features` (ex: `"indicative,present,singular"`) était stockée en texte brut dans chaque ligne de `dict_form`. Avec 1,7 M de formes, la même chaîne était répétée des milliers de fois.

**Solution :** Table `dict_features (id, features TEXT UNIQUE)`. `dict_form.features_id` stocke un entier à la place de la chaîne. Seules **4 806 chaînes uniques** existent pour 844 794 formes.

**Impact :** Réduction significative de la taille des lignes `dict_form`.

---

## Optim 2 — Filtrage des POS hors scope

**Problème :** Les POS `name`, `phrase`, `abbrev`, `suffix`, `prefix`… généraient des entrées sans formes fléchies utiles, mais avec des traductions qui gonflaient `dict_translation`.

**Solution :** `ACCEPTED_POS` dans `extract.py` — seules ces catégories sont conservées :
```python
ACCEPTED_POS = {"noun", "verb", "adj", "adv", "pron", "prep", "conj", "intj", "det", "num", "particle"}
```

**Impact :** Réduction du nombre d'entrées et de traductions.

---

## Optim 3 — Exclusion des formes identiques au lemme

**Problème :** Stocker `"groß"` comme forme de l'entrée dont le lemme est `"groß"` est inutile pour le lookup (on retrouverait la même entrée en cherchant directement le lemme).

**Solution :** Dans `generate_dictionary_db.py`, skip si `form.lower() == lemma.lower()`.

**Impact :** Suppression de plusieurs centaines de milliers de lignes redondantes.

---

## Optim 4 — Exclusion des tags spéciaux

**Problème :** Certaines formes fléchies correspondent à des constructions inutiles :

| Tag | Exemple | Raison d'exclure |
|-----|---------|-----------------|
| `processual-passive` | formes passives analytiques DE | constructions avec auxiliaire, pas des formes simples |
| `multiword-construction` | formes analytiques FR | idem |
| `anterior` | passé antérieur FR (`j'eus aimé`) | temps littéraire, très rare à l'oral et à l'écrit courant |
| `predicative` | `"er ist pittoresk"`, `"am pittoreskesten"` | phrases entières, pas des formes fléchies simples |

**Solution :** `EXCLUDED_TAGS` dans `extract.py` — une forme est rejetée si ses tags contiennent l'un de ces tags :
```python
EXCLUDED_TAGS = {"processual-passive", "multiword-construction", "anterior", "predicative"}
```

**Impact :** ~57 000 formes prédicatives supprimées pour les adjectifs allemands seuls.

---

## ~~Optim 5 — Déduplication par chaîne de forme~~ ❌ Retirée

**Idée initiale :** Réduire la clé de déduplication à `form` seul pour éviter de stocker plusieurs fois la même chaîne avec des features différentes. Gain mesuré : **8,2×** sur les adjectifs, −51% de formes au total.

**Pourquoi retirée :** Pour afficher un tableau de conjugaison/déclinaison complet (ex: "liest = 3ème pers. sing. présent"), il faut conserver toutes les combinaisons `(form, features, pronouns)`. En ne gardant qu'une ligne par chaîne, on perd les informations de personne/nombre pour les formes syncrétriques (ex: `"sind"` = 1ère et 3ème pers. pluriel).

**Coût du retrait :** +42 MB (58 MB → 100 MB) — prix à payer pour les tableaux complets.

---

## Optim 6 — Exclusion des formes verbales analytiques composées

**Problème :** En allemand, la même chaîne de forme apparaît pour plusieurs combinaisons de cas/genre/nombre. Par exemple, `"pittoresken"` est à la fois génitif masculin singulier, datif masculin singulier, accusatif masculin singulier, et pluriel pour toutes les déclinaisons. L'ancienne clé de déduplication `(form, features, pronouns)` conservait chaque ligne distincte.

**Mesure :** Pour un adjectif typique comme `pittoresk` :
- Formes totales dans le dump : **159**
- Chaînes distinctes utiles pour le lookup : **29**
- Ratio de réduction : **8,2×** sur les adjectifs

**Solution :** Clé de déduplication réduite à `form` seul dans `generate_dictionary_db.py`. On conserve les features de la première occurrence rencontrée.

```python
# Avant
key = (form, features, pronouns)

# Après
key = form
```

**Impact :** 1 713 706 → 844 794 formes (−51%). C'est l'optimisation la plus impactante.

**Distribution des formes après optim 5 :**

| Locale | POS | Formes |
|--------|-----|--------|
| de | verb | 517 582 |
| de | noun | 87 303 |
| de | adj | 69 263 |
| fr | verb | 132 992 |
| fr | autres | 37 654 |

---

## Optim 6 — Exclusion des formes verbales analytiques composées

**Problème :** Le tag `["subjunctive-ii"]` dans `forms_config.json` capturait trop large. En plus du Konjunktiv II simple (utile), il matchait des temps composés analytiques (constructions avec auxiliaire) rarement rencontrés en lecture courante :

| Tag | Temps | Exemple | Fréquence |
|-----|-------|---------|-----------|
| `pluperfect` | Konjunktiv II Plusquamperfekt | "er hätte gegessen" | rare, littéraire |
| `future-i` | Konjunktiv II Futur I | "er würde essen" | analytique (2 mots) |
| `future-ii` | Konjunktiv II Futur II | "er würde gegessen haben" | très rare |

Ces formes représentaient chacune ~5 500 lignes × plusieurs personnes/nombres.

**Solution :** Ajout à `EXCLUDED_TAGS` dans `extract.py` :
```python
EXCLUDED_TAGS = {"processual-passive", "multiword-construction", "anterior", "predicative",
                 "pluperfect", "future-i", "future-ii"}
```

**Impact :** 844 794 → 667 765 formes (−21%). Taille : 72 MB → **58 MB**.

---

## Optim 7 — Exclusion du Zustandspassiv (statal-passive)

**Problème :** On avait exclu `processual-passive` (Vorgangspassiv : "wird gebaut") mais pas `statal-passive` (Zustandspassiv : "ist gebaut"). Ces formes passives d'état sont des constructions analytiques (ist/war + Partizip II) inutiles pour une appli de vocabulaire.

Exemples : `"er/sie/es ist abaissiert"`, `"er/sie/es war abaissiert"`, `"er/sie/es sei abaissiert"`

**Solution :** Ajout de `statal-passive` à `EXCLUDED_TAGS` dans `extract.py` :
```python
EXCLUDED_TAGS = {"processual-passive", "multiword-construction", "anterior", "predicative",
                 "pluperfect", "future-i", "future-ii", "statal-passive"}
```

---

## Optim 8 — Exclusion des formes verbales à pronom sujet intégré

**Problème :** Le dump kaikki.org stocke les formes verbales en double :
- **Représentation compacte** (souhaitée) : `form="aale"`, `features="present"`, `pronouns="ich"`
- **Représentation étendue** (redondante) : `form="ich aale"`, `features="first-person,singular,present,active,indicative"`, `pronouns=None`

La seconde représentation est capturée par nos règles (`["present"]` matche aussi les formes avec tags détaillés). Elle représentait **404 677 formes multi-mots** pour les seuls verbes allemands.

**Règle de filtrage :** Exclure les formes dont la chaîne contient un espace ET dont les features contiennent `first-person`, `second-person` ou `third-person`. Cela préserve :
- Les infinitifs réfléchis (`"sich aalen"` — pas de tag personne)
- Les impératifs honorifiques (`"aalen Sie!"` — tag `honorific`, pas de personne)
- Les participes (`"sich aalend"`)
- Toutes les formes simples (un seul mot)

**Solution :** Dans `generate_dictionary_db.py`, dans `insert_forms` :
```python
if " " in form and any(p in features for p in ("first-person", "second-person", "third-person")):
    continue
```

**Impact :** 1 479 262 → 1 101 785 formes (−25%). Taille : 100 MB → **76.5 MB**.  
Bonus : features uniques 4 864 → **484** (−90%) — les combinaisons détaillées type `first-person,singular,present,active,indicative` disparaissent.

---

## Optim 9 — Exclusion du futur antérieur français

**Problème :** Le futur antérieur (`"j'aurai lu"`, `"tu auras lu"`...) est un temps composé analytique avec auxiliaire. Il était capturé par la règle `["future", "indicative"]` du `forms_config.json` car ses tags contiennent `future` et `indicative`. Ces 19 343 formes sont toutes multi-mots mais n'avaient pas de tag personne (`first-person`...), donc non filtrées par l'optim 8.

**Solution :** Extension du filtre multi-mots dans `generate_dictionary_db.py` pour inclure `perfect` :
```python
if " " in form and any(p in features for p in ("first-person", "second-person", "third-person", "perfect")):
    continue
```
Le `" " in form` protège les Partizip II allemands (`"gegessen"` → pas d'espace, features contient `perfect`).

---

## Optim 10 — Nettoyage des formes du subjonctif français

**Problème :** Le dump kaikki.org stocke les formes du subjonctif avec le `que` introductif intégré dans la chaîne :
```
"que je lise"       features=subjunctive,present   pronouns=None
"qu'il/elle/on lise"  features=subjunctive,present   pronouns=None
```
Le `que` est une conjonction de subordination, pas une partie de la forme verbale. Ces formes ne sont pas utilisables directement pour un tableau de conjugaison.

**Solution :** Dans `normalize.py`, strip du préfixe `que/qu'` avant l'extraction du pronom :
```python
FR_QUE_RE = re.compile(r"^qu['\u2019e]\s*", re.IGNORECASE)

def extract_fr_pronoun(form):
    que_match = FR_QUE_RE.match(form)
    if que_match:
        form = form[que_match.end():]
    # ... extraction normale du pronom
```

**Résultat :**
```
"que je lise"  →  form="lise"   pronouns="je"
"qu'il/elle/on lise"  →  form="lise"   pronouns="il/elle/on"
```

**Impact :** 1 101 785 → 1 079 963 formes (−2%). Taille : 76.5 MB → **74.7 MB**.  
Qualité des données significativement améliorée pour le subjonctif.

---

## Feature — Exemples de phrases

**Ajout :** Stockage d'un exemple de phrase par traduction pour contextualiser le sens.

**Couverture dans les dumps :**
- DE : 86% des entrées ont des exemples (~108 chars en moyenne)
- FR : 66% des entrées ont des exemples (~147 chars en moyenne)

**Stratégie de stockage (évite la duplication) :**

| Cas | Stockage |
|-----|---------|
| `sense_index` fiable → exemple lié à la traduction précise | `dict_translation.example` (non null) |
| `sense_index` absent → exemple partagé entre toutes les traductions | `dict_entry.example` (non null) |

**Logique app :** `dict_translation.example ?? dict_entry.example`

**Résultat :**
- 90 213 exemples précis dans `dict_translation`
- 39 165 exemples fallback dans `dict_entry`
- ~70 772 entrées sans exemple

**Impact taille :** 74.7 MB → **91.8 MB** (+17 MB pour les exemples)
