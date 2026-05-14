package com.example.myapplication.data

interface AiService {
    suspend fun complete(
        providerType: AiProviderType,
        apiKey: String,
        systemPrompt: String,
        messages: List<ChatMessage>
    ): String
}
