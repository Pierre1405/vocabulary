package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordReviewScreen(
    dictionaryRepository: DictionaryRepository,
    learningRepository: LearningRepository,
    reversed: Boolean = false,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: WordReviewViewModel = viewModel {
        WordReviewViewModel(dictionaryRepository, learningRepository, reversed)
    }

    val items by viewModel.items.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val currentGrade by viewModel.currentGrade.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mots" +
                        if (items.isNotEmpty()) "  ${currentIndex + 1} / ${items.size}" else ""
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun mot à réviser.")
            }
        } else {
            val item = items[currentIndex]
            WordReviewCard(
                item = item,
                reversed = viewModel.reversed,
                currentGrade = currentGrade,
                onGradeSelected = { grade -> viewModel.saveGrade(item.translationId, item.entryId, grade) },
                onNext = { viewModel.moveToNext() },
                onPrevious = { viewModel.moveToPrevious() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun WordReviewCard(
    item: WordReviewItem,
    reversed: Boolean,
    currentGrade: Int?,
    onGradeSelected: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAnswer by remember(item.translationId) { mutableStateOf(false) }

    val sourceText = if (reversed) "${localeToFlag(item.translationLocale)}  ${item.translationText}"
                     else "${localeToFlag(item.wordLocale)}  ${item.lemma}"
    val answerText = if (reversed) "${localeToFlag(item.wordLocale)}  ${item.lemma}"
                     else "${localeToFlag(item.translationLocale)}  ${item.translationText}"

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = sourceText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!showAnswer)
                                Modifier.clickable { showAnswer = true }.blur(8.dp)
                            else Modifier
                        )
                ) {
                    Text(
                        text = answerText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            (1..5).forEach { grade ->
                val isSelected = grade == currentGrade
                Button(
                    onClick = { onGradeSelected(grade) },
                    modifier = Modifier.weight(if (isSelected) 1.3f else 1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gradeColor(grade).copy(alpha = if (isSelected) 1f else 0.45f)
                    ),
                    border = if (isSelected) BorderStroke(2.dp, gradeColor(grade)) else null
                ) {
                    Text(
                        text = "$grade",
                        style = if (isSelected) MaterialTheme.typography.titleMedium
                                else MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onPrevious, modifier = Modifier.weight(1f)) {
                Text("← Précédent")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text("Suivant →")
            }
        }
    }
}
