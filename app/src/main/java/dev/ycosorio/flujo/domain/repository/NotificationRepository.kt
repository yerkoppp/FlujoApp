package dev.ycosorio.flujo.domain.repository

import dev.ycosorio.flujo.domain.model.Notification
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    /**
     * Obtiene las notificaciones de un usuario en tiempo real
     */
    fun getUserNotifications(userId: String): Flow<Resource<List<Notification>>>

    /**
     * Marca una notificación como leída
     */
    suspend fun markAsRead(notificationId: String): Resource<Unit>

    /**
     * Elimina una notificación
     */
    suspend fun deleteNotification(notificationId: String): Resource<Unit>
}