package com.example.myapplication.data

// ---------------------------------------------------------------------------
// Programme d'apprentissage des conjugaisons allemandes
// ---------------------------------------------------------------------------
//
// GERMAN_TENSE_LIST  — clés ordonnées par fréquence (index 0 = plus fréquent)
//                      Chaque clé = GroupConfig.key dans FormsConfigDe
//
// VerbCategory       — catégorie grammaticale ; les réguliers sont subdivisés
//                      en 4 sous-catégories selon leur particularité ortho.
//
// VerbEntry          — (infinitif, phase, catégorie)
//
// GERMAN_VERB_LEARNING_LIST — 93 verbes, 19 phases :
//   1  – Réguliers — patron de base
//   2  – Auxiliaires (sein, haben, werden)
//   3  – Modaux (können, müssen, dürfen, sollen, wollen, mögen)
//   4  – Réguliers — insertion -e- (radical en -t, -d, consonne+n/m)
//   5  – Réguliers — radical en -s, -z, -ss (2e sg : du reist, tanzt, küsst)
//   6  – Réguliers — verbes en -eln et -ern
//   7  – Forts classe 1 : ei → ie → ie
//   8  – Forts classe 2 : ie → o → o
//   9  – Forts classe 3 : i/e → a → u/o
//  10  – Forts classe 4 : e(→i) → a → o
//  11  – Forts classe 5 : e(→i) → a → e
//  12  – Forts classe 6 : a(→ä) → u → a
//  13  – Forts classe 7 : voyelle → ie → voyelle
//  14  – Très irréguliers hors classe
//  15  – Verbes mixtes (Mischverben)
//  16  – Préfixes séparables
//  17  – Préfixes inséparables
//  18  – Réflexifs
//  19  – Verbes en -ieren
// ---------------------------------------------------------------------------

/** Temps verbaux ordonnés par fréquence (index 0 = le plus fréquent).
 *  Les clés correspondent aux [GroupConfig.key] de [FormsConfigDe]. */
val GERMAN_TENSE_LIST: List<String> = listOf(
    "indicative_present",   // Präsens
    "perfect",              // Perfekt
    "indicative_past",      // Präteritum
    "imperative",           // Imperativ
    "indicative_future",    // Futur I
    "subjunctive_ii",       // Konjunktiv II
    "participle_past",      // Partizip II  (passif, adjectif verbal)
    "participle_present",   // Partizip I   (adjectif verbal)
    "subjunctive_i",        // Konjunktiv I (discours indirect)
)

enum class VerbCategory(val labelFr: String) {
    // Réguliers — patron de base (radical + terminaisons standards)
    REGULAR("Régulier"),
    // Réguliers — insertion d'un -e- devant les terminaisons -st/-t/-te
    // quand le radical se termine par -t, -d, ou groupe consonne+n/m
    REGULAR_E_INSERT("Régulier — insertion -e-"),
    // Réguliers — radical terminé par -s, -ss, -ß, -z, -x :
    // la 2e pers. sg. perd le -s additionnel (du reist, tanzt, küsst)
    REGULAR_SIBILANT("Régulier — radical en -s/-z/-ss"),
    // Réguliers — infinitif en -eln ou -ern :
    // chute du -e- intérieur à la 1re sg (ich lächle, ich wandre)
    REGULAR_ELN_ERN("Régulier — verbe en -eln/-ern"),
    AUXILIARY("Auxiliaire"),
    MODAL("Modal"),
    STRONG("Fort (Ablaut)"),
    MIXED("Mixte"),
    SEPARABLE("Préfixe séparable"),
    INSEPARABLE("Préfixe inséparable"),
    REFLEXIVE("Réflexif"),
    IEREN("Verbe en -ieren")
}

data class VerbEntry(
    val infinitive: String,
    val phase: Int,
    val category: VerbCategory
)

val GERMAN_VERB_LEARNING_LIST: List<VerbEntry> = listOf(

    // ── Phase 1 : Réguliers — patron de base ─────────────────────────────────
    VerbEntry("machen",  1, VerbCategory.REGULAR),
    VerbEntry("lernen",  1, VerbCategory.REGULAR),
    VerbEntry("kaufen",  1, VerbCategory.REGULAR),

    // ── Phase 2 : Auxiliaires ─────────────────────────────────────────────────
    VerbEntry("sein",    2, VerbCategory.AUXILIARY),
    VerbEntry("haben",   2, VerbCategory.AUXILIARY),
    VerbEntry("werden",  2, VerbCategory.AUXILIARY),

    // ── Phase 3 : Modaux ──────────────────────────────────────────────────────
    VerbEntry("können",  3, VerbCategory.MODAL),
    VerbEntry("müssen",  3, VerbCategory.MODAL),
    VerbEntry("dürfen",  3, VerbCategory.MODAL),

    // ── Phase 4 : Réguliers — insertion -e- ──────────────────────────────────
    VerbEntry("arbeiten", 4, VerbCategory.REGULAR_E_INSERT),
    VerbEntry("reden",    4, VerbCategory.REGULAR_E_INSERT),
    VerbEntry("warten",   4, VerbCategory.REGULAR_E_INSERT),

    // ── Phase 5 : Réguliers — radical en -s, -z, -ss ─────────────────────────
    VerbEntry("reisen", 5, VerbCategory.REGULAR_SIBILANT),
    VerbEntry("tanzen", 5, VerbCategory.REGULAR_SIBILANT),
    VerbEntry("küssen", 5, VerbCategory.REGULAR_SIBILANT),

    // ── Phase 6 : Réguliers — verbes en -eln et -ern ─────────────────────────
    VerbEntry("lächeln", 6, VerbCategory.REGULAR_ELN_ERN),
    VerbEntry("sammeln", 6, VerbCategory.REGULAR_ELN_ERN),
    VerbEntry("ändern",  6, VerbCategory.REGULAR_ELN_ERN),

    // ── Phase 7 : Forts cl. 1 — ei → ie → ie ─────────────────────────────────
    VerbEntry("bleiben",   7, VerbCategory.STRONG),
    VerbEntry("schreiben", 7, VerbCategory.STRONG),
    VerbEntry("steigen",   7, VerbCategory.STRONG),

    // ── Phase 8 : Forts cl. 2 — ie → o → o ──────────────────────────────────
    VerbEntry("fliegen",   8, VerbCategory.STRONG),
    VerbEntry("verlieren", 8, VerbCategory.STRONG),
    VerbEntry("frieren",   8, VerbCategory.STRONG),

    // ── Phase 9 : Forts cl. 3 — i/e → a → u/o ───────────────────────────────
    VerbEntry("finden",  9, VerbCategory.STRONG),
    VerbEntry("singen",  9, VerbCategory.STRONG),
    VerbEntry("trinken", 9, VerbCategory.STRONG),

    // ── Phase 10 : Forts cl. 4 — e(→i) → a → o ──────────────────────────────
    VerbEntry("sprechen", 10, VerbCategory.STRONG),
    VerbEntry("helfen",   10, VerbCategory.STRONG),
    VerbEntry("brechen",  10, VerbCategory.STRONG),

    // ── Phase 11 : Forts cl. 5 — e(→i) → a → e ──────────────────────────────
    VerbEntry("geben", 11, VerbCategory.STRONG),
    VerbEntry("sehen", 11, VerbCategory.STRONG),
    VerbEntry("lesen", 11, VerbCategory.STRONG),

    // ── Phase 12 : Forts cl. 6 — a(→ä) → u → a ──────────────────────────────
    VerbEntry("fahren",  12, VerbCategory.STRONG),
    VerbEntry("tragen",  12, VerbCategory.STRONG),
    VerbEntry("waschen", 12, VerbCategory.STRONG),

    // ── Phase 13 : Forts cl. 7 — voyelle → ie → voyelle ──────────────────────
    VerbEntry("fallen", 13, VerbCategory.STRONG),
    VerbEntry("halten", 13, VerbCategory.STRONG),
    VerbEntry("lassen", 13, VerbCategory.STRONG),

    // ── Phase 14 : Très irréguliers ───────────────────────────────────────────
    VerbEntry("gehen",  14, VerbCategory.STRONG),
    VerbEntry("kommen", 14, VerbCategory.STRONG),
    VerbEntry("stehen", 14, VerbCategory.STRONG),

    // ── Phase 15 : Mixtes ─────────────────────────────────────────────────────
    VerbEntry("bringen", 15, VerbCategory.MIXED),
    VerbEntry("denken",  15, VerbCategory.MIXED),
    VerbEntry("kennen",  15, VerbCategory.MIXED),

    // ── Phase 16 : Préfixes séparables ───────────────────────────────────────
    VerbEntry("aufstehen",   16, VerbCategory.SEPARABLE),
    VerbEntry("ankommen",    16, VerbCategory.SEPARABLE),
    VerbEntry("einschlafen", 16, VerbCategory.SEPARABLE),

    // ── Phase 17 : Préfixes inséparables ─────────────────────────────────────
    VerbEntry("besuchen",  17, VerbCategory.INSEPARABLE),
    VerbEntry("verstehen", 17, VerbCategory.INSEPARABLE),
    VerbEntry("erklären",  17, VerbCategory.INSEPARABLE),

    // ── Phase 18 : Réflexifs ──────────────────────────────────────────────────
    VerbEntry("sich freuen",   18, VerbCategory.REFLEXIVE),
    VerbEntry("sich erinnern", 18, VerbCategory.REFLEXIVE),
    VerbEntry("sich setzen",   18, VerbCategory.REFLEXIVE),

    // ── Phase 19 : Verbes en -ieren ───────────────────────────────────────────
    VerbEntry("studieren",    19, VerbCategory.IEREN),
    VerbEntry("telefonieren", 19, VerbCategory.IEREN),
    VerbEntry("fotografieren",19, VerbCategory.IEREN),
)
