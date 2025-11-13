package dev.ycosorio.flujo.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dev.ycosorio.flujo.domain.model.Message
import dev.ycosorio.flujo.domain.repository.MessageRepository
import dev.ycosorio.flujo.utils.FirestoreConstants
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MessageRepository {

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
                Log.d("MessagesRepositoryImpl", "🔍 Cargando mensajes recibidos para: $userId")
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
}