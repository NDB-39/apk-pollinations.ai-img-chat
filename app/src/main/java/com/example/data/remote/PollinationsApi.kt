package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val jsonMode: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ChatResponseDto(
    val choices: List<ChoiceDto>
)

@JsonClass(generateAdapter = true)
data class ChoiceDto(
    val message: ChatMessageDto
)

interface PollinationsApi {
    @POST("openai/chat/completions")
    suspend fun generateText(@Body request: ChatRequestDto): ChatResponseDto
}
