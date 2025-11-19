package dev.ycosorio.flujo.domain.model

import java.util.Date

/**
 * Representa un mensaje en el sistema de comunicación interna de la aplicación.
 *
 * Esta clase contiene todos los detalles de un mensaje, incluyendo quién lo envía,
 * quiénes son los destinatarios, el contenido y su estado de lectura.
 *
 * @property id El identificador único del mensaje. Generalmente se genera automáticamente.
 * @property senderId El ID del usuario que envió el mensaje.
 * @property senderName El nombre del remitente para mostrar en la interfaz.
 * @property recipientIds Una lista con los IDs de todos los usuarios destinatarios del mensaje.
 * @property subject El asunto o título del mensaje.
 * @property content El cuerpo principal del mensaje.
 * @property timestamp La fecha y hora en que se creó o envió el mensaje.
 * @property isRead Un mapa que rastrea el estado de lectura para cada destinatario. La clave es el ID del usuario y el valor es `true` si lo ha leído.
 * @property isToAllWorkers Un indicador booleano que, si es `true`, significa que el mensaje es un anuncio general para todos los trabajadores, ignorando la lista `recipientIds`.
 */
data class Message(
    val id: String = "",
    val senderId: String,
    val senderName: String,
    val recipientIds: List<String>,
    val subject: String,
    val content: String,
    val timestamp: Date = Date(),
    val isRead: Map<String, Boolean> = emptyMap(),
    val isToAllWorkers: Boolean = false
)