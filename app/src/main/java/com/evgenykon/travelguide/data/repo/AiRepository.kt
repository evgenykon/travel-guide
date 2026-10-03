package com.evgenykon.travelguide.data.repo

import com.evgenykon.travelguide.data.prefs.SecureStore
import com.evgenykon.travelguide.network.ChatMessage
import com.evgenykon.travelguide.network.ChatRequest
import com.evgenykon.travelguide.network.ModelDto
import com.evgenykon.travelguide.network.OpenRouterApi
import java.util.Locale

class AiRepository(
    private val secureStore: SecureStore,
    private val api: OpenRouterApi
) {

    val isConnected: Boolean
        get() = !secureStore.openRouterKey.isNullOrBlank()

    fun saveKey(key: String) {
        secureStore.openRouterKey = key.trim()
    }

    fun disconnect() {
        secureStore.clearOpenRouter()
    }

    suspend fun listModels(): List<ModelDto> {
        val key = secureStore.openRouterKey ?: error("OpenRouter не подключён")
        return api.models(authHeader(key)).data
            .filter { it.id.isNotBlank() }
            .sortedBy { it.id }
    }

    suspend fun generateDescription(
        model: String,
        name: String,
        lat: Double,
        lng: Double,
        hint: String
    ): String {
        val key = secureStore.openRouterKey ?: error("OpenRouter не подключён")
        val prompt = buildString {
            appendLine("Составь описание точки на карте для путешественника.")
            if (name.isNotBlank()) appendLine("Название: $name")
            appendLine("Координаты: ${String.format(Locale.US, "%.5f, %.5f", lat, lng)}")
            if (hint.isNotBlank()) appendLine("Заметка пользователя: $hint")
            append("Ответь 2–4 предложениями на русском языке, без заголовков и списков.")
        }
        val response = api.chatCompletions(
            authHeader(key),
            ChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage("system", "Ты — опытный экскурсовод и составитель путеводителей."),
                    ChatMessage("user", prompt)
                )
            )
        )
        return response.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            .ifBlank { error("Модель вернула пустой ответ") }
    }

    private fun authHeader(key: String) = "Bearer $key"
}
