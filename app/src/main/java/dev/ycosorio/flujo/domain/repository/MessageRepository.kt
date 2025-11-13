package dev.ycosorio.flujo.domain.repository

import dev.ycosorio.flujo.domain.model.Message
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun sendMessage(message: Message): Resource<Unit>
    fun getReceivedMessages(userId: String): Flow<Resource<List<Message>>>
    fun getSentMessages(userId: String): Flow<Resource<List<Message>>>
    suspend fun markAsRead(messageId: String, userId: String): Resource<Unit>
    suspend fun deleteMessage(messageId: String): Resource<Unit>
}