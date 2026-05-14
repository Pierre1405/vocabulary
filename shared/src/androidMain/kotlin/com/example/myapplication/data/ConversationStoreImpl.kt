package com.example.myapplication.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationStoreImpl(context: Context) : ConversationStore {

    private val prefs = context.getSharedPreferences("conversation_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun getProviderType(): AiProviderType =
        runCatching { AiProviderType.valueOf(prefs.getString("provider", null) ?: "") }
            .getOrDefault(AiProviderType.CLAUDE)

    override fun getApiKey(): String = prefs.getString("api_key", "") ?: ""

    override fun hasApiKey(): Boolean = getApiKey().isNotEmpty()

    override fun saveConfig(providerType: AiProviderType, apiKey: String) {
        prefs.edit()
            .putString("provider", providerType.name)
            .putString("api_key", apiKey)
            .apply()
    }

    override fun getProgress(): ConversationProgress {
        val raw = prefs.getString("progress", null) ?: return ConversationProgress()
        return runCatching { json.decodeFromString<ConversationProgress>(raw) }
            .getOrDefault(ConversationProgress())
    }

    override fun saveProgress(progress: ConversationProgress) {
        prefs.edit().putString("progress", json.encodeToString(progress)).apply()
    }

    override fun todayString(): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
}
