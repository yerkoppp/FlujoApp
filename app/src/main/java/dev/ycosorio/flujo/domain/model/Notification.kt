package dev.ycosorio.flujo.domain.model

import com.google.firebase.Timestamp

/**
 * Representa una notificación dirigida a un usuario específico dentro de la aplicación.
 *
 * Esta clase encapsula toda la información necesaria para mostrar una notificación,
 * incluyendo su contenido, estado y metadatos para acciones posteriores.
 *
 * @property id El identificador único de la notificación.
 * @property userId El ID del usuario que recibe la notificación.
 * @property title El título de la notificación, visible para el usuario.
 * @property body El cuerpo o mensaje principal de la notificación.
 * @property timestamp La fecha y hora en que se generó o recibió la notificación.
 * @property isRead Un indicador booleano que marca si el usuario ya ha leído la notificación.
 * @property type Una cadena que categoriza la notificación (ej. "message", "request", "document") para gestionarla de forma diferente en la app.
 * @property data Un mapa de datos adicionales que puede contener información útil, como IDs de documentos o solicitudes para navegar a la pantalla correcta al pulsar la notificación.
 */
data class Notification(
    val id: String = "",
    val userId: String,
    val title: String,
    val body: String,
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val type: String = "message",
    val data: Map<String, String> = emptyMap()
)