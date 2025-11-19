package dev.ycosorio.flujo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dev.ycosorio.flujo.domain.model.Notification
import dev.ycosorio.flujo.domain.repository.NotificationRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementación del repositorio de notificaciones utilizando Firebase Firestore.
 *
 * @property firestore La instancia de FirebaseFirestore para gestionar las notificaciones.
 */
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    /**
     * Obtiene un flujo de notificaciones para un usuario específico.
     *
     * @param userId El ID del usuario cuyas notificaciones se van a obtener.
     * @return Un flujo que emite recursos que contienen listas de notificaciones o errores.
     */
    override fun getUserNotifications(userId: String): Flow<Resource<List<Notification>>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error al obtener notificaciones"))
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Notification(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            title = doc.getString("title") ?: "",
                            body = doc.getString("body") ?: "",
                            timestamp = doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now(),
                            isRead = doc.getBoolean("isRead") ?: false,
                            type = doc.getString("type") ?: "message",
                            data = doc.get("data") as? Map<String, String> ?: emptyMap()
                        )
                    } catch (e: Exception) {
                        Timber.tag("NotificationRepository").e(e, "Error al mapear notificación")
                        null
                    }
                } ?: emptyList()

                trySend(Resource.Success(notifications))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Marca una notificación como leída.
     *
     * @param notificationId El ID de la notificación a marcar como leída.
     * @return Un recurso que indica el éxito o el error de la operación.
     */
    override suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al marcar como leída")
        }
    }

    /**
     * Elimina una notificación.
     *
     * @param notificationId El ID de la notificación a eliminar.
     * @return Un recurso que indica el éxito o el error de la operación.
     */
    override suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al eliminar notificación")
        }
    }
}