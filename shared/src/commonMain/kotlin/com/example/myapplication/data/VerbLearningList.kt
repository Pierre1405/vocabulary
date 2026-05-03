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
    VerbEntry("wohnen",  1, VerbCategory.REGULAR),
    VerbEntry("spielen", 1, VerbCategory.REGULAR),
    VerbEntry("kochen",  1, VerbCategory.REGULAR),
    VerbEntry("hören",   1, VerbCategory.REGULAR),
    VerbEntry("sagen",   1, VerbCategory.REGULAR),
    VerbEntry("suchen",  1, VerbCategory.REGULAR),
    VerbEntry("lieben",  1, VerbCategory.REGULAR),
    VerbEntry("glauben", 1, VerbCategory.REGULAR),

    // ── Phase 2 : Auxiliaires ─────────────────────────────────────────────────
    VerbEntry("sein",    2, VerbCategory.AUXILIARY),
    VerbEntry("haben",   2, VerbCategory.AUXILIARY),
    VerbEntry("werden",  2, VerbCategory.AUXILIARY),

    // ── Phase 3 : Modaux ──────────────────────────────────────────────────────
    VerbEntry("können",  3, VerbCategory.MODAL),
    VerbEntry("müssen",  3, VerbCategory.MODAL),
    VerbEntry("dürfen",  3, VerbCategory.MODAL),
    VerbEntry("sollen",  3, VerbCategory.MODAL),
    VerbEntry("wollen",  3, VerbCategory.MODAL),
    VerbEntry("mögen",   3, VerbCategory.MODAL),

    // ── Phase 4 : Réguliers — insertion -e- ──────────────────────────────────
    VerbEntry("arbeiten", 4, VerbCategory.REGULAR_E_INSERT), // radical en -t
    VerbEntry("reden",    4, VerbCategory.REGULAR_E_INSERT), // radical en -d
    VerbEntry("warten",   4, VerbCategory.REGULAR_E_INSERT), // radical en -t
    VerbEntry("öffnen",   4, VerbCategory.REGULAR_E_INSERT), // consonne + n
    VerbEntry("atmen",    4, VerbCategory.REGULAR_E_INSERT), // consonne + m

    // ── Phase 5 : Réguliers — radical en -s, -z, -ss ─────────────────────────
    VerbEntry("reisen", 5, VerbCategory.REGULAR_SIBILANT), // du reist
    VerbEntry("tanzen", 5, VerbCategory.REGULAR_SIBILANT), // du tanzt
    VerbEntry("küssen", 5, VerbCategory.REGULAR_SIBILANT), // du küsst

    // ── Phase 6 : Réguliers — verbes en -eln et -ern ─────────────────────────
    VerbEntry("lächeln", 6, VerbCategory.REGULAR_ELN_ERN), // ich lächle
    VerbEntry("sammeln", 6, VerbCategory.REGULAR_ELN_ERN), // ich sammle
    VerbEntry("ändern",  6, VerbCategory.REGULAR_ELN_ERN), // ich ändere / ändre
    VerbEntry("wandern", 6, VerbCategory.REGULAR_ELN_ERN), // ich wandere / wandre

    // ── Phase 7 : Forts cl. 1 — ei → ie → ie ─────────────────────────────────
    VerbEntry("bleiben",   7, VerbCategory.STRONG),
    VerbEntry("schreiben", 7, VerbCategory.STRONG),
    VerbEntry("steigen",   7, VerbCategory.STRONG),
    VerbEntry("treiben",   7, VerbCategory.STRONG),
    VerbEntry("scheinen",  7, VerbCategory.STRONG),

    // ── Phase 8 : Forts cl. 2 — ie → o → o ──────────────────────────────────
    VerbEntry("fliegen",   8, VerbCategory.STRONG),
    VerbEntry("verlieren", 8, VerbCategory.STRONG),
    VerbEntry("frieren",   8, VerbCategory.STRONG),
    VerbEntry("biegen",    8, VerbCategory.STRONG),
    VerbEntry("ziehen",    8, VerbCategory.STRONG),

    // ── Phase 9 : Forts cl. 3 — i/e → a → u/o ───────────────────────────────
    VerbEntry("finden",    9, VerbCategory.STRONG),
    VerbEntry("singen",    9, VerbCategory.STRONG),
    VerbEntry("trinken",   9, VerbCategory.STRONG),
    VerbEntry("binden",    9, VerbCategory.STRONG),
    VerbEntry("beginnen",  9, VerbCategory.STRONG),
    VerbEntry("schwimmen", 9, VerbCategory.STRONG),

    // ── Phase 10 : Forts cl. 4 — e(→i) → a → o ──────────────────────────────
    VerbEntry("sprechen",  10, VerbCategory.STRONG),
    VerbEntry("helfen",    10, VerbCategory.STRONG),
    VerbEntry("brechen",   10, VerbCategory.STRONG),
    VerbEntry("treffen",   10, VerbCategory.STRONG),
    VerbEntry("werfen",    10, VerbCategory.STRONG),
    VerbEntry("sterben",   10, VerbCategory.STRONG),
    VerbEntry("nehmen",    10, VerbCategory.STRONG),

    // ── Phase 11 : Forts cl. 5 — e(→i) → a → e ──────────────────────────────
    VerbEntry("geben",   11, VerbCategory.STRONG),
    VerbEntry("sehen",   11, VerbCategory.STRONG),
    VerbEntry("lesen",   11, VerbCategory.STRONG),
    VerbEntry("essen",   11, VerbCategory.STRONG),
    VerbEntry("treten",  11, VerbCategory.STRONG),

    // ── Phase 12 : Forts cl. 6 — a(→ä) → u → a ──────────────────────────────
    VerbEntry("fahren",   12, VerbCategory.STRONG),
    VerbEntry("tragen",   12, VerbCategory.STRONG),
    VerbEntry("waschen",  12, VerbCategory.STRONG),
    VerbEntry("schlagen", 12, VerbCategory.STRONG),
    VerbEntry("graben",   12, VerbCategory.STRONG),
    VerbEntry("wachsen",  12, VerbCategory.STRONG),

    // ── Phase 13 : Forts cl. 7 — voyelle → ie → voyelle ──────────────────────
    VerbEntry("fallen",   13, VerbCategory.STRONG),
    VerbEntry("halten",   13, VerbCategory.STRONG),
    VerbEntry("lassen",   13, VerbCategory.STRONG),
    VerbEntry("schlafen", 13, VerbCategory.STRONG),
    VerbEntry("laufen",   13, VerbCategory.STRONG),
    VerbEntry("rufen",    13, VerbCategory.STRONG),
    VerbEntry("stoßen",   13, VerbCategory.STRONG),
    VerbEntry("heißen",   13, VerbCategory.STRONG),

    // ── Phase 14 : Très irréguliers ───────────────────────────────────────────
    VerbEntry("gehen",   14, VerbCategory.STRONG),
    VerbEntry("kommen",  14, VerbCategory.STRONG),
    VerbEntry("stehen",  14, VerbCategory.STRONG),
    VerbEntry("tun",     14, VerbCategory.STRONG),
    VerbEntry("wissen",  14, VerbCategory.STRONG),

    // ── Phase 15 : Mixtes ─────────────────────────────────────────────────────
    VerbEntry("bringen", 15, VerbCategory.MIXED),
    VerbEntry("denken",  15, VerbCategory.MIXED),
    VerbEntry("kennen",  15, VerbCategory.MIXED),
    VerbEntry("nennen",  15, VerbCategory.MIXED),
    VerbEntry("rennen",  15, VerbCategory.MIXED),
    VerbEntry("brennen", 15, VerbCategory.MIXED),
    VerbEntry("senden",  15, VerbCategory.MIXED),
    VerbEntry("wenden",  15, VerbCategory.MIXED),

    // ── Phase 16 : Préfixes séparables ───────────────────────────────────────
    VerbEntry("aufstehen",   16, VerbCategory.SEPARABLE),
    VerbEntry("ankommen",    16, VerbCategory.SEPARABLE),
    VerbEntry("einschlafen", 16, VerbCategory.SEPARABLE),
    VerbEntry("anrufen",     16, VerbCategory.SEPARABLE),
    VerbEntry("mitnehmen",   16, VerbCategory.SEPARABLE),
    VerbEntry("aufmachen",   16, VerbCategory.SEPARABLE),
    VerbEntry("ausgehen",    16, VerbCategory.SEPARABLE),
    VerbEntry("anfangen",    16, VerbCategory.SEPARABLE),
    VerbEntry("vorstellen",  16, VerbCategory.SEPARABLE),

    // ── Phase 17 : Préfixes inséparables ─────────────────────────────────────
    VerbEntry("besuchen",   17, VerbCategory.INSEPARABLE),
    VerbEntry("verstehen",  17, VerbCategory.INSEPARABLE),
    VerbEntry("erklären",   17, VerbCategory.INSEPARABLE),
    VerbEntry("vergessen",  17, VerbCategory.INSEPARABLE),
    VerbEntry("bekommen",   17, VerbCategory.INSEPARABLE),
    VerbEntry("erzählen",   17, VerbCategory.INSEPARABLE),
    VerbEntry("gefallen",   17, VerbCategory.INSEPARABLE),
    VerbEntry("gehören",    17, VerbCategory.INSEPARABLE),

    // ── Phase 18 : Réflexifs ──────────────────────────────────────────────────
    VerbEntry("sich freuen",        18, VerbCategory.REFLEXIVE),
    VerbEntry("sich erinnern",      18, VerbCategory.REFLEXIVE),
    VerbEntry("sich setzen",        18, VerbCategory.REFLEXIVE),
    VerbEntry("sich fühlen",        18, VerbCategory.REFLEXIVE),
    VerbEntry("sich befinden",      18, VerbCategory.REFLEXIVE),
    VerbEntry("sich vorstellen",    18, VerbCategory.REFLEXIVE),
    VerbEntry("sich waschen",       18, VerbCategory.REFLEXIVE),
    VerbEntry("sich interessieren", 18, VerbCategory.REFLEXIVE),

    // ── Phase 19 : Verbes en -ieren ───────────────────────────────────────────
    VerbEntry("studieren",     19, VerbCategory.IEREN),
    VerbEntry("telefonieren",  19, VerbCategory.IEREN),
    VerbEntry("fotografieren", 19, VerbCategory.IEREN),
    VerbEntry("passieren",     19, VerbCategory.IEREN),
    VerbEntry("diskutieren",   19, VerbCategory.IEREN),
    VerbEntry("funktionieren", 19, VerbCategory.IEREN),
    VerbEntry("interessieren", 19, VerbCategory.IEREN),
)
