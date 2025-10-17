package dev.ycosorio.flujo.domain.repository

import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow
import com.google.firebase.firestore.Query

/**
 * Define el contrato para las operaciones de datos relacionadas con el inventario.
 */
interface InventoryRepository {

    /**
     * Obtiene las solicitudes de materiales en tiempo real con opciones de orden y filtro.
     * @param orderBy El campo por el cual ordenar (ej: "requestDate").
     * @param direction La dirección del orden (Ascendente o Descendente).
     * @param statusFilter Un estado específico para filtrar, o null para no filtrar.
     * @return Un Flow que emite la lista de solicitudes.
     */
    fun getMaterialRequests(
        orderBy: String = "requestDate",
        direction: Query.Direction = Query.Direction.DESCENDING,
        statusFilter: RequestStatus? = null
    ): Flow<Resource<List<MaterialRequest>>>

    /**
     * Crea una nueva solicitud de materiales.
     */
    suspend fun createMaterialRequest(request: MaterialRequest): Resource<Unit>

    /**
     * Actualiza el estado de una solicitud existente.
     * Usado por el administrador para aprobar o rechazar.
     */
    suspend fun updateRequestStatus(requestId: String, newStatus: RequestStatus): Resource<Unit>

    /**
     * Obtiene las solicitudes de materiales para un trabajador específico en tiempo real.
     * @param workerId El ID del trabajador del que se quieren obtener las solicitudes.
     */
    fun getRequestsForWorker(workerId: String): Flow<Resource<List<MaterialRequest>>>
}