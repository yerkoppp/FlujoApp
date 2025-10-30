package dev.ycosorio.flujo.domain.repository

import dev.ycosorio.flujo.domain.model.DocumentAssignment
import dev.ycosorio.flujo.domain.model.DocumentStatus
import dev.ycosorio.flujo.domain.model.DocumentTemplate
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Define el contrato para las operaciones de datos relacionadas con los documentos y sus asignaciones.
 */
interface DocumentRepository {

    /**
     * Obtiene todas las plantillas de documentos disponibles (para el administrador).
     */
    fun getDocumentTemplates(): Flow<Resource<List<DocumentTemplate>>>

    /**
     * Asigna un documento a uno o varios trabajadores.
     * @param template La plantilla a asignar.
     * @param workerIds La lista de IDs de los trabajadores a quienes se asignará.
     */
    suspend fun assignDocument(template: DocumentTemplate, workerIds: List<String>): Resource<Unit>

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
}