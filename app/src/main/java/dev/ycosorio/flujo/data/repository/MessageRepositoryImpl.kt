package dev.ycosorio.flujo.data.repository

import android.util.Log
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import dev.ycosorio.flujo.domain.model.Message
import dev.ycosorio.flujo.domain.repository.MessageRepository
import dev.ycosorio.flujo.utils.FirestoreConstants
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : MessageRepository {

    private val messagesCollection = firestore.collection(FirestoreConstants.MESSAGES_COLLECTION)


    override suspend fun sendMessage(message: Message): Resource<Unit> {
        return try {
            val messageData = hashMapOf(
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "recipientIds" to message.recipientIds,
                "subject" to message.subject,
                "content" to message.content,
                "timestamp" to message.timestamp,
                "isRead" to message.recipientIds.associate { it to false },
                "isToAllWorkers" to message.isToAllWorkers
            )

            firestore.collection(FirestoreConstants.MESSAGES_COLLECTION)
                .add(messageData)
                .await()

            // Enviar notificación push a los destinatarios
            sendPushNotification(message)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al enviar mensaje")
        }
    }

    override fun getReceivedMessages(userId: String): Flow<Resource<List<Message>>> = callbackFlow {
        val listener = firestore.collection(FirestoreConstants.MESSAGES_COLLECTION)
            .whereArrayContains("recipientIds", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                Timber.d("🔍 Cargando mensajes recibidos para: $userId")
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error al obtener mensajes"))
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    Message(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        recipientIds = doc.get("recipientIds") as? List<String> ?: emptyList(),
                        subject = doc.getString("subject") ?: "",
                        content = doc.getString("content") ?: "",
                        timestamp = doc.getDate("timestamp") ?: java.util.Date(),
                        isRead = doc.get("isRead") as? Map<String, Boolean> ?: emptyMap(),
                        isToAllWorkers = doc.getBoolean("isToAllWorkers") ?: false
                    )
                } ?: emptyList()

                trySend(Resource.Success(messages))
            }

        awaitClose { listener.remove() }
    }

    override fun getSentMessages(userId: String): Flow<Resource<List<Message>>> = callbackFlow {
        val listener = firestore.collection(FirestoreConstants.MESSAGES_COLLECTION)
            .whereEqualTo("senderId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error al obtener mensajes"))
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    Message(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        recipientIds = doc.get("recipientIds") as? List<String> ?: emptyList(),
                        subject = doc.getString("subject") ?: "",
                        content = doc.getString("content") ?: "",
                        timestamp = doc.getDate("timestamp") ?: java.util.Date(),
                        isRead = doc.get("isRead") as? Map<String, Boolean> ?: emptyMap(),
                        isToAllWorkers = doc.getBoolean("isToAllWorkers") ?: false
                    )
                } ?: emptyList()

                trySend(Resource.Success(messages))
            }

        awaitClose { listener.remove() }
    }

    override suspend fun markAsRead(messageId: String, userId: String): Resource<Unit> {
        return try {
            firestore.collection(FirestoreConstants.MESSAGES_COLLECTION)
                .document(messageId)
                .update("isRead.$userId", true)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al marcar como leído")
        }
    }

    override suspend fun deleteMessage(messageId: String): Resource<Unit> {
        return try {
            firestore.collection(FirestoreConstants.MESSAGES_COLLECTION)
                .document(messageId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al eliminar mensaje")
        }
    }

    override fun getReceivedMessagesPaged(userId: String): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,              // Cargar 20 mensajes por página
                prefetchDistance = 5,       // Empezar a cargar cuando quedan 5 items
                enablePlaceholders = false  // No mostrar placeholders
            ),
            pagingSourceFactory = {
                MessagesPagingSource(
                    messagesCollection = messagesCollection,
                    userId = userId,
                    isReceived = true
                )
            }
        ).flow
    }

    override fun getSentMessagesPaged(userId: String): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                MessagesPagingSource(
                    messagesCollection = messagesCollection,
                    userId = userId,
                    isReceived = false
                )
            }
        ).flow
    }

    /**
     * Envía una notificación push a los destinatarios del mensaje
     */
    private fun sendPushNotification(message: Message) {
        try {
            val data = hashMapOf(
                "userIds" to message.recipientIds,
                "title" to "Nuevo mensaje de ${message.senderName}",
                "body" to message.subject,
                "data" to hashMapOf(
                    "type" to "message",
                    "senderId" to message.senderId,
                    "subject" to message.subject
                )
            )

            functions.getHttpsCallable("sendNotificationToUsers")
                .call(data)
                .addOnSuccessListener {
                    Timber.tag("MessageRepository").d("✅ Notificación enviada correctamente")
                }
                .addOnFailureListener { e ->
                    Timber.tag("MessageRepository")
                        .e(e, "❌ Error al enviar notificación: ${e.message}")
                    // No fallar el envío del mensaje si falla la notificación
                }
        } catch (e: Exception) {
            Timber.tag("MessageRepository").e(e, "❌ Error al preparar notificación")
        }
    }
}