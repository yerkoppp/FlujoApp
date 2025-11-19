package dev.ycosorio.flujo.data.repository

import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dev.ycosorio.flujo.domain.model.ExpenseReport
import dev.ycosorio.flujo.domain.model.ExpenseReportStatus
import dev.ycosorio.flujo.domain.repository.ExpenseRepository
import dev.ycosorio.flujo.utils.FirestoreConstants
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * Implementación del repositorio de reportes de gastos utilizando Firebase Firestore y Firebase Storage.
 *
 * @property firestore La instancia de FirebaseFirestore para gestionar los datos de reportes de gastos.
 * @property storage La instancia de FirebaseStorage para gestionar las imágenes asociadas a los reportes.
 */
class ExpenseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ExpenseRepository {

    /** Referencia a la colección de reportes de gastos en Firestore. */
    private val expenseReportsCol = firestore.collection(FirestoreConstants.EXPENSE_COLLECTION)

    /** Obtiene los reportes de gastos para un trabajador específico. */
    override fun getExpenseReportsForWorker(workerId: String): Flow<Resource<List<ExpenseReport>>> =
        callbackFlow {
            trySend(Resource.Loading())

            val listener = expenseReportsCol
                .whereEqualTo("workerId", workerId)
                .orderBy("createdDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message ?: "Error al cargar rendiciones"))
                        return@addSnapshotListener
                    }

                    try {
                        val reports = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(ExpenseReport::class.java)?.copy(id = doc.id)
                        } ?: emptyList()
                        trySend(Resource.Success(reports))
                    } catch (e: Exception) {
                        trySend(Resource.Error(e.message ?: "Error al cargar rendiciones"))
                    }
                }

            awaitClose { listener.remove() }  // ✅ Cancelar el listener
        }

    /** Obtiene todos los reportes de gastos que no están en estado Borrador. */
    override fun getAllExpenseReports(): Flow<Resource<List<ExpenseReport>>> =
        callbackFlow {
            trySend(Resource.Loading())

            val listener = expenseReportsCol
                .whereNotEqualTo("status", ExpenseReportStatus.DRAFT.name)
                .orderBy("status")
                .orderBy("createdDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message ?: "Error al cargar rendiciones"))
                        return@addSnapshotListener
                    }

                    try {
                        val reports = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(ExpenseReport::class.java)?.copy(id = doc.id)
                        } ?: emptyList()
                        trySend(Resource.Success(reports))
                    } catch (e: Exception) {
                        trySend(Resource.Error(e.message ?: "Error al cargar rendiciones"))
                    }
                }

            awaitClose { listener.remove() }
        }

    /** Obtiene un reporte de gastos por su ID.*/
    override fun getExpenseReportById(reportId: String): Flow<Resource<ExpenseReport>> =
        callbackFlow {
            trySend(Resource.Loading())

            val listener = expenseReportsCol.document(reportId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message ?: "Error al cargar rendición"))
                        return@addSnapshotListener
                    }

                    try {
                        val report = snapshot?.toObject(ExpenseReport::class.java)?.copy(id = snapshot.id)
                        if (report != null) {
                            trySend(Resource.Success(report))
                        } else {
                            trySend(Resource.Error("Rendición no encontrada"))
                        }
                    } catch (e: Exception) {
                        trySend(Resource.Error(e.message ?: "Error al cargar rendición"))
                    }
                }

            awaitClose { listener.remove() }
        }

    /** Guarda o actualiza un reporte de gastos en Firestore. */
    override suspend fun saveExpenseReport(report: ExpenseReport): Resource<String> {
        return try {
            val docRef = if (report.id.isEmpty()) {
                expenseReportsCol.document()
            } else {
                expenseReportsCol.document(report.id)
            }

            val reportToSave = report.copy(id = docRef.id)
            docRef.set(reportToSave).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al guardar rendición")
        }
    }

    /** Sube una imagen asociada a un reporte de gastos a Firebase Storage. */
    override suspend fun uploadExpenseImage(workerId: String, imageUri: Uri): Resource<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference
                .child("expense_images/$workerId/$fileName")

            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al subir imagen")
        }
    }

    /** Actualiza el estado de un reporte de gastos. */
    override suspend fun updateExpenseReportStatus(
        reportId: String,
        status: ExpenseReportStatus,
        adminNotes: String?
    ): Resource<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name
            )

            when (status) {
                ExpenseReportStatus.SUBMITTED -> updates["submittedDate"] = Date()
                ExpenseReportStatus.APPROVED, ExpenseReportStatus.REJECTED -> {
                    updates["reviewedDate"] = Date()
                    adminNotes?.let { updates["adminNotes"] = it }
                }
                else -> {}
            }

            expenseReportsCol.document(reportId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al actualizar estado")
        }
    }

    /** Elimina un reporte de gastos por su ID. */
    override suspend fun deleteExpenseReport(reportId: String): Resource<Unit> {
        return try {
            expenseReportsCol.document(reportId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al eliminar rendición")
        }
    }

    /** Obtiene los reportes de gastos para un trabajador específico en formato paginado. */
    override fun getExpenseReportsForWorkerPaged(userId: String): Flow<PagingData<ExpenseReport>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                ExpenseReportPagingSource(
                    expenseReportsCollection = expenseReportsCol,
                    userId = userId
                )
            }
        ).flow
    }
}