package com.example.myapplication.data.forms

private val HABEN_PRESENT = listOf("habe", "hast", "hat", "haben", "habt", "haben")
private val SEIN_PRESENT   = listOf("bin",  "bist", "ist", "sind",  "seid", "sind")
private val WERDEN_PRESENT = listOf("werde","wirst","wird","werden","werdet","werden")
private val DE_PRONOUNS    = listOf("ich", "du", "er,sie,es", "wir", "ihr", "sie")

private fun conjugateWith(auxiliaryForms: List<String>, nonFinite: String): List<FormRow> =
    DE_PRONOUNS.zip(auxiliaryForms) { pronoun, aux -> FormRow(pronoun, "$aux $nonFinite") }

private val VERB = setOf("verb")
private val NOUN = setOf("noun")
private val ADJ  = setOf("adj")

val FormsConfigDe = FormsConfig(
    groups = listOf(
        GroupConfig("indicative_present",  "Präsens",      pos = VERB),
        GroupConfig("indicative_past",     "Präteritum",   pos = VERB),
        GroupConfig("indicative_future",   "Futur I",      pos = VERB, derive = { lemma, _ ->
            conjugateWith(WERDEN_PRESENT, lemma)
        }),
        GroupConfig("subjunctive_i",       "Konjunktiv I",  pos = VERB),
        GroupConfig("subjunctive_ii",      "Konjunktiv II", pos = VERB),
        GroupConfig("participle_present",  "Partizip I",    pos = VERB, derive = { lemma, _ ->
            val form = if (lemma.endsWith("en")) lemma + "d" else lemma.dropLast(1) + "end"
            listOf(FormRow(label = null, form = form))
        }),
        GroupConfig("participle_past",     "Partizip II",  pos = VERB),
        GroupConfig("auxiliary",           "Hilfsverb",    pos = VERB),
        GroupConfig("perfect",             "Perfekt",      pos = VERB, derive = { _, dbForms ->
            val partizipII = dbForms["participle_past"]?.firstOrNull()?.form
            partizipII?.let { pp ->
                val auxiliary = dbForms["auxiliary"]?.firstOrNull()?.form ?: "haben"
                val auxForms  = if (auxiliary == "sein") SEIN_PRESENT else HABEN_PRESENT
                conjugateWith(auxForms, pp)
            } ?: emptyList()
        }),
        GroupConfig("imperative",          "Imperativ",    pos = VERB),
        GroupConfig("nominative",          "Nominativ",    pos = NOUN),
        GroupConfig("accusative",          "Akkusativ",    pos = NOUN),
        GroupConfig("dative",              "Dativ",        pos = NOUN),
        GroupConfig("genitive",            "Genitiv",      pos = NOUN),
        GroupConfig("positive",            "Positiv",      pos = ADJ, derive = { lemma, _ ->
            listOf(FormRow(label = null, form = lemma))
        }),
        GroupConfig("comparative",         "Komparativ",   pos = ADJ),
        GroupConfig("superlative",         "Superlativ",   pos = ADJ),
    ),
    pronounOrder = listOf("ich", "du", "er,sie,es", "wir", "ihr", "sie")
)
