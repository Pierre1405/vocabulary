package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onLectureClick: () -> Unit,
    onRevisionClick: () -> Unit,
    onDictionnaireClick: () -> Unit,
    onConjugaisonsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = LocalStrings.current.appTitle,
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(modifier = Modifier.height(64.dp))

        HomeButton(
            label = LocalStrings.current.homeReading,
            icon = { Icon(Icons.Filled.AutoStories, contentDescription = null, modifier = Modifier.size(28.dp)) },
            onClick = onLectureClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeButton(
            label = LocalStrings.current.homeReview,
            icon = { Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(28.dp)) },
            onClick = onRevisionClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeButton(
            label = LocalStrings.current.homeDictionary,
            icon = { Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(28.dp)) },
            onClick = onDictionnaireClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeButton(
            label = LocalStrings.current.homeConjugation,
            icon = { Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(28.dp)) },
            onClick = onConjugaisonsClick
        )
    }
}

@Composable
private fun HomeButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.size(12.dp))
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}
