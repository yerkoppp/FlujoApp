package dev.ycosorio.flujo.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import dev.ycosorio.flujo.domain.model.ExpenseReport
import dev.ycosorio.flujo.domain.model.ExpenseReportStatus
import dev.ycosorio.flujo.domain.model.ExpenseItem
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date

/**
 * PagingSource para cargar reportes de gastos desde Firestore con paginación.
 *
 * @property expenseReportsCollection La referencia a la colección de reportes de gastos en Firestore.
 * @property userId El ID del usuario cuyos reportes de gastos se van a cargar.
 */
class ExpenseReportPagingSource(
    private val expenseReportsCollection: CollectionReference,
    private val userId: String
) : PagingSource<DocumentSnapshot, ExpenseReport>() {

    /**
     * Carga una página de reportes de gastos desde Firestore.
     *
     * @param params Los parámetros de carga que incluyen la clave de inicio y el tamaño de carga.
     * @return El resultado de la carga que puede ser una página de datos o un error.
     */
    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, ExpenseReport> {
        return try {
            val query = expenseReportsCollection
                .whereEqualTo("workerId", userId)
                .orderBy("createdDate", Query.Direction.DESCENDING)

            val snapshot: QuerySnapshot = if (params.key == null) {
                query.limit(params.loadSize.toLong()).get().await()
            } else {
                query.startAfter(params.key!!).limit(params.loadSize.toLong()).get().await()
            }

            val reports = snapshot.documents.mapNotNull { doc ->
                doc.toExpenseReport()
            }

            Timber.tag("ExpensePaging").d("📄 Cargados ${reports.size} reportes")

            LoadResult.Page(
                data = reports,
                prevKey = null,
                nextKey = if (snapshot.documents.isEmpty()) null else snapshot.documents.last()
            )

        } catch (e: Exception) {
            Timber.tag("ExpensePaging").e(e, "❌ Error al cargar reportes")
            LoadResult.Error(e)
        }
    }

    /**
     * Obtiene la clave de actualización para la paginación.
     *
     * @param state El estado de paginación actual.
     * @return La clave de actualización o null si no se puede determinar.
     */
    override fun getRefreshKey(state: PagingState<DocumentSnapshot, ExpenseReport>): DocumentSnapshot? {
        return null
    }

    /**
     * Convierte un DocumentSnapshot de Firestore en un objeto ExpenseReport.
     *
     * @receiver DocumentSnapshot El documento de Firestore a convertir.
     * @return Un objeto ExpenseReport o null si ocurre un error durante la conversión.
     */
    private fun DocumentSnapshot.toExpenseReport(): ExpenseReport? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val itemsList = get("items") as? List<Map<String, Any>> ?: emptyList()

            // ✅ CORREGIDO: Mapear según TU modelo de ExpenseItem
            val items = itemsList.map { itemMap ->
                ExpenseItem(
                    id = itemMap["id"] as? String ?: "",
                    imageUrl = itemMap["imageUrl"] as? String ?: "",
                    reason = itemMap["reason"] as? String ?: "",
                    documentNumber = itemMap["documentNumber"] as? String ?: "",
                    amount = (itemMap["amount"] as? Number)?.toDouble() ?: 0.0,
                    uploadDate = (itemMap["uploadDate"] as? com.google.firebase.Timestamp)?.toDate()
                        ?: Date()
                )
            }

            ExpenseReport(
                id = getString("id") ?: id,
                workerId = getString("workerId") ?: "",
                workerName = getString("workerName") ?: "",
                items = items,
                totalAmount = getDouble("totalAmount") ?: 0.0,
                status = getString("status")?.let { ExpenseReportStatus.valueOf(it) }
                    ?: ExpenseReportStatus.DRAFT,
                createdDate = getDate("createdDate") ?: Date(),
                submittedDate = getDate("submittedDate"),
                reviewedDate = getDate("reviewedDate"),
                adminNotes = getString("notes")
            )
        } catch (e: Exception) {
            Timber.tag("ExpensePaging").e(e, "Error al parsear reporte ${this.id}")
            null
        }
    }
}