package com.example.myapplication.data

import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL

class AiServiceImpl : AiService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(
        providerType: AiProviderType,
        apiKey: String,
        systemPrompt: String,
        messages: List<ChatMessage>
    ): String = when (providerType) {
        AiProviderType.CLAUDE  -> callClaude(apiKey, systemPrompt, messages)
        AiProviderType.OPENAI  -> callOpenAi(apiKey, systemPrompt, messages)
        AiProviderType.GEMINI  -> callGemini(apiKey, systemPrompt, messages)
    }

    private fun callClaude(apiKey: String, systemPrompt: String, messages: List<ChatMessage>): String {
        val body = buildJsonObject {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 1024)
            put("system", systemPrompt)
            put("messages", buildJsonArray {
                messages.forEach { msg ->
                    add(buildJsonObject { put("role", msg.role); put("content", msg.content) })
                }
            })
        }
        val response = httpPost(
            url = "https://api.anthropic.com/v1/messages",
            headers = mapOf(
                "Content-Type" to "application/json",
                "x-api-key" to apiKey,
                "anthropic-version" to "2023-06-01"
            ),
            body = body.toString()
        )
        return json.parseToJsonElement(response)
            .jsonObject["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
    }

    private fun callOpenAi(apiKey: String, systemPrompt: String, messages: List<ChatMessage>): String {
        val allMessages = buildJsonArray {
            add(buildJsonObject { put("role", "system"); put("content", systemPrompt) })
            messages.forEach { msg ->
                add(buildJsonObject { put("role", msg.role); put("content", msg.content) })
            }
        }
        val body = buildJsonObject {
            put("model", "gpt-4o-mini")
            put("max_tokens", 1024)
            put("messages", allMessages)
        }
        val response = httpPost(
            url = "https://api.openai.com/v1/chat/completions",
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $apiKey"
            ),
            body = body.toString()
        )
        return json.parseToJsonElement(response)
            .jsonObject["choices"]!!.jsonArray[0]
            .jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
    }

    private fun callGemini(apiKey: String, systemPrompt: String, messages: List<ChatMessage>): String {
        val contents = buildJsonArray {
            messages.forEach { msg ->
                add(buildJsonObject {
                    put("role", if (msg.role == "assistant") "model" else "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", msg.content) })
                    })
                })
            }
        }
        val body = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemPrompt) })
                })
            })
            put("contents", contents)
        }
        val response = httpPost(
            url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey",
            headers = mapOf("Content-Type" to "application/json"),
            body = body.toString()
        )
        return json.parseToJsonElement(response)
            .jsonObject["candidates"]!!.jsonArray[0]
            .jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray[0]
            .jsonObject["text"]!!.jsonPrimitive.content
    }

    private fun httpPost(url: String, headers: Map<String, String>, body: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        connection.outputStream.use { it.write(body.toByteArray()) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream.use { it.readBytes().decodeToString() }
        if (code !in 200..299) throw Exception("HTTP $code: $response")
        return response
    }
}
