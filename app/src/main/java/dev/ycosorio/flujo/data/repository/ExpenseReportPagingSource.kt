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

class ExpenseReportPagingSource(
    private val expenseReportsCollection: CollectionReference,
    private val userId: String
) : PagingSource<DocumentSnapshot, ExpenseReport>() {

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

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, ExpenseReport>): DocumentSnapshot? {
        return null
    }

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