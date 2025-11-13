package dev.ycosorio.flujo.domain.model

import java.util.Date

data class Message(
    val id: String = "",
    val senderId: String,
    val senderName: String,
    val recipientIds: List<String>, // Lista de IDs de destinatarios
    val subject: String,
    val content: String,
    val timestamp: Date = Date(),
    val isRead: Map<String, Boolean> = emptyMap(), // userId -> isRead
    val isToAllWorkers: Boolean = false
)