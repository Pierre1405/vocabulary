package com.example.myapplication.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.LocalStrings

@Composable
fun ReviewPreviewScreen(
    items: List<Pair<String, String>>,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Text(
                text = strings.reviewPreviewTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        itemsIndexed(items) { index, (source, target) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = target,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            if (index < items.lastIndex) {
                HorizontalDivider()
            }
        }

        item {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Text(strings.reviewPreviewStart)
            }
        }
    }
}
