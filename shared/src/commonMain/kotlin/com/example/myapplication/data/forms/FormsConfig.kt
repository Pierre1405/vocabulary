package com.example.myapplication.data.forms

data class FormRow(val label: String?, val form: String)
data class FormGroup(val key: String, val label: String, val rows: List<FormRow>)

data class FormsConfig(
    val groups: List<GroupConfig>,
    val pronounOrder: List<String>
)

data class GroupConfig(
    val key: String,
    val label: String,
    val pos: Set<String>? = null,
    val derive: ((lemma: String, dbForms: Map<String, List<FormRow>>) -> List<FormRow>)? = null
)
