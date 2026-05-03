package com.example.myapplication.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.LocalStrings

@Composable
fun ReviewCompletionScreen(
    onRestart: () -> Unit,
    onRestartLowGrades: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(LocalStrings.current.completionTitle, style = MaterialTheme.typography.headlineMedium)

        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(LocalStrings.current.completionRestart)
        }

        Button(onClick = onRestartLowGrades, modifier = Modifier.fillMaxWidth()) {
            Text(LocalStrings.current.completionRestartLow)
        }

        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(LocalStrings.current.completionFinish)
        }
    }
}
