package com.example.myapplication.data

import app.cash.sqldelight.db.SqlDriver
import com.example.myapplication.db.dictionary.DictionaryDatabase

data class DictEntry(
    val id: Long,
    val lemma: String,
    val locale: String,
    val pos: String?,
    val gender: String?,
    val example: String?         // non null = exemple partagé (fallback)
)

data class DictTranslation(
    val id: Long,
    val entryId: Long,
    val targetLocale: String,
    val text: String,
    val glossSource: String?,
    val example: String?         // non null = exemple précis pour cette traduction
)

data class DictForm(
    val form: String,
    val features: String?,
    val pronouns: String?
)

data class DictLookupResult(
    val entry: DictEntry,
    val features: String?,
    val pronouns: String?
)

class DictionaryRepository(driver: SqlDriver) {

    private val db = DictionaryDatabase(driver)
    private val entryQueries = db.dictEntryQueries
    private val translationQueries = db.dictTranslationQueries
    private val formQueries = db.dictFormQueries

    /** Retourne une entrée par son id. */
    fun getById(id: Long): DictEntry? =
        entryQueries.getById(id).executeAsOneOrNull()?.toDictEntry()

    /** Cherche une entrée par lemme exact. */
    fun getByLemma(lemma: String, locale: String): List<DictEntry> =
        entryQueries.getByLemma(lemma, locale).executeAsList().map { it.toDictEntry() }

    /** Cherche des entrées dont le lemme commence par un préfixe. */
    fun searchByPrefix(prefix: String, locale: String): List<DictEntry> =
        entryQueries.searchByPrefix("$prefix%", locale).executeAsList().map { it.toDictEntry() }

    /** Retourne toutes les traductions d'une entrée. */
    fun getTranslations(entryId: Long): List<DictTranslation> =
        translationQueries.getByEntryId(entryId).executeAsList().map { it.toDictTranslation() }

    /** Retourne toutes les formes fléchies d'une entrée (tableau de conjugaison/déclinaison). */
    fun getForms(entryId: Long): List<DictForm> =
        formQueries.getFormsByEntryId(entryId).executeAsList().map {
            DictForm(form = it.form, features = it.features, pronouns = it.pronouns)
        }

    /**
     * Lookup depuis une forme fléchie : retourne l'entrée correspondante.
     * Ex: "liebte" → entrée "lieben"
     */
    fun lookupByForm(form: String): List<DictLookupResult> =
        formQueries.lookupByForm(form).executeAsList().map {
            DictLookupResult(
                entry = DictEntry(
                    id = it.id,
                    lemma = it.lemma,
                    locale = it.locale,
                    pos = it.pos,
                    gender = it.gender,
                    example = it.example
                ),
                features = it.features,
                pronouns = it.pronouns
            )
        }

    /**
     * Retourne l'exemple à afficher pour une traduction.
     * Priorité : exemple précis de la traduction, sinon exemple fallback de l'entrée.
     */
    fun resolveExample(translation: DictTranslation, entry: DictEntry): String? =
        translation.example ?: entry.example

    // Extensions de mapping
    private fun com.example.myapplication.db.dictionary.Dict_entry.toDictEntry() = DictEntry(
        id = id, lemma = lemma, locale = locale, pos = pos, gender = gender, example = example
    )

    private fun com.example.myapplication.db.dictionary.Dict_translation.toDictTranslation() = DictTranslation(
        id = id, entryId = entry_id, targetLocale = target_locale,
        text = text, glossSource = gloss_source, example = example
    )
}
