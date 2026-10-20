package com.evgenykon.travelguide.data.repo

import android.content.Context
import com.evgenykon.travelguide.R
import com.evgenykon.travelguide.data.prefs.SecureStore
import com.evgenykon.travelguide.network.ChatMessage
import com.evgenykon.travelguide.network.ChatRequest
import com.evgenykon.travelguide.network.ModelDto
import com.evgenykon.travelguide.network.OpenRouterApi
import com.evgenykon.travelguide.network.responseDetails
import com.evgenykon.travelguide.util.MapPoi
import com.evgenykon.travelguide.util.localized
import retrofit2.HttpException

class AiRepository(
    private val context: Context,
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
        val key = secureStore.openRouterKey ?: error(context.getString(R.string.error_openrouter_not_connected))
        return api.models(authHeader(key)).data
            .filter { it.id.isNotBlank() }
            .sortedBy { it.id }
    }

    suspend fun generateDescription(
        model: String,
        promptTemplate: String,
        name: String,
        place: PlaceInfo?,
        lat: Double,
        lng: Double,
        hint: String,
        poi: MapPoi?
    ): String {
        val key = secureStore.openRouterKey ?: error(context.getString(R.string.error_openrouter_not_connected))
        val prompt = PromptBuilder.build(
            template = promptTemplate,
            name = name,
            place = place,
            lat = lat,
            lng = lng,
            hint = hint,
            poi = poi?.localized(context.resources.configuration.locales[0])
        )
        val response = try {
            api.chatCompletions(
                authHeader(key),
                ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", context.getString(R.string.ai_system_prompt)),
                        ChatMessage("user", prompt)
                    ),
                    temperature = 0.3,
                    maxTokens = 900
                )
            )
        } catch (e: HttpException) {
            throw IllegalStateException(
                "OpenRouter: HTTP ${e.code()}${e.responseDetails()}",
                e
            )
        }
        return response.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            .ifBlank { error(context.getString(R.string.error_model_empty)) }
    }

    private fun authHeader(key: String) = "Bearer $key"
}
