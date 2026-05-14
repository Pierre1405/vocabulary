# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew :app:build               # Android app
./gradlew :shared:build            # Shared KMP library

# Tests
./gradlew jvmTest                  # Unit tests (shared module, fast)
./gradlew connectedDebugAndroidTest  # Instrumentation tests (requires device)

# Lint
./gradlew lintDebug
./gradlew lintFix                  # Apply safe fixes

# Database
./gradlew generateSqlDelightInterface   # Regenerate SQLDelight code
./gradlew verifySqlDelightMigration     # Verify migrations

# Resource generation (Python pipeline)
# See generated/README.md for adding/updating content
```

## Architecture

**Kotlin Multiplatform** app (Android + iOS) for bilingual vocabulary learning with spaced repetition. All UI is **Compose Multiplatform** with **Material 3**.

### Layer structure

```
UI Screens + ViewModels  (shared/src/commonMain/.../ui/)
        ↓  StateFlow / suspend
Repositories             (shared/src/commonMain/.../data/)
        ↓  SQLDelight generated interfaces
3 SQLite databases       (assets + runtime)
```

Platform-specific implementations live under `androidMain/` and `iosMain/` via Kotlin `expect/actual` (audio playback, speech recognition, DB drivers).

### Three databases

| Database | Source | Contents |
|---|---|---|
| `vocabulary.db` | Shipped in assets, versioned via `DB_VERSION.kt` | Stories, sentences (stable `sentence_key` PK), translations, categories, app config |
| `dictionary.db` | Shipped in assets (~92 MB) | 100K+ lemmas, 173K+ translations, ~1M inflected forms (de↔fr) |
| `learning.db` | Runtime, local only | Sentence review progress (SM-2), word mastery, virtual clock config |

When bumping `DB_VERSION`, the app automatically recopies the DB from assets on next launch.

### Navigation

```
HomeScreen
├── StoryListScreen → SentenceScreen → DictionaryScreen (tap a word)
├── ReviewSelectionScreen
│   ├── ReviewScreen (sentences, 3 display modes)
│   ├── WordReviewScreen
│   └── ConjugationReviewScreen
├── DictionaryScreen → DictionaryDetailScreen (conjugation tables)
└── ConjugationScreen
```

Entry point: `app/src/main/java/com/example/myapplication/MainActivity.kt` — initializes repositories, launches Compose. Navigation routes: `shared/src/commonMain/kotlin/.../ui/AppNavigation.kt`.

### Spaced repetition

SM-2 algorithm in `LearningRepository`. A **virtual clock** anti-overwhelm mechanism caps catch-up to 24 h/day — see `docs/spaced-repetition.md`.

### Content generation pipeline

Python scripts under `generated/` produce the SQLite assets from raw sources. See `generated/README.md` and `docs/add-new-language.md` before touching vocabulary data.

### Key docs

- `etat_actuel_code.md` — comprehensive architecture overview (French)
- `docs/plan-forms-config.md` — verb conjugation form configuration
- `docs/add-new-language.md` — adding a new language pair
