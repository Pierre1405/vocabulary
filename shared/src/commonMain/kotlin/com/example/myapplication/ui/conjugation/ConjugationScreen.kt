package com.example.myapplication.ui.conjugation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.GERMAN_VERB_LEARNING_LIST
import com.example.myapplication.ui.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConjugationScreen(
    dictionaryRepository: DictionaryRepository,
    onVerbClick: (entryId: Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ConjugationViewModel = viewModel { ConjugationViewModel(dictionaryRepository) }
    val entryIdByInfinitive by viewModel.entryIdByInfinitive.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val verbsByPhase = remember { GERMAN_VERB_LEARNING_LIST.groupBy { it.phase } }
    val sortedPhases = remember { verbsByPhase.keys.sorted() }

    var expandedPhases by remember { mutableStateOf(emptySet<Int>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalStrings.current.homeConjugation) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LocalStrings.current.back)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            sortedPhases.forEach { phase ->
                val verbs = verbsByPhase[phase] ?: return@forEach
                val phaseLabel = "Phase $phase · ${verbs.first().category.labelFr}"
                val isExpanded = phase in expandedPhases

                item(key = "phase_$phase") {
                    PhaseSectionHeader(
                        label = phaseLabel,
                        isExpanded = isExpanded,
                        onClick = {
                            expandedPhases = if (phase in expandedPhases)
                                expandedPhases - phase
                            else
                                expandedPhases + phase
                        }
                    )
                    HorizontalDivider()
                }

                if (isExpanded) {
                    items(verbs, key = { "verb_${it.infinitive}" }) { verb ->
                        val entryId = entryIdByInfinitive[verb.infinitive]
                        VerbRow(
                            infinitive = verb.infinitive,
                            enabled = entryId != null,
                            onClick = { if (entryId != null) onVerbClick(entryId) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PhaseSectionHeader(
    label: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null
        )
    }
}

@Composable
private fun VerbRow(
    infinitive: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = infinitive,
        style = MaterialTheme.typography.bodyMedium,
        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 10.dp)
    )
}
