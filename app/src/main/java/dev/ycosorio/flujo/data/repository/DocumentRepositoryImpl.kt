package dev.ycosorio.flujo.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import com.google.firebase.firestore.snapshots
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : DocumentRepository {

    private val templatesCollection = firestore.collection("document_templates")
    private val assignmentsCollection = firestore.collection("document_assignments")

    private val storageReference = storage.reference

    override suspend fun uploadTemplate(title: String, fileUri: Uri): Resource<Unit> {
        return try {
            Log.d("DocumentRepository", "📤 Iniciando subida: $title")
            Log.d("DocumentRepository", "URI: $fileUri")

            // 1. Crear nombre único para el archivo
            val timestamp = System.currentTimeMillis()
            val originalName = fileUri.lastPathSegment?.replace(" ", "_") ?: "document.pdf"
            val fileName = "template_${timestamp}_$originalName"

            Log.d("DocumentRepository", "📁 Nombre de archivo: $fileName")

            // 2. Referencia en Storage
            val storageRef = storage.reference.child("document_templates/$fileName")

            // 3. Subir el archivo
            Log.d("DocumentRepository", "⬆️ Subiendo archivo...")
            val uploadTask = storageRef.putFile(fileUri).await()
            Log.d("DocumentRepository", "✅ Archivo subido")

            // 4. Obtener URL de descarga
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Log.d("DocumentRepository", "🔗 URL obtenida: $downloadUrl")

            // 5. Crear documento en Firestore
            val newTemplateRef = templatesCollection.document()
            val templateData = mapOf(
                "id" to newTemplateRef.id,
                "title" to title,
                "fileUrl" to downloadUrl
            )

            Log.d("DocumentRepository", "💾 Guardando en Firestore...")
            newTemplateRef.set(templateData).await()
            Log.d("DocumentRepository", "✅ Plantilla guardada exitosamente")

            Resource.Success(Unit)

        } catch (e: Exception) {
            Log.e("DocumentRepository", "❌ Error al subir plantilla", e)
            Resource.Error(e.localizedMessage ?: "Error al subir la plantilla.")
        }
    }

    override fun getDocumentTemplates(): Flow<Resource<List<DocumentTemplate>>> = callbackFlow {
        val query = templatesCollection.orderBy("title")

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Manejar PERMISSION_DENIED sin propagar el error (ocurre al cerrar sesión)
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))
                    close()  // Cerrar sin propagar el error
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar plantillas."))
                    close(error)
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
                // Manejar PERMISSION_DENIED sin propagar el error (ocurre al cerrar sesión)
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))
                    close()  // Cerrar sin propagar el error
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar documentos pendientes."))
                    close(error)
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
            Log.d("DocumentRepository", "💾 Marcando documento como firmado: $assignmentId")
            Log.d("DocumentRepository", "🔗 URL de firma: $signatureUrl")

            val updates = mapOf(
                "status" to DocumentStatus.FIRMADO.name,
                "signatureUrl" to signatureUrl,
                "signedDate" to Date()
            )
            Log.d("DocumentRepository", "📝 Updates a aplicar: $updates")
            assignmentsCollection.document(assignmentId).update(updates).await()
            Log.d("DocumentRepository", "✅ Documento actualizado exitosamente")

            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e("DocumentRepository", "❌ FirebaseFirestoreException al guardar la firma", e)
            Resource.Error(e.localizedMessage ?: "Error al guardar la firma.")
        } catch (e: Exception) {
            Log.e("DocumentRepository", "❌ Exception al guardar la firma", e)
            Resource.Error(e.localizedMessage ?: "Error inesperado.")
        }
    }

    override fun getAllAssignments(): Flow<Resource<List<DocumentAssignment>>> = callbackFlow {
        val query = assignmentsCollection.orderBy("assignedDate", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Manejar PERMISSION_DENIED sin propagar el error (ocurre al cerrar sesión)
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))
                    close()  // Cerrar sin propagar el error
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar asignaciones."))
                    close(error)
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
                    close()
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al obtener documentos asignados"))
                    close(error)
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
                    close()
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar documentos firmados."))
                    close(error)
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