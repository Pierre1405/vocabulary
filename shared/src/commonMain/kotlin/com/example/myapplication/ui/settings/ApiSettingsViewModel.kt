package com.example.myapplication.ui.settings

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.AiProviderType
import com.example.myapplication.data.ConversationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val CONVERSATION_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")

data class ApiSettingsUiState(
    val providerType: AiProviderType = AiProviderType.CLAUDE,
    val apiKey: String = "",
    val level: String = "A1",
    val saved: Boolean = false
)

class ApiSettingsViewModel(private val conversationStore: ConversationStore) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ApiSettingsUiState(
            providerType = conversationStore.getProviderType(),
            apiKey = conversationStore.getApiKey(),
            level = conversationStore.getLevel()
        )
    )
    val uiState: StateFlow<ApiSettingsUiState> = _uiState.asStateFlow()

    fun setProvider(providerType: AiProviderType) {
        _uiState.value = _uiState.value.copy(providerType = providerType, saved = false)
    }

    fun setApiKey(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key, saved = false)
    }

    fun setLevel(level: String) {
        _uiState.value = _uiState.value.copy(level = level, saved = false)
    }

    fun save() {
        conversationStore.saveConfig(_uiState.value.providerType, _uiState.value.apiKey)
        conversationStore.saveLevel(_uiState.value.level)
        _uiState.value = _uiState.value.copy(saved = true)
    }
}
