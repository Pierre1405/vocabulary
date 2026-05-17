# Mode mains-libres — Conversation IA

## Objectif

Permettre une conversation vocale continue sans interaction tactile :
l'IA parle, le micro s'ouvre automatiquement, l'utilisateur répond,
le message est envoyé dès le silence détecté.

## Architecture cible

### State machine dans `ConversationViewModel`

```
IDLE
  │  (utilisateur active le mode)
  ▼
AI_SPEAKING  ──────────────────────────────────────────────┐
  │  (TtsPlayer.onSpeakFinished)                           │
  ▼                                                        │
LISTENING                                                  │
  │  (SpeechRecognizer.onResults → texte non vide)         │
  ▼                                                        │
SENDING                                                    │
  │  (réponse IA reçue → isLoading = false)                │
  └──────────────────────────────────────────────────────►─┘
```

Transitions d'erreur :
- `LISTENING` → silence sans résultat → retour `AI_SPEAKING` (relance TTS "Je n'ai pas entendu…")
- `LISTENING` → erreur micro → retour `IDLE`
- `SENDING` → erreur API → retour `IDLE`

---

## Modifications requises

### 1. `TtsPlayer` — callback fin de parole

```kotlin
// expect
expect class TtsPlayer {
    fun speak(text: String, locale: String, onDone: (() -> Unit)? = null)
    fun stop()
    fun shutdown()
}
```

Implémentation Android (`androidMain`) : passer `onDone` au
`TextToSpeech.OnUtteranceProgressListener.onDone`.

### 2. `ConversationViewModel` — state machine

Ajouter un état `handsFreeMode : HandsFreeState` au `ConversationUiState` :

```kotlin
enum class HandsFreeState { OFF, AI_SPEAKING, LISTENING, SENDING }
```

Logique :
- Activation → si dernier message IA existe, lancer TTS + passer en `AI_SPEAKING`
- `onSpeakFinished` → passer en `LISTENING`, appeler `speechRecognizer.startListening()`
- `onResult(text)` → passer en `SENDING`, appeler `sendMessage(text)`
- Fin de `sendMessage` → passer en `AI_SPEAKING`, lancer TTS sur la réponse

### 3. `ConversationScreen` — indicateur visuel + bouton toggle

Ajouter dans la TopAppBar :
- Icône micro (actif = animé/coloré) qui toggle le mode
- Indicateur d'état textuel sous les messages ("🎙 En écoute…", "🤖 Répond…")

---

## Points d'attention

### Écho TTS → micro

Le micro ne doit s'ouvrir qu'**après** la fin du TTS, sinon le
`SpeechRecognizer` capte la voix de synthèse.
Solution : le callback `onSpeakFinished` garantit l'ordre.

### Relance du `SpeechRecognizer`

Android `SpeechRecognizer` s'arrête après chaque résultat — il faut le
relancer manuellement à chaque tour. Ce comportement est normal et géré
par la boucle de la state machine.

### Faux déclenchements

Filtrer les résultats à faible confiance :
- Ignorer les résultats vides ou inférieurs à un seuil de confiance
- Option : afficher le texte reconnu et laisser 1–2 s pour annuler avant envoi

### Consommation batterie

Le mode mains-libres maintient le micro actif en continu.
Ajouter un timeout d'inactivité (ex : 2 min sans parole → retour `IDLE`)
avec notification visuelle.

### Interruption

L'utilisateur doit pouvoir interrompre l'IA pendant qu'elle parle :
- Tap sur l'écran → `ttsPlayer.stop()` + passer directement en `LISTENING`

---

## Correction des erreurs de transcription vocale

### Problème

La reconnaissance vocale confond fréquemment des mots phonétiquement
proches (*vais/veux*, *a/à*, *son/sont*, formes conjuguées allemandes).
Ces erreurs ne viennent pas de l'apprenant — elles faussent la
conversation et peuvent induire des corrections injustes de l'IA.

### Option A — Appel LLM de correction (précis, +latence)

Avant d'envoyer le message reconnu à l'IA de conversation, on le soumet
à un second appel LLM léger :

```
Prompt système :
"Le texte suivant a été transcrit par reconnaissance vocale en [langue].
Contexte de la conversation : [2-3 derniers échanges].
Corrige uniquement les erreurs de transcription phonétique probables
(homophones, mots découpés incorrectement).
Ne corrige PAS la grammaire ni le vocabulaire de l'apprenant.
Retourne uniquement le texte corrigé, sans explication."
```

**Avantages** : correction précise, contexte pris en compte.  
**Inconvénients** : +1–2s de latence par message, coût API supplémentaire.

État à ajouter dans la state machine : `CORRECTING` entre `LISTENING` et `SENDING`.

```
LISTENING
  │  (onResults → texte non vide)
  ▼
CORRECTING   ← appel LLM de correction
  │  (texte corrigé reçu)
  ▼
SENDING
```

### Option B — Instruction dans le prompt système (sans coût, moins précis)

Ajouter au prompt système existant de l'IA de conversation :

> *"Si le message de l'utilisateur semble contenir une erreur de
> transcription vocale (mot phonétiquement proche d'un autre compte tenu
> du contexte), interprète l'intention la plus probable avant de
> répondre."*

**Avantages** : zéro latence, zéro coût supplémentaire.  
**Inconvénients** : l'IA corrige silencieusement sans que l'on sache ce
qui a été interprété ; moins fiable sur les ambiguïtés fortes.

### Option C — Fenêtre de confirmation (recommandé en priorité)

Afficher le texte reconnu à l'écran pendant ~2s avant envoi.
L'utilisateur voit la transcription et peut annuler si elle est fausse.

C'est la protection la plus simple et elle se combine avec A ou B :
- Mode automatique rapide → Option B + fenêtre de confirmation
- Mode haute qualité → Option A (la fenêtre de confirmation peut être
  raccourcie puisque le texte affiché est déjà corrigé)

### Recommandation

Implémenter dans cet ordre :
1. **Option C** (fenêtre de confirmation) — déjà prévue dans la state machine
2. **Option B** (instruction prompt) — gratuit, à ajouter immédiatement
3. **Option A** (appel LLM) — en option activable si la qualité de B est insuffisante

---

## Plan d'implémentation

| Étape | Fichiers | Complexité |
|-------|----------|------------|
| 1. Ajouter `onDone` à `TtsPlayer` | `TtsPlayer.kt`, `androidMain/TtsPlayer.kt` | Faible |
| 2. State machine dans le VM | `ConversationViewModel.kt` | Moyenne |
| 3. UI toggle + indicateur + fenêtre confirmation (Option C) | `ConversationScreen.kt` | Faible |
| 4. Instruction correction dans le prompt système (Option B) | `ConversationViewModel.kt` | Faible |
| 5. Timeout inactivité | `ConversationViewModel.kt` | Faible |
| 6. Gestion interruption (tap) | `ConversationScreen.kt` | Faible |
| 7. *(Optionnel)* Appel LLM de correction (Option A) | `ConversationViewModel.kt`, `AiService.kt` | Moyenne |

Estimation totale : **2–3h** (étapes 1–6), **+1h** pour l'option A.
