package com.example.myapplication.data

import kotlinx.serialization.Serializable

@Serializable
data class ConversationProgress(
    val wordsLearned: List<String> = emptyList(),
    val commonErrors: List<String> = emptyList(),
    val topicsSeen: List<String> = emptyList(),
    val sessionsCount: Int = 0,
    val totalMessages: Int = 0,
    val lastSession: String = ""
)
