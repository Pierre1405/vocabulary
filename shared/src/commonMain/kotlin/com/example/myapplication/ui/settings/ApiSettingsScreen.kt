package com.example.myapplication.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.AiProviderType
import com.example.myapplication.data.ConversationStore
import com.example.myapplication.ui.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(
    conversationStore: ConversationStore,
    onBack: () -> Unit
) {
    val vm: ApiSettingsViewModel = viewModel { ApiSettingsViewModel(conversationStore) }
    val state by vm.uiState.collectAsState()
    var keyVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalStrings.current.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LocalStrings.current.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(LocalStrings.current.settingsProvider, style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AiProviderType.entries.forEach { provider ->
                    FilterChip(
                        selected = state.providerType == provider,
                        onClick = { vm.setProvider(provider) },
                        label = { Text(provider.label()) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(LocalStrings.current.settingsLevel, style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CONVERSATION_LEVELS.forEach { level ->
                    FilterChip(
                        selected = state.level == level,
                        onClick = { vm.setLevel(level) },
                        label = { Text(level) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(LocalStrings.current.settingsApiKey, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.apiKey,
                onValueChange = { vm.setApiKey(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(state.providerType.keyHint()) },
                singleLine = true,
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )

            Text(
                text = state.providerType.keyDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { vm.save(); onBack() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.apiKey.isNotBlank()
            ) {
                if (state.saved) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                }
                Text(LocalStrings.current.settingsSave)
            }
        }
    }
}

private fun AiProviderType.label() = when (this) {
    AiProviderType.CLAUDE -> "Claude"
    AiProviderType.OPENAI -> "ChatGPT"
    AiProviderType.GEMINI -> "Gemini"
}

private fun AiProviderType.keyHint() = when (this) {
    AiProviderType.CLAUDE -> "sk-ant-..."
    AiProviderType.OPENAI -> "sk-..."
    AiProviderType.GEMINI -> "AIza..."
}

private fun AiProviderType.keyDescription() = when (this) {
    AiProviderType.CLAUDE -> "console.anthropic.com → API Keys"
    AiProviderType.OPENAI -> "platform.openai.com → API Keys"
    AiProviderType.GEMINI -> "aistudio.google.com → Get API Key"
}
