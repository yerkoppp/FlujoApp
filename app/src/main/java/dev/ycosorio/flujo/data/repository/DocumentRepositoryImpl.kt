package dev.ycosorio.flujo.data.repository

import android.net.Uri
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dev.ycosorio.flujo.domain.model.DocumentAssignment
import dev.ycosorio.flujo.domain.model.DocumentStatus
import dev.ycosorio.flujo.domain.model.DocumentTemplate
import dev.ycosorio.flujo.domain.repository.DocumentRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * Implementación del DocumentRepository que se comunica con Firebase Firestore y Storage.
 *
 * @property firestore Instancia de FirebaseFirestore inyectada.
 */
class DocumentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage // Para guardar las firmas
) : DocumentRepository {

    private val templatesCollection = firestore.collection("document_templates")
    private val assignmentsCollection = firestore.collection("document_assignments")

    override suspend fun uploadTemplate(title: String, fileUri: Uri): Resource<Unit> {
        return try {
            // 1. Definir dónde se guardará en Storage
            val fileName = "${UUID.randomUUID()}-${fileUri.lastPathSegment ?: "document"}"
            val storageRef = storage.reference.child("document_templates/$fileName")

            // 2. Subir el archivo
            val uploadTask = storageRef.putFile(fileUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            // 3. Crear el documento en Firestore
            val newTemplateRef = templatesCollection.document()
            val newTemplate = mapOf(
                "id" to newTemplateRef.id,
                "title" to title,
                "fileUrl" to downloadUrl
            )

            newTemplateRef.set(newTemplate).await()
            Resource.Success(Unit)

        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al subir la plantilla.")
        }
    }

    /**
     * Obtiene todas las plantillas de documentos (para el Admin).
     */
    override fun getDocumentTemplates(): Flow<Resource<List<DocumentTemplate>>> = callbackFlow{
        val query = templatesCollection.orderBy("title")

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.localizedMessage ?: "Error al cargar plantillas."))
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val templates = snapshot.documents.mapNotNull { it.toDocumentTemplate() }
                trySend(Resource.Success(templates))
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Asigna una plantilla a múltiples trabajadores usando un WriteBatch.
     */
    override suspend fun assignDocument(template: DocumentTemplate, workerIds: List<String>): Resource<Unit> {
        return try {
            firestore.runBatch { batch ->
                workerIds.forEach { workerId ->
                    // 1. Generar una referencia con ID automático
                    val newDocRef = assignmentsCollection.document()

                    // 2. Crear el objeto de asignación
                    val newAssignment = DocumentAssignment(
                        id = newDocRef.id, // Usar el ID generado
                        templateId = template.id,
                        documentTitle = template.title,
                        workerId = workerId,
                        status = DocumentStatus.PENDIENTE,
                        assignedDate = Date(),
                        signedDate = null,
                        signatureUrl = null
                    )

                    // 3. Añadir la operación de "set" al batch
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
    ): Flow<Resource<List<DocumentAssignment>>> = callbackFlow{
        val query = assignmentsCollection
            .whereEqualTo("workerId", workerId)
            .whereEqualTo("status", DocumentStatus.PENDIENTE.name) // Guardamos el enum como String
            .orderBy("assignedDate", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.localizedMessage ?: "Error al cargar documentos pendientes."))
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val assignments = snapshot.documents.mapNotNull { it.toDocumentAssignment() }
                trySend(Resource.Success(assignments))
            }
        }
        awaitClose { subscription.remove() }
    }
    /**
     * Actualiza una asignación a FIRMADO.
     */
    override suspend fun markDocumentAsSigned(
        assignmentId: String, signatureUrl: String
    ): Resource<Unit> {
        return try {
            val updates = mapOf(
                "status" to DocumentStatus.FIRMADO.name,
                "signatureUrl" to signatureUrl,
                "signedDate" to Date()
            )
            assignmentsCollection.document(assignmentId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error(e.localizedMessage ?: "Error al guardar la firma.")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error inesperado.")
        }
    }

    /**
     * Obtiene todas las asignaciones (para el Admin).
     */
    override fun getAllAssignments(): Flow<Resource<List<DocumentAssignment>>> = callbackFlow {
        val query = assignmentsCollection.orderBy("assignedDate", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.localizedMessage ?: "Error al cargar asignaciones."))
                close(error)
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

// --- FUNCIONES DE MAPEO (TRADUCTORES) ---

/**
 * Convierte un DocumentSnapshot de Firestore a nuestro modelo DocumentTemplate.
 */
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

/**
 * Convierte un DocumentSnapshot de Firestore a nuestro modelo DocumentAssignment.
 */
private fun DocumentSnapshot.toDocumentAssignment(): DocumentAssignment? {
    return try {
        DocumentAssignment(
            id = getString("id")!!,
            templateId = getString("templateId")!!,
            documentTitle = getString("documentTitle")!!,
            workerId = getString("workerId")!!,
            status = DocumentStatus.valueOf(getString("status")!!), // Convierte String a Enum
            assignedDate = getDate("assignedDate")!!,
            signedDate = getDate("signedDate"), // Puede ser nulo
            signatureUrl = getString("signatureUrl") // Puede ser nulo
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Convierte nuestro modelo DocumentAssignment a un Map que Firestore entiende.
 */
private fun DocumentAssignment.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "templateId" to templateId,
        "documentTitle" to documentTitle,
        "workerId" to workerId,
        "status" to status.name, // Convierte Enum a String
        "assignedDate" to assignedDate,
        "signedDate" to signedDate,
        "signatureUrl" to signatureUrl
    )
}