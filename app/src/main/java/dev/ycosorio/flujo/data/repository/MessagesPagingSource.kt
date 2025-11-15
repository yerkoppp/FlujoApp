package dev.ycosorio.flujo.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import dev.ycosorio.flujo.domain.model.Message
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class MessagesPagingSource(
    private val messagesCollection: CollectionReference,
    private val userId: String,
    private val isReceived: Boolean
) : PagingSource<DocumentSnapshot, Message>() {

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Message> {
        return try {
            // Construir la query según si son mensajes recibidos o enviados
            val query = if (isReceived) {
                messagesCollection
                    .whereArrayContains("recipientIds", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
            } else {
                messagesCollection
                    .whereEqualTo("senderId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
            }

            // Obtener documentos (primera página o página siguiente)
            val snapshot: QuerySnapshot = if (params.key == null) {
                // Primera página
                query.limit(params.loadSize.toLong()).get().await()
            } else {
                // Páginas siguientes (empezar después del último documento)
                query.startAfter(params.key!!).limit(params.loadSize.toLong()).get().await()
            }

            // Convertir documentos a objetos Message
            val messages = snapshot.documents.mapNotNull { doc ->
                doc.toMessage()
            }

            Timber.tag("MessagesPaging").d("📄 Cargados ${messages.size} mensajes")

            // Retornar resultado
            LoadResult.Page(
                data = messages,
                prevKey = null, // No soportamos scroll hacia arriba (opcional)
                nextKey = if (snapshot.documents.isEmpty()) null else snapshot.documents.last()
            )

        } catch (e: Exception) {
            Timber.tag("MessagesPaging").e(e, "❌ Error al cargar mensajes")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Message>): DocumentSnapshot? {
        // Retornar null para siempre empezar desde el principio al refrescar
        return null
    }

    // Función helper para convertir DocumentSnapshot a Message
    private fun DocumentSnapshot.toMessage(): Message? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val recipientIds = get("recipientIds") as? List<String> ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val isReadMap = get("isRead") as? Map<String, Boolean> ?: emptyMap()

            Message(
                id = getString("id") ?: id,
                senderId = getString("senderId") ?: "",
                senderName = getString("senderName") ?: "",
                recipientIds = recipientIds,
                isToAllWorkers = getBoolean("isToAllWorkers") ?: false,
                subject = getString("subject") ?: "",
                content = getString("content") ?: "",
                timestamp = getDate("timestamp") ?: java.util.Date(),
                isRead = isReadMap
            )
        } catch (e: Exception) {
            Timber.tag("MessagesPaging").e(e, "Error al parsear mensaje ${this.id}")
            null
        }
    }
}