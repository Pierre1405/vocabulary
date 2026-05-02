package com.example.myapplication.data.forms

private val HABEN_PRESENT = listOf("habe", "hast", "hat", "haben", "habt", "haben")
private val SEIN_PRESENT   = listOf("bin",  "bist", "ist", "sind",  "seid", "sind")
private val WERDEN_PRESENT = listOf("werde","wirst","wird","werden","werdet","werden")
private val DE_PRONOUNS    = listOf("ich", "du", "er,sie,es", "wir", "ihr", "sie")

private fun conjugateWith(auxiliaryForms: List<String>, nonFinite: String): List<FormRow> =
    DE_PRONOUNS.zip(auxiliaryForms) { pronoun, aux -> FormRow(pronoun, "$aux $nonFinite") }

val FormsConfigDe = FormsConfig(
    groups = listOf(
        GroupConfig("indicative_present",  "Präsens"),
        GroupConfig("indicative_past",     "Präteritum"),
        GroupConfig("indicative_future",   "Futur I", derive = { lemma, _ ->
            conjugateWith(WERDEN_PRESENT, lemma)
        }),
        GroupConfig("subjunctive_i",       "Konjunktiv I"),
        GroupConfig("subjunctive_ii",      "Konjunktiv II"),
        GroupConfig("participle_present",  "Partizip I", derive = { lemma, _ ->
            val form = if (lemma.endsWith("en")) lemma + "d" else lemma.dropLast(1) + "end"
            listOf(FormRow(label = null, form = form))
        }),
        GroupConfig("participle_past",     "Partizip II"),
        GroupConfig("auxiliary",           "Hilfsverb"),
        GroupConfig("perfect",             "Perfekt", derive = { _, dbForms ->
            val partizipII = dbForms["participle_past"]?.firstOrNull()?.form
            partizipII?.let { pp ->
                val auxiliary = dbForms["auxiliary"]?.firstOrNull()?.form ?: "haben"
                val auxForms  = if (auxiliary == "sein") SEIN_PRESENT else HABEN_PRESENT
                conjugateWith(auxForms, pp)
            } ?: emptyList()
        }),
        GroupConfig("imperative",          "Imperativ"),
        GroupConfig("nominative",          "Nominativ"),
        GroupConfig("accusative",          "Akkusativ"),
        GroupConfig("dative",              "Dativ"),
        GroupConfig("genitive",            "Genitiv"),
        GroupConfig("positive",            "Positiv"),
        GroupConfig("comparative",         "Komparativ"),
        GroupConfig("superlative",         "Superlativ"),
    ),
    pronounOrder = listOf("ich", "du", "er,sie,es", "wir", "ihr", "sie")
)
