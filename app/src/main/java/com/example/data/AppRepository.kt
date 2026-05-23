package com.example.data

import com.example.data.local.ChatDao
import com.example.data.local.MessageEntity
import com.example.data.local.SessionEntity
import com.example.data.remote.ApiClient
import com.example.data.remote.ChatMessageDto
import com.example.data.remote.ChatRequestDto
import kotlinx.coroutines.flow.Flow
import java.net.URLEncoder

class AppRepository(
    private val chatDao: ChatDao
) {
    fun getSessions(type: Int): Flow<List<SessionEntity>> = chatDao.getSessions(type)
    
    fun getMessages(sessionId: Int): Flow<List<MessageEntity>> = chatDao.getMessagesForSession(sessionId)

    suspend fun createSession(title: String, type: Int): Int {
        return chatDao.insertSession(SessionEntity(title = title, type = type)).toInt()
    }

    suspend fun deleteSession(sessionId: Int) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun clearAllSessions(type: Int) {
        chatDao.clearSessions(type)
    }

    suspend fun updateSessionTitle(sessionId: Int, title: String) {
        chatDao.updateSessionTitle(sessionId, title)
    }

    suspend fun sendMessageSync(content: String, model: String, systemPrompt: String): String {
        // Prepare request
        val request = ChatRequestDto(
            model = model,
            messages = listOf(
                ChatMessageDto(role = "system", content = systemPrompt),
                ChatMessageDto(role = "user", content = content)
            )
        )
        val response = ApiClient.pollinationsApi.generateText(request)
        return response.choices.firstOrNull()?.message?.content ?: "Empty response"
    }

    suspend fun saveUserMessage(sessionId: Int, content: String) {
        chatDao.insertMessage(MessageEntity(sessionId = sessionId, content = content, isUser = true, type = 0))
    }

    suspend fun saveBotTextMessage(sessionId: Int, content: String) {
        chatDao.insertMessage(MessageEntity(sessionId = sessionId, content = content, isUser = false, type = 0))
    }

    suspend fun saveBotImageMessage(sessionId: Int, imageUrl: String) {
        chatDao.insertMessage(MessageEntity(sessionId = sessionId, content = imageUrl, isUser = false, type = 1))
    }

    fun buildImageUrl(prompt: String, model: String): String {
        val encodedPrompt = URLEncoder.encode(prompt, "UTF-8")
        return "https://pollinations-proxy.spritenguyen.workers.dev/prompt/$encodedPrompt?model=$model&width=1024&height=1024&nologo=true"
    }
}
