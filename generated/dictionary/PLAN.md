# Plan d'action — Dictionnaire bilingue de↔fr avec morphologie

## Source des données
- **kaikki.org** — dumps JSONL extraits du Wiktionnaire via [wiktextract](https://github.com/tatuylonen/wiktextract) (CC BY-SA)
- Données prêtes à télécharger, mises à jour hebdomadairement, sans avoir à faire tourner wiktextract soi-même
- Format : JSONL (une entrée par ligne), templates MediaWiki et macros Lua déjà expandés

### Fichiers utilisés

| Dump | URL | Taille | Usage |
|------|-----|--------|-------|
| Wiktionnaire allemand | `https://kaikki.org/dictionary/downloads/de/de-extract.jsonl.gz` | 280 MB | Mots allemands + formes fléchies |
| Wiktionnaire français | `https://kaikki.org/dictionary/downloads/fr/fr-extract.jsonl.gz` | 663 MB | Mots français + formes fléchies + traductions vers DE |

### Pourquoi deux dumps ?

Le dictionnaire doit fonctionner dans les **deux sens** (de→fr et fr→de), ce qui nécessite les deux dumps :

| | `de-extract` | `fr-extract` |
|---|---|---|
| Mots allemands + formes fléchies | ✅ | ❌ |
| Mots français + formes fléchies | ❌ | ✅ |
| Glosses en français | ❌ (glosses en DE) | ✅ |
| Glosses en allemand | ✅ | ❌ (glosses en FR) |
| Traductions de→fr | ✅ | ✅ |
| Exemples de phrases | ✅ (86% des entrées) | ✅ (66% des entrées) |

> **Note — contenu réel du dump `de-extract`**
>
> `de-extract` est une extraction du **Wiktionnaire allemand** (de.wiktionary.org). Il contient des
> définitions pour des mots de toutes les langues rédigées en allemand. Il faut filtrer sur
> `lang_code == "de"` pour n'extraire que les mots allemands.

### Mapping JSON → SQLite

| Source | Champ JSON | Table SQLite |
|--------|------------|--------------|
| `de-extract` | `word` (filtre `lang_code=de`) | `dict_entry.lemma` |
| `de-extract` | `pos` | `dict_entry.pos` |
| `de-extract` | `tags` (genre) | `dict_entry.gender` |
| `de-extract` | `forms[].form` + `forms[].tags` | `dict_form.form` + `dict_form.features` |
| `de-extract` | `forms[].pronouns` | `dict_form.pronouns` |
| `de-extract` | `senses[].examples[0].text` | `dict_entry.example` ou `dict_translation.example` |
| `fr-extract` | `word` (filtre `lang_code=fr`) | `dict_entry.lemma` |
| `fr-extract` | `translations[lang_code=de].word` | `dict_translation.text` |
| `fr-extract` | `senses[].glosses[0]` | `dict_translation.gloss_source` |
| `fr-extract` | `senses[].examples[0].text` | `dict_entry.example` ou `dict_translation.example` |

### Stratégie des exemples

Les exemples sont liés aux sens via `sense_index`. Deux cas :

| Cas | Stockage |
|-----|---------|
| `sense_index` fiable → exemple lié à la traduction | `dict_translation.example` (non null) |
| `sense_index` absent ou mot à sens unique | `dict_entry.example` (fallback partagé) |

**Logique app :** afficher `dict_translation.example` si non null, sinon `dict_entry.example`.

### Passé composé français

Le dump FR ne contient **pas** l'auxiliaire (avoir/être) pour le passé composé. La reconstruction
à l'affichage doit s'appuyer sur une liste statique des ~17 verbes conjugués avec être
(aller, venir, partir, arriver, naître, mourir, rester…) + tous les verbes réfléchis.

---

## Étape 1 — Exploration des données ✅

- [x] Télécharger et inspecter des échantillons DE et FR
- [x] Valider la structure : champs `forms`, `translations`, `tags`, `senses`, `examples`
- [x] Analyser la fiabilité du `sense_index` (70% absent dans le dump FR)

---

## Étape 2 — Conception du schéma SQLite ✅

```sql
dict_entry (
  id      INTEGER PRIMARY KEY,
  lemma   TEXT NOT NULL,
  locale  TEXT NOT NULL,        -- 'de' ou 'fr'
  pos     TEXT,                 -- 'noun', 'verb', 'adj'...
  gender  TEXT,                 -- 'masculine', 'feminine', 'neuter'
  example TEXT                  -- exemple fallback (non null = partagé entre toutes les traductions)
)

dict_features (
  id       INTEGER PRIMARY KEY,
  features TEXT NOT NULL UNIQUE  -- ex: "indicative,present" — déduplication
)

dict_translation (
  id            INTEGER PRIMARY KEY,
  entry_id      INTEGER REFERENCES dict_entry(id),
  target_locale TEXT,
  text          TEXT,
  gloss_source  TEXT,            -- définition dans la langue source
  example       TEXT             -- exemple précis via sense_index (null = utiliser dict_entry.example)
)

dict_form (
  id          INTEGER PRIMARY KEY,
  entry_id    INTEGER REFERENCES dict_entry(id),
  form        TEXT NOT NULL,     -- ex: "liebte"
  features_id INTEGER REFERENCES dict_features(id),  -- ex: "past,indicative"
  pronouns    TEXT               -- ex: "ich", "er,sie,es" — NULL pour les noms
)
```

**Taille actuelle : 91.8 MB** (100 150 entrées, 173 386 traductions, 1 079 963 formes)

---

## Étape 3 — Scripts Python d'extraction ✅

### Pipeline

```
de-extract.jsonl.gz  →  extract.py --locale de  →  extract_de.json
fr-extract.jsonl.gz  →  extract.py --locale fr  →  extract_fr.json
                                                         ↓
                         normalize.py (×2)        normalized_de.json
                                                   normalized_fr.json
                                                         ↓
                         generate_dictionary_db.py  →  dictionary.db
```

### `extract.py`
- Filtre sur `lang_code == locale` et `pos in ACCEPTED_POS`
- Extrait lemme, genre, POS, formes essentielles (selon `forms_config.json`), traductions, glosses, exemples
- `EXCLUDED_TAGS` : formes analytiques et constructions exclues (voir `OPTIMIZATIONS.md`)

### `forms_config.json`
- Tags essentiels par locale et par POS
- DE nouns : 8 combinaisons cas × nombre
- DE verbs : présent, prétérit, Partizip II, auxiliaire, Konjunktiv II, impératif
- DE adj : positif, comparatif, superlatif
- FR nouns : pluriel uniquement
- FR verbs : infinitif, participes, indicatif (présent/imparfait/passé simple/futur), conditionnel, subjonctif, impératif
- FR adj : masculin/féminin × singulier/pluriel

### `normalize.py`
- DE : pronoms depuis le champ `pronouns` de l'entrée
- FR : pronoms extraits de la forme (`"je lis"` → `form="lis", pronouns="je"`)
- FR subjonctif : strip du préfixe `que/qu'` (`"que je lise"` → `form="lise", pronouns="je"`)
- Features : liste de tags jointe en string (`"indicative,present"`)
- Gloss : priorité au gloss_source résolu (de-extract), fallback sur champ `sense` (fr-extract)

### `generate_dictionary_db.py`
- Fusionne normalized_de.json + normalized_fr.json
- Features déduplication via `dict_features`
- Exemples : précis dans `dict_translation.example` si sense_index fiable, sinon fallback dans `dict_entry.example`
- Filtres dans `insert_forms` : formes = lemme exclues, formes multi-mots avec pronom sujet intégré exclues

---

## Étape 3b — Filtrage par fréquence (à faire plus tard)

Réduire le dictionnaire aux ~10 000 mots les plus courants par langue pour alléger la DB.

- [ ] Télécharger les listes Hermit Dave (`de_50k.txt`, `fr_50k.txt`) depuis https://github.com/hermitdave/FrequencyWords
- [ ] Dans `extract.py`, ajouter `--freq-list <path>` pour filtrer les lemmes absents de la liste
- [ ] Valider la couverture (vérifier que les mots courants ne sont pas perdus)

---

## Étape 4 — Intégration Android ✅

- [x] Ajouter `dictionary.db` dans `app/src/main/assets/` (91.8 MB)
- [x] Écrire les requêtes SQLDelight :
  - Lookup par mot exact (lemme) — `DictEntry.sq`
  - Recherche de forme fléchie → lemme + features — `DictForm.sq`
  - Récupération des traductions avec exemple (précis ou fallback) — `DictTranslation.sq`
- [x] `DictionaryDriverFactory` en `expect/actual` (copie depuis assets à la première installation, versionnée via `DICTIONARY_DB_VERSION`)
- [x] `DictionaryRepository` exposé dans `shared/commonMain`
- [x] `MainActivity` instancie `DictionaryRepository` (prêt à passer aux screens)

### Détails techniques

- Les `.sq` de `DictionaryDatabase` sont dans `src/commonMain/sqldelight-dictionary/` (répertoire séparé de `VocabularyDatabase` pour éviter les redéclarations de types générés)
- `build.gradle.kts` : `srcDirs("src/commonMain/sqldelight-dictionary")` dans le bloc `DictionaryDatabase`
- `DictionaryRepository.resolveExample(translation, entry)` : `translation.example ?: entry.example`

---

## Étape 5 — UI ✅

- [x] Écran dédié `DictionaryScreen` : recherche bilingue de↔fr
  - `TextField` avec debounce 300ms
  - Recherche simultanée dans les deux locales, résultats triés par lemme, limités à 10
  - Chaque résultat : flag + lemme + pos/genre + traductions séparées par des virgules
  - Tap sur un résultat → `DictionaryDetailScreen`
- [x] `DictionaryDetailScreen` : détail complet d'une entrée
  - En-tête : flag + lemme (headline) + pos + genre
  - Section **Traductions** : texte (gras) + gloss source (italique) + exemple résolu (`dict_translation.example ?? dict_entry.example`)
  - Section **Formes** : groupées par temps/cas dans l'ordre grammatical logique
    - Verbes : Infinitif → Présent → Prétérit → Imparfait → Futur → Passé simple → Konjunktiv II → Subjonctif → Conditionnel → Impératif → Participes
    - Noms : Nominatif → Accusatif → Datif → Génitif
    - Adjectifs : Positif → Comparatif → Superlatif
  - Rows : `pronom | forme` pour les verbes, `Sg./Pl.` pour les noms, `Masc./Fém.` pour les adjectifs
- [x] `DictionaryRepository.getById` ajouté + requête `getById` dans `DictEntry.sq`
- [ ] Passé composé FR : reconstruction avec liste statique être/avoir (non prioritaire)
- [ ] Filtrage par fréquence (Hermit Dave wordlists) pour alléger la DB

---

## Licence

Données sous **CC BY-SA** (Wiktionnaire via kaikki.org / wiktextract).
Obligations : mentionner la source, rendre les données disponibles sous CC BY-SA.
Le code applicatif reste privé.
