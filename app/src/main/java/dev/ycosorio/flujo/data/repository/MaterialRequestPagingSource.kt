package dev.ycosorio.flujo.data.repository

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.model.RequestItem
import kotlinx.coroutines.tasks.await

class MaterialRequestPagingSource(
    private val requestsCollection: CollectionReference,
    private val workerId: String? = null,  // null = todas las solicitudes (admin)
    private val statusFilter: RequestStatus? = null
) : PagingSource<DocumentSnapshot, MaterialRequest>() {

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, MaterialRequest> {
        return try {
            // Construir query base
            var query: Query = requestsCollection

            // Filtrar por trabajador si se especifica (para vista de trabajador)
            if (workerId != null) {
                query = query.whereEqualTo("workerId", workerId)
            }

            // Filtrar por estado si se especifica
            if (statusFilter != null) {
                query = query.whereEqualTo("status", statusFilter.name)
            }

            // Ordenar por fecha
            query = query.orderBy("requestDate", Query.Direction.DESCENDING)

            // Obtener documentos
            val snapshot: QuerySnapshot = if (params.key == null) {
                query.limit(params.loadSize.toLong()).get().await()
            } else {
                query.startAfter(params.key!!).limit(params.loadSize.toLong()).get().await()
            }

            // Convertir a objetos MaterialRequest
            val requests = snapshot.documents.mapNotNull { doc ->
                doc.toMaterialRequest()
            }

            Log.d("MaterialRequestPaging", "📄 Cargadas ${requests.size} solicitudes")

            LoadResult.Page(
                data = requests,
                prevKey = null,
                nextKey = if (snapshot.documents.isEmpty()) null else snapshot.documents.last()
            )

        } catch (e: Exception) {
            Log.e("MaterialRequestPaging", "❌ Error al cargar solicitudes", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, MaterialRequest>): DocumentSnapshot? {
        return null
    }

    private fun DocumentSnapshot.toMaterialRequest(): MaterialRequest? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val itemsList = get("items") as? List<Map<String, Any>> ?: emptyList()

            val items = itemsList.map { itemMap ->
                RequestItem(
                    materialId = itemMap["materialId"] as? String ?: "",
                    materialName = itemMap["materialName"] as? String ?: "",
                    quantity = (itemMap["quantity"] as? Long)?.toInt() ?: 0
                )
            }

            MaterialRequest(
                id = getString("id") ?: id,
                workerId = getString("workerId") ?: "",
                workerName = getString("workerName") ?: "",
                warehouseId = getString("warehouseId") ?: "",
                items = items,
                status = getString("status")?.let { RequestStatus.valueOf(it) } ?: RequestStatus.PENDIENTE,
                requestDate = getDate("requestDate") ?: java.util.Date(),
                approvalDate = getDate("approvalDate"),
                rejectionDate = getDate("rejectionDate"),
                deliveryDate = getDate("deliveryDate"),
                adminNotes = getString("adminNotes")
            )
        } catch (e: Exception) {
            Log.e("MaterialRequestPaging", "Error al parsear solicitud ${this.id}", e)
            null
        }
    }
}