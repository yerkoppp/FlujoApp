package dev.ycosorio.flujo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dev.ycosorio.flujo.domain.model.DocumentAssignment
import dev.ycosorio.flujo.domain.model.DocumentStatus
import dev.ycosorio.flujo.domain.model.DocumentTemplate
import dev.ycosorio.flujo.domain.repository.DocumentRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementación del DocumentRepository que se comunica con Firebase Firestore y Storage.
 *
 * @property firestore Instancia de FirebaseFirestore inyectada.
 * // A futuro inyectaremos FirebaseStorage aquí también.
 */
class DocumentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
    // private val storage: FirebaseStorage // Para guardar las firmas
) : DocumentRepository {

    private val templatesCollection = firestore.collection("document_templates")
    private val assignmentsCollection = firestore.collection("document_assignments")

    override fun getDocumentTemplates(): Flow<Resource<List<DocumentTemplate>>> {
        TODO("Not yet implemented")
    }

    override suspend fun assignDocument(template: DocumentTemplate, workerIds: List<String>): Resource<Unit> {
        TODO("Not yet implemented")
    }

    override fun getPendingAssignmentsForWorker(workerId: String): Flow<Resource<List<DocumentAssignment>>> {
        TODO("Not yet implemented")
    }

    override suspend fun markDocumentAsSigned(assignmentId: String, signatureUrl: String): Resource<Unit> {
        TODO("Not yet implemented")
    }

    override fun getAllAssignments(): Flow<Resource<List<DocumentAssignment>>> {
        TODO("Not yet implemented")
    }
}