package dev.ycosorio.flujo.domain.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val userId: String,           // Usuario que recibe la notificación
    val title: String,             // Título de la notificación
    val body: String,              // Cuerpo del mensaje
    val timestamp: Timestamp = Timestamp.now(),  // Cuándo se recibió
    val isRead: Boolean = false,   // Si ya fue leída
    val type: String = "message",  // Tipo: message, request, document, etc.
    val data: Map<String, String> = emptyMap() // Datos adicionales
)