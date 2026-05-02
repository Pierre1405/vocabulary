package com.example.myapplication.ui.dictionary

import com.example.myapplication.data.forms.FormGroup
import com.example.myapplication.data.forms.FormRow
import com.example.myapplication.data.forms.FormsConfig

internal fun buildFormGroups(
    forms: List<Triple<String, String?, String?>>, // form, group_key, pronouns
    config: FormsConfig,
    lemma: String
): List<FormGroup> {
    val groupByKey = config.groups.associateBy { it.key }

    val groupedFromDb = forms
        .filter { (_, groupKey, _) -> groupKey != null && groupKey in groupByKey }
        .distinctBy { (form, groupKey, pronouns) -> Triple(groupKey, pronouns, form) }
        .groupBy { (_, groupKey, _) -> groupKey!! }

    val resolvedDbForms: Map<String, List<FormRow>> = groupedFromDb.mapValues { (_, groupForms) ->
        groupForms
            .sortedBy { (_, _, pronouns) ->
                config.pronounOrder.indexOf(pronouns).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
            .map { (form, _, pronouns) -> FormRow(label = pronouns, form = form) }
    }

    val derivedGroups: Map<String, List<FormRow>> = config.groups
        .filter { it.derive != null && it.key !in groupedFromDb }
        .associate { it.key to it.derive!!(lemma, resolvedDbForms) }
        .filter { (_, rows) -> rows.isNotEmpty() }

    return (groupedFromDb.keys + derivedGroups.keys)
        .distinct()
        .sortedBy { key -> config.groups.indexOfFirst { it.key == key } }
        .mapNotNull { key ->
            val groupConfig = groupByKey[key] ?: return@mapNotNull null
            val rows = resolvedDbForms[key] ?: derivedGroups[key] ?: return@mapNotNull null
            FormGroup(label = groupConfig.label, rows = rows)
        }
}
