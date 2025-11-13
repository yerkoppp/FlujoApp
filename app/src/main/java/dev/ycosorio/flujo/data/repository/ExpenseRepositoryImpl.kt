package dev.ycosorio.flujo.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import dev.ycosorio.flujo.domain.model.ExpenseReport
import dev.ycosorio.flujo.domain.model.ExpenseReportStatus
import dev.ycosorio.flujo.domain.repository.ExpenseRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ExpenseRepository {

    private val expenseReportsCol = firestore.collection("expense_reports")

    override fun getExpenseReportsForWorker(workerId: String): Flow<Resource<List<ExpenseReport>>> =
        callbackFlow {
            val listener = expenseReportsCol
                .whereEqualTo("workerId", workerId)
                .orderBy("createdDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    try {
                        val reports = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ExpenseReport::class.java)?.copy(id = doc.id)
                        }
                        Resource.Success(reports)
                    } catch (e: Exception) {
                        Resource.Error(e.message ?: "Error al cargar rendiciones")
                    }
                }
                .collect { trySend(it) }

            awaitClose { }
        }

    override fun getAllExpenseReports(): Flow<Resource<List<ExpenseReport>>> =
        callbackFlow {
            val listener = expenseReportsCol
                .orderBy("createdDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    try {
                        val reports = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ExpenseReport::class.java)?.copy(id = doc.id)
                        }
                        Resource.Success(reports)
                    } catch (e: Exception) {
                        Resource.Error(e.message ?: "Error al cargar rendiciones")
                    }
                }
                .collect { trySend(it) }

            awaitClose { }
        }

    override fun getExpenseReportById(reportId: String): Flow<Resource<ExpenseReport>> =
        callbackFlow {
            val listener = expenseReportsCol.document(reportId)
                .snapshots()
                .map { snapshot ->
                    try {
                        val report = snapshot.toObject(ExpenseReport::class.java)?.copy(id = snapshot.id)
                        if (report != null) {
                            Resource.Success(report)
                        } else {
                            Resource.Error("Rendición no encontrada")
                        }
                    } catch (e: Exception) {
                        Resource.Error(e.message ?: "Error al cargar rendición")
                    }
                }
                .collect { trySend(it) }

            awaitClose { }
        }

    override suspend fun saveExpenseReport(report: ExpenseReport): Resource<Unit> {
        return try {
            val docRef = if (report.id.isEmpty()) {
                expenseReportsCol.document()
            } else {
                expenseReportsCol.document(report.id)
            }

            val reportToSave = report.copy(id = docRef.id)
            docRef.set(reportToSave).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al guardar rendición")
        }
    }

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

    override suspend fun deleteExpenseReport(reportId: String): Resource<Unit> {
        return try {
            expenseReportsCol.document(reportId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al eliminar rendición")
        }
    }
}