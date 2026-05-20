package com.example.myapplication.data

interface ConversationStore {
    fun getProviderType(): AiProviderType
    fun getApiKey(): String
    fun hasApiKey(): Boolean
    fun saveConfig(providerType: AiProviderType, apiKey: String)
    fun getLevel(): String
    fun saveLevel(level: String)
    fun getProgress(): ConversationProgress
    fun saveProgress(progress: ConversationProgress)
    fun todayString(): String
}
