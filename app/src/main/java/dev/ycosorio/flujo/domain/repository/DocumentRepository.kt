package dev.ycosorio.flujo.domain.repository

import dev.ycosorio.flujo.domain.model.DocumentAssignment
import dev.ycosorio.flujo.domain.model.DocumentStatus
import dev.ycosorio.flujo.domain.model.DocumentTemplate
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow
import android.net.Uri
import dev.ycosorio.flujo.domain.model.User

/**
 * Define el contrato para las operaciones de datos relacionadas con los documentos y sus asignaciones.
 */
interface DocumentRepository {

    /**
     * Sube un archivo de plantilla a Storage y crea la entrada en Firestore.
     * @param title El título que verá el usuario.
     * @param fileUri El Uri del archivo local (PDF, etc.) a subir.
     */
    suspend fun uploadTemplate(title: String, fileUri: Uri): Resource<Unit>

    /**
     * Obtiene todas las plantillas de documentos disponibles (para el administrador).
     */
    fun getDocumentTemplates(): Flow<Resource<List<DocumentTemplate>>>

    /**
     * Asigna un documento a uno o varios trabajadores.
     * @param template La plantilla a asignar.
     * @param workerIds La lista de IDs de los trabajadores a quienes se asignará.
     */
    suspend fun assignDocument(template: DocumentTemplate, workers: List<User>): Resource<Unit>

    /**
     * Obtiene todas las asignaciones de documentos pendientes para un trabajador específico.
     * @param workerId El ID del trabajador.
     */
    fun getPendingAssignmentsForWorker(workerId: String): Flow<Resource<List<DocumentAssignment>>>

    /**
     * Actualiza el estado de una asignación a FIRMADO y guarda la URL de la firma.
     * @param assignmentId El ID de la asignación a actualizar.
     * @param signatureUrl La URL de la imagen de la firma almacenada (ej: en Firebase Storage).
     */
    suspend fun markDocumentAsSigned(assignmentId: String, signatureUrl: String): Resource<Unit>

    /**
     * Obtiene todas las asignaciones de documentos (para la vista del administrador).
     * Podríamos añadir filtros y ordenamiento como hicimos con el inventario.
     */
    fun getAllAssignments(): Flow<Resource<List<DocumentAssignment>>>
    /**
     * Obtiene una lista (en tiempo real) de todos los documentos asignados a un usuario específico.
     */
    fun getAssignedDocumentsForUser(workerId: String): Flow<Resource<List<DocumentAssignment>>>

    /**
     * Obtiene todos los documentos firmados por un trabajador específico.
     */
    fun getSignedDocumentsForWorker(workerId: String): Flow<Resource<List<DocumentAssignment>>>
}