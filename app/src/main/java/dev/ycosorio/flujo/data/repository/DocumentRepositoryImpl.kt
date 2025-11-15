package dev.ycosorio.flujo.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dev.ycosorio.flujo.domain.model.DocumentAssignment
import dev.ycosorio.flujo.domain.model.DocumentStatus
import dev.ycosorio.flujo.domain.model.DocumentTemplate
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.DocumentRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : DocumentRepository {

    private val templatesCollection = firestore.collection("document_templates")
    private val assignmentsCollection = firestore.collection("document_assignments")

    override suspend fun uploadTemplate(title: String, fileUri: Uri): Resource<Unit> {
        return try {
            Timber.d("📤 Iniciando subida: $title")
            Timber.d("URI: $fileUri")

            val timestamp = System.currentTimeMillis()
            val originalName = fileUri.lastPathSegment?.replace(" ", "_") ?: "document.pdf"
            val fileName = "template_${timestamp}_$originalName"
            Timber.d("📁 Nombre de archivo: $fileName")

            val storageRef = storage.reference.child("document_templates/$fileName")

            Timber.d("⬆️ Subiendo archivo...")
            val uploadTask = storageRef.putFile(fileUri).await()
            Timber.d("✅ Archivo subido")

            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Timber.d("🔗 URL obtenida: $downloadUrl")

            val newTemplateRef = templatesCollection.document()
            val templateData = mapOf(
                "id" to newTemplateRef.id,
                "title" to title,
                "fileUrl" to downloadUrl
            )

            Timber.d("💾 Guardando en Firestore...")
            newTemplateRef.set(templateData).await()
            Timber.d("✅ Plantilla guardada exitosamente")

            Resource.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "❌ Error al subir plantilla")
            Resource.Error(e.localizedMessage ?: "Error al subir la plantilla.")
        }
    }

    override fun getDocumentTemplates(): Flow<Resource<List<DocumentTemplate>>> = callbackFlow {
        val query = templatesCollection.orderBy("title")

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))

                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar plantillas."))
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val templates = snapshot.documents.mapNotNull { it.toDocumentTemplate() }
                trySend(Resource.Success(templates))
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun assignDocument(template: DocumentTemplate, workers: List<User>): Resource<Unit> {
        return try {
            val signedDocs = mutableListOf<String>()
            for (worker in workers) {
                val existing = assignmentsCollection
                    .whereEqualTo("workerId", worker.uid)
                    .whereEqualTo("templateId", template.id)
                    .whereEqualTo("status", DocumentStatus.FIRMADO.name)
                    .limit(1)
                    .get()
                    .await()

                if (!existing.isEmpty) {
                    signedDocs.add(worker.name)
                }
            }

            if (signedDocs.isNotEmpty()) {
                return Resource.Error("Los siguientes trabajadores ya firmaron este documento: ${signedDocs.joinToString(", ")}")
            }

            firestore.runBatch { batch ->
                workers.forEach { worker ->
                    val newDocRef = assignmentsCollection.document()
                    val newAssignment = DocumentAssignment(
                        id = newDocRef.id,
                        templateId = template.id,
                        documentTitle = template.title,
                        documentFileUrl = template.fileUrl,
                        workerId = worker.uid,
                        workerName = worker.name,
                        status = DocumentStatus.PENDIENTE,
                        assignedDate = Date(),
                        signedDate = null,
                        signatureUrl = null
                    )
                    batch.set(newDocRef, newAssignment.toFirestoreMap())
                }
            }.await()
            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error(e.localizedMessage ?: "Error al asignar documentos.")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error inesperado.")
        }
    }

    override fun getPendingAssignmentsForWorker(
        workerId: String
    ): Flow<Resource<List<DocumentAssignment>>> = callbackFlow {
        val query = assignmentsCollection
            .whereEqualTo("workerId", workerId)
            .whereEqualTo("status", DocumentStatus.PENDIENTE.name)
            .orderBy("assignedDate", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))

                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar documentos pendientes."))
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val assignments = snapshot.documents.mapNotNull { it.toDocumentAssignment() }
                trySend(Resource.Success(assignments))
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun markDocumentAsSigned(
        assignmentId: String, signatureUrl: String
    ): Resource<Unit> {
        return try {
            Timber.d("💾 Marcando documento como firmado: $assignmentId")
            Timber.d("🔗 URL de firma: $signatureUrl")

            val updates = mapOf(
                "status" to DocumentStatus.FIRMADO.name,
                "signatureUrl" to signatureUrl,
                "signedDate" to Date()
            )
            Timber.d("📝 Updates a aplicar: $updates")
            assignmentsCollection.document(assignmentId).update(updates).await()
            Timber.d("✅ Documento actualizado exitosamente")

            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Timber.e(e, "❌ FirebaseFirestoreException al guardar la firma")
            Resource.Error(e.localizedMessage ?: "Error al guardar la firma.")
        } catch (e: Exception) {
            Timber.e(e, "❌ Exception al guardar la firma")
            Resource.Error(e.localizedMessage ?: "Error inesperado.")
        }
    }

    override fun getAllAssignments(): Flow<Resource<List<DocumentAssignment>>> = callbackFlow {
        val query = assignmentsCollection.orderBy("assignedDate", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))                 } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar asignaciones."))
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val assignments = snapshot.documents.mapNotNull { it.toDocumentAssignment() }
                trySend(Resource.Success(assignments))
            }
        }
        awaitClose { subscription.remove() }
    }

    override fun getAssignedDocumentsForUser(workerId: String): Flow<Resource<List<DocumentAssignment>>> = callbackFlow {
        val query = assignmentsCollection.whereEqualTo("workerId", workerId)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al obtener documentos asignados"))
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val assignments = snapshot.documents.mapNotNull { it.toDocumentAssignment() }
                trySend(Resource.Success(assignments))
            }
        }
        awaitClose { subscription.remove() }
    }

    override fun getSignedDocumentsForWorker(workerId: String): Flow<Resource<List<DocumentAssignment>>> = callbackFlow {
        val query = assignmentsCollection
            .whereEqualTo("workerId", workerId)
            .whereEqualTo("status", DocumentStatus.FIRMADO.name)
            .orderBy("signedDate", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar documentos firmados."))
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val assignments = snapshot.documents.mapNotNull { it.toDocumentAssignment() }
                trySend(Resource.Success(assignments))
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun deleteTemplate(template: DocumentTemplate): Resource<Unit> {
        return try {
            Timber.d("🗑️ Eliminando plantilla: ${template.title}")

            // 1. Verificar si hay asignaciones asociadas a esta plantilla
            val assignmentsSnapshot = assignmentsCollection
                .whereEqualTo("templateId", template.id)
                .limit(1)
                .get()
                .await()

            if (!assignmentsSnapshot.isEmpty) {
                Timber.w("⚠️ No se puede eliminar: existen asignaciones asociadas")
                return Resource.Error("No se puede eliminar esta plantilla porque tiene asignaciones asociadas.")
            }

            // 2. Eliminar el archivo de Storage
            try {
                val fileRef = storage.getReferenceFromUrl(template.fileUrl)
                fileRef.delete().await()
                Timber.d("✅ Archivo eliminado de Storage")
            } catch (e: Exception) {
                Timber.w("⚠️ No se pudo eliminar el archivo de Storage: ${e.message}")
                // Continuamos incluso si falla la eliminación del archivo
            }

            // 3. Eliminar el documento de Firestore
            templatesCollection.document(template.id).delete().await()
            Timber.d("✅ Plantilla eliminada de Firestore")

            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ Error al eliminar plantilla")
            Resource.Error(e.localizedMessage ?: "Error al eliminar la plantilla.")
        }
    }
}

// --- FUNCIONES DE MAPEO ---

private fun DocumentSnapshot.toDocumentTemplate(): DocumentTemplate? {
    return try {
        DocumentTemplate(
            id = getString("id")!!,
            title = getString("title")!!,
            fileUrl = getString("fileUrl")!!
        )
    } catch (e: Exception) {
        null
    }
}

private fun DocumentSnapshot.toDocumentAssignment(): DocumentAssignment? {
    return try {
        DocumentAssignment(
            id = getString("id")!!,
            templateId = getString("templateId")!!,
            documentTitle = getString("documentTitle")!!,
            documentFileUrl = getString("documentFileUrl") ?: "",
            workerId = getString("workerId")!!,
            workerName = getString("workerName") ?: "",
            status = DocumentStatus.valueOf(getString("status")!!),
            assignedDate = getDate("assignedDate")!!,
            signedDate = getDate("signedDate"),
            signatureUrl = getString("signatureUrl")
        )
    } catch (_: Exception) {
        null
    }
}

private fun DocumentAssignment.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "templateId" to templateId,
        "documentTitle" to documentTitle,
        "documentFileUrl" to documentFileUrl,
        "workerId" to workerId,
        "workerName" to workerName,
        "status" to status.name,
        "assignedDate" to assignedDate,
        "signedDate" to signedDate,
        "signatureUrl" to signatureUrl
    )
}