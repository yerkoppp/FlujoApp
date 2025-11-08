package dev.ycosorio.flujo.domain.repository

import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow
import com.google.firebase.firestore.Query
import dev.ycosorio.flujo.domain.model.Material
import dev.ycosorio.flujo.domain.model.StockItem
import dev.ycosorio.flujo.domain.model.Warehouse
import dev.ycosorio.flujo.domain.model.WarehouseType

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
    suspend fun updateRequestStatus(
        requestId: String,
        newStatus: RequestStatus
    ): Resource<Unit>

    /**
     * Obtiene las solicitudes de materiales para un trabajador específico en tiempo real.
     * @param workerId El ID del trabajador del que se quieren obtener las solicitudes.
     */
    fun getRequestsForWorker(workerId: String): Flow<Resource<List<MaterialRequest>>>

    // --- Funciones de Gestión de Admin ---
    /**
     * Obtiene todas las solicitudes de materiales en tiempo real (sin filtrar por trabajador).
     */
    fun getAllRequests(): Flow<Resource<List<MaterialRequest>>>

    /**
     * Actualiza el estado de una solicitud existente con notas del administrador.
     * Usado por el administrador para aprobar o rechazar.
     */
    suspend fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
        adminNotes: String?
    ): Resource<Unit>

    // --- FUNCIONES DE GESTIÓN DE BODEGAS ---

    /**
     * Obtiene una lista (en tiempo real) de todas las bodegas.
     */
    fun getWarehouses(): Flow<Resource<List<Warehouse>>>

    /**
     * Crea una nueva bodega (fija o móvil).
     */
    suspend fun createWarehouse(name: String, type: WarehouseType): Resource<Unit>


    // --- FUNCIONES DE GESTIÓN DE MATERIALES (Definiciones) ---

    /**
     * Obtiene la lista maestra (en tiempo real) de todas las definiciones de materiales.
     */
    fun getMaterials(): Flow<Resource<List<Material>>>

    /**
     * Crea una nueva definición de material en la colección 'materials'.
     */
    suspend fun createMaterialDefinition(name: String, description: String): Resource<Unit>


    // --- FUNCIONES DE GESTIÓN DE STOCK (StockItem) ---

    /**
     * Obtiene el inventario (en tiempo real) de una bodega específica.
     * @param warehouseId El ID de la bodega (fija o móvil) a consultar.
     */
    fun getStockForWarehouse(warehouseId: String): Flow<Resource<List<StockItem>>>

    /**
     * Agrega stock a una bodega específica.
     * Si el material no existe en esa bodega, lo crea.
     * Si ya existe, incrementa la cantidad.
     */
    suspend fun addStockToWarehouse(
        warehouseId: String,
        material: Material,
        quantity: Int
    ): Resource<Unit>

    /**
     * Transfiere una cantidad de un material de una bodega a otra.
     * Esta función debe ser transaccional.
     *
     * @param fromWarehouseId Bodega de origen (ej. BODEGA_CENTRAL).
     * @param toWarehouseId Bodega de destino (ej. el ID de una bodega móvil).
     * @param material Objeto Material completo, necesario para crear el StockItem
     * en la bodega de destino si no existe.
     * @param quantityToTransfer La cantidad a mover.
     */
    suspend fun transferStock(
        fromWarehouseId: String,
        toWarehouseId: String,
        material: Material,
        quantityToTransfer: Int
    ): Resource<Unit>

    /**
     * Marca una solicitud como ENTREGADA y transfiere el stock automáticamente.
     * Esta operación es atómica (todo o nada).
     *
     * @param requestId ID de la solicitud
     * @param centralWarehouseId ID de la bodega central (origen)
     * @param adminNotes Notas opcionales del administrador
     * @return Resource indicando éxito o error
     */
    suspend fun deliverMaterialRequest(
        requestId: String,
        centralWarehouseId: String,
        adminNotes: String? = null
    ): Resource<Unit>
}