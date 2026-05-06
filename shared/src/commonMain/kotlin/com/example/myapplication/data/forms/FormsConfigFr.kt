package com.example.myapplication.data.forms

private val FR_PRONOUNS = listOf("je", "tu", "il/elle/on", "nous", "vous", "ils/elles")

private val AVOIR_PRESENT      = listOf("ai",     "as",     "a",      "avons",   "avez",   "ont")
private val AVOIR_IMPARFAIT    = listOf("avais",  "avais",  "avait",  "avions",  "aviez",  "avaient")
private val AVOIR_FUTUR        = listOf("aurai",  "auras",  "aura",   "aurons",  "aurez",  "auront")
private val AVOIR_CONDITIONNEL = listOf("aurais", "aurais", "aurait", "aurions", "auriez", "auraient")
private val AVOIR_SUBJONCTIF   = listOf("aie",    "aies",   "ait",    "ayons",   "ayez",   "aient")

private val ETRE_PRESENT       = listOf("suis",   "es",     "est",    "sommes",  "êtes",   "sont")
private val ETRE_IMPARFAIT     = listOf("étais",  "étais",  "était",  "étions",  "étiez",  "étaient")
private val ETRE_FUTUR         = listOf("serai",  "seras",  "sera",   "serons",  "serez",  "seront")
private val ETRE_CONDITIONNEL  = listOf("serais", "serais", "serait", "serions", "seriez", "seraient")
private val ETRE_SUBJONCTIF    = listOf("sois",   "sois",   "soit",   "soyons",  "soyez",  "soient")

private fun conjugateWith(auxiliaryForms: List<String>, nonFinite: String): List<FormRow> =
    FR_PRONOUNS.zip(auxiliaryForms) { pronoun, aux -> FormRow(pronoun, "$aux $nonFinite") }

private fun auxForms(dbForms: Map<String, List<FormRow>>, avoir: List<String>, être: List<String>): List<String> {
    val auxiliary = dbForms["auxiliary"]?.firstOrNull()?.form ?: "avoir"
    return if (auxiliary == "être") être else avoir
}

private fun compoundWith(
    avoir: List<String>,
    être: List<String>
): (String, Map<String, List<FormRow>>) -> List<FormRow> = { _, dbForms ->
    dbForms["participle_past"]?.firstOrNull()?.form
        ?.let { pp -> conjugateWith(auxForms(dbForms, avoir, être), pp) }
        ?: emptyList()
}

private val VERB = setOf("verb")
private val ADJ  = setOf("adj")
private val NOUN = setOf("noun")

val FormsConfigFr = FormsConfig(
    groups = listOf(
        // Indicatif — simple / composé alternés
        GroupConfig("indicative_present",    "Présent",              pos = VERB),
        GroupConfig("passe_compose",         "Passé composé",        pos = VERB, derive = compoundWith(AVOIR_PRESENT,      ETRE_PRESENT)),
        GroupConfig("indicative_imperfect",  "Imparfait",            pos = VERB),
        GroupConfig("plus_que_parfait",      "Plus-que-parfait",     pos = VERB, derive = compoundWith(AVOIR_IMPARFAIT,    ETRE_IMPARFAIT)),
        GroupConfig("indicative_past",       "Passé simple",         pos = VERB),
        GroupConfig("futur_anterieur",       "Futur antérieur",      pos = VERB, derive = compoundWith(AVOIR_FUTUR,        ETRE_FUTUR)),
        GroupConfig("indicative_future",     "Futur simple",         pos = VERB),
        // Conditionnel
        GroupConfig("conditional",           "Conditionnel présent", pos = VERB),
        GroupConfig("conditionnel_passe",    "Conditionnel passé",   pos = VERB, derive = compoundWith(AVOIR_CONDITIONNEL, ETRE_CONDITIONNEL)),
        // Subjonctif
        GroupConfig("subjunctive_present",   "Subjonctif présent",   pos = VERB),
        GroupConfig("subjonctif_passe",      "Subjonctif passé",     pos = VERB, derive = compoundWith(AVOIR_SUBJONCTIF,   ETRE_SUBJONCTIF)),
        // Impératif & formes nominales
        GroupConfig("imperative",            "Impératif",            pos = VERB),
        GroupConfig("participle_present",    "Participe présent",    pos = VERB),
        GroupConfig("participle_past",       "Participe passé",      pos = VERB),
        // Adjectifs
        GroupConfig("masculine_singular",    "Masc. Sg.",            pos = ADJ),
        GroupConfig("masculine_plural",      "Masc. Pl.",            pos = ADJ),
        GroupConfig("feminine_singular",     "Fém. Sg.",             pos = ADJ),
        GroupConfig("feminine_plural",       "Fém. Pl.",             pos = ADJ),
        // Noms
        GroupConfig("plural",               "Pluriel",               pos = NOUN),
        // Auxiliaire (utilisé par les lambdas, affiché pour info)
        GroupConfig("auxiliary",            "Auxiliaire",            pos = VERB),
    ),
    pronounOrder = listOf("je", "tu", "il/elle/on", "nous", "vous", "ils/elles")
)
