package com.evgenykon.travelguide.data.repo

import com.evgenykon.travelguide.data.prefs.SecureStore
import com.evgenykon.travelguide.network.ChatMessage
import com.evgenykon.travelguide.network.ChatRequest
import com.evgenykon.travelguide.network.ModelDto
import com.evgenykon.travelguide.network.OpenRouterApi
import com.evgenykon.travelguide.util.MapPoi

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
        promptTemplate: String,
        name: String,
        address: String?,
        lat: Double,
        lng: Double,
        hint: String,
        poi: MapPoi?
    ): String {
        val key = secureStore.openRouterKey ?: error("OpenRouter не подключён")
        val prompt = PromptBuilder.build(
            template = promptTemplate,
            name = name,
            address = address,
            lat = lat,
            lng = lng,
            hint = hint,
            poi = poi
        )
        val response = api.chatCompletions(
            authHeader(key),
            ChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage("system", SYSTEM_PROMPT),
                    ChatMessage("user", prompt)
                ),
                temperature = 0.3,
                maxTokens = 900
            )
        )
        return response.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            .ifBlank { error("Модель вернула пустой ответ") }
    }

    private fun authHeader(key: String) = "Bearer $key"

    private companion object {
        const val SYSTEM_PROMPT =
            "Ты — опытный экскурсовод и составитель путеводителей. " +
                "Используешь только проверенные факты. Категорически запрещено выдумывать " +
                "имена, даты, события и связи; при отсутствии достоверных данных факт не упоминается."
    }
}
