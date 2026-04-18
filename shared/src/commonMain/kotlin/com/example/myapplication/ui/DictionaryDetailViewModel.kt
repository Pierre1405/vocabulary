package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DictEntry
import com.example.myapplication.data.DictTranslation
import com.example.myapplication.data.DictionaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FormRow(val label: String?, val form: String)
data class FormGroup(val label: String, val rows: List<FormRow>)

data class DictionaryDetailState(
    val entry: DictEntry? = null,
    val translations: List<DictTranslation> = emptyList(),
    val formGroups: List<FormGroup> = emptyList()
)

class DictionaryDetailViewModel(
    private val dictionaryRepository: DictionaryRepository,
    private val entryId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(DictionaryDetailState())
    val state: StateFlow<DictionaryDetailState> = _state

    init {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.Default) {
                val entry = dictionaryRepository.getById(entryId)
                val translations = dictionaryRepository.getTranslations(entryId)
                val forms = dictionaryRepository.getForms(entryId)
                val formGroups = buildFormGroups(forms.map { Triple(it.form, it.features, it.pronouns) })
                DictionaryDetailState(entry, translations, formGroups)
            }
        }
    }

    private fun buildFormGroups(forms: List<Triple<String, String?, String?>>): List<FormGroup> {
        return forms
            .groupBy { featuresGroupKey(it.second) }
            .entries
            .sortedBy { GROUP_ORDER.indexOf(it.key).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
            .map { (key, groupForms) ->
                FormGroup(
                    label = groupKeyToLabel(key),
                    rows = groupForms.map { (form, features, pronouns) ->
                        FormRow(label = formRowLabel(features, pronouns), form = form)
                    }
                )
            }
    }

    companion object {
        private val GROUP_ORDER = listOf(
            "infinitive",
            "indicative_present", "indicative_past", "indicative_imperfect",
            "indicative_future", "indicative_past_historic", "indicative",
            "subjunctive_ii",
            "subjunctive_present", "subjunctive_past", "subjunctive",
            "conditional",
            "imperative",
            "participle_present", "participle_past", "participle",
            "auxiliary",
            "nominative", "accusative", "dative", "genitive",
            "positive", "comparative", "superlative",
            "plural"
        )

        fun featuresGroupKey(features: String?): String {
            if (features.isNullOrBlank()) return "other"
            val tags = features.split(",").map { it.trim() }
            return when {
                "infinitive" in tags                                        -> "infinitive"
                "participle" in tags && "past" in tags                      -> "participle_past"
                "participle" in tags && "present" in tags                   -> "participle_present"
                "participle" in tags                                        -> "participle"
                "auxiliary" in tags                                         -> "auxiliary"
                "subjunctive-ii" in tags                                    -> "subjunctive_ii"
                "indicative" in tags && "present" in tags                   -> "indicative_present"
                "indicative" in tags && "past-historic" in tags             -> "indicative_past_historic"
                "indicative" in tags && "past" in tags                      -> "indicative_past"
                "indicative" in tags && "imperfect" in tags                 -> "indicative_imperfect"
                "indicative" in tags && "future" in tags                    -> "indicative_future"
                "indicative" in tags                                        -> "indicative"
                "subjunctive" in tags && "present" in tags                  -> "subjunctive_present"
                "subjunctive" in tags && "past" in tags                     -> "subjunctive_past"
                "subjunctive" in tags                                       -> "subjunctive"
                "conditional" in tags                                       -> "conditional"
                "imperative" in tags                                        -> "imperative"
                "nominative" in tags                                        -> "nominative"
                "accusative" in tags                                        -> "accusative"
                "dative" in tags                                            -> "dative"
                "genitive" in tags                                          -> "genitive"
                "comparative" in tags                                       -> "comparative"
                "superlative" in tags                                       -> "superlative"
                "positive" in tags                                          -> "positive"
                "plural" in tags                                            -> "plural"
                else -> features
            }
        }

        fun groupKeyToLabel(key: String): String = when (key) {
            "infinitive"             -> "Infinitif"
            "indicative_present"     -> "Présent"
            "indicative_past"        -> "Prétérit"
            "indicative_imperfect"   -> "Imparfait"
            "indicative_future"      -> "Futur"
            "indicative_past_historic" -> "Passé simple"
            "indicative"             -> "Indicatif"
            "subjunctive_ii"         -> "Konjunktiv II"
            "subjunctive_present"    -> "Subjonctif présent"
            "subjunctive_past"       -> "Subjonctif passé"
            "subjunctive"            -> "Subjonctif"
            "conditional"            -> "Conditionnel"
            "imperative"             -> "Impératif"
            "participle_present"     -> "Participe présent"
            "participle_past"        -> "Participe passé"
            "participle"             -> "Participe"
            "auxiliary"              -> "Auxiliaire"
            "nominative"             -> "Nominatif"
            "accusative"             -> "Accusatif"
            "dative"                 -> "Datif"
            "genitive"               -> "Génitif"
            "positive"               -> "Positif"
            "comparative"            -> "Comparatif"
            "superlative"            -> "Superlatif"
            "plural"                 -> "Pluriel"
            else -> key
        }

        fun formRowLabel(features: String?, pronouns: String?): String? {
            if (!pronouns.isNullOrBlank()) return pronouns
            if (features.isNullOrBlank()) return null
            val tags = features.split(",").map { it.trim() }
            return when {
                "masculine" in tags && "singular" in tags -> "Masc. Sg."
                "masculine" in tags && "plural" in tags   -> "Masc. Pl."
                "feminine" in tags && "singular" in tags  -> "Fém. Sg."
                "feminine" in tags && "plural" in tags    -> "Fém. Pl."
                "neuter" in tags && "singular" in tags    -> "Neutre Sg."
                "neuter" in tags && "plural" in tags      -> "Neutre Pl."
                "masculine" in tags                       -> "Masculin"
                "feminine" in tags                        -> "Féminin"
                "neuter" in tags                          -> "Neutre"
                "singular" in tags                        -> "Singulier"
                "plural" in tags                          -> "Pluriel"
                else -> null
            }
        }
    }
}
