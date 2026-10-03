package com.evgenykon.travelguide.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelsResponse(
    val data: List<ModelDto> = emptyList()
)

@Serializable
data class ModelDto(
    val id: String,
    val name: String? = null,
    @SerialName("context_length") val contextLength: Int? = null
) {
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: id
}

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 500
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    val message: ChatMessage? = null
)

@Serializable
data class ExchangeCodeRequest(
    val code: String,
    @SerialName("code_verifier") val codeVerifier: String,
    @SerialName("code_challenge_method") val codeChallengeMethod: String = "S256"
)

@Serializable
data class ExchangeCodeResponse(
    val key: String,
    @SerialName("user_id") val userId: String? = null
)

@Serializable
data class IamTokenRequest(
    val jwt: String? = null
)

@Serializable
data class IamTokenResponse(
    val iamToken: String,
    val expiresAt: String
)

@Serializable
data class VoicesResponse(
    val voices: List<VoiceDto> = emptyList()
)

@Serializable
data class ElevationResponse(
    val elevation: List<Double> = emptyList()
)

@Serializable
data class VoiceDto(
    val name: String,
    val gender: String? = null,
    val languages: List<String> = emptyList(),
    @SerialName("sampleRateHertz") val sampleRateHertz: String? = null
) {
    val displayName: String
        get() {
            val genderLabel = when (gender?.uppercase()) {
                "FEMALE" -> "жен."
                "MALE" -> "муж."
                else -> null
            }
            return if (genderLabel != null) "$name ($genderLabel)" else name
        }
}
