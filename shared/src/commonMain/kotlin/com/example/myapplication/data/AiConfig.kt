package com.example.myapplication.data

import kotlinx.serialization.Serializable

enum class AiProviderType { CLAUDE, GEMINI, OPENAI }

@Serializable
data class ChatMessage(val role: String, val content: String)
