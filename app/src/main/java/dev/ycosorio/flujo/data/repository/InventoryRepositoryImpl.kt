package dev.ycosorio.flujo.data.repository

import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.common.collect.Multimaps.index
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import dev.ycosorio.flujo.domain.model.Material
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.model.StockItem
import dev.ycosorio.flujo.domain.model.Warehouse
import dev.ycosorio.flujo.domain.model.WarehouseType
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.model.ConsolidatedStock
import dev.ycosorio.flujo.domain.model.WarehouseStock
import dev.ycosorio.flujo.domain.model.RequestItem
import dev.ycosorio.flujo.utils.FirestoreConstants.MATERIALS_COLLECTION
import dev.ycosorio.flujo.utils.FirestoreConstants.MATERIAL_REQUESTS_COLLECTION
import dev.ycosorio.flujo.utils.FirestoreConstants.STOCK_SUBCOLLECTION
import dev.ycosorio.flujo.utils.FirestoreConstants.WAREHOUSES_COLLECTION
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * Implementación del InventoryRepository que se comunica con Firebase Firestore.
 *
 * @property firestore Instancia de FirebaseFirestore inyectada para la comunicación con la base de datos.
 */
class InventoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : InventoryRepository {

    /** CoroutineScope para operaciones asíncronas en segundo plano.
     * Usamos Dispatchers.IO para operaciones de E/S.
     */
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** Referencia a la colección de solicitudes de materiales en Firestore. */
    private val requestsCollection = firestore.collection(MATERIAL_REQUESTS_COLLECTION)
    /** Referencia a la colección de bodegas en Firestore. */
    private val warehousesCol = firestore.collection(WAREHOUSES_COLLECTION)
    /** Referencia a la colección de materiales en Firestore. */
    private val materialsCol = firestore.collection(MATERIALS_COLLECTION)

    /**
     * Obtiene las solicitudes de materiales con opciones de ordenamiento y filtrado.
     *
     * @param orderBy Campo por el cual ordenar los resultados.
     * @param direction Dirección del ordenamiento (ASCENDENTE o DESCENDENTE).
     * @param statusFilter Filtro opcional por estado de la solicitud.
     * @return Flujo que emite recursos con la lista de solicitudes de materiales.
     */
    override fun getMaterialRequests(
        orderBy: String,
        direction: Query.Direction,
        statusFilter: RequestStatus?
    ): Flow<Resource<List<MaterialRequest>>> = callbackFlow {
        // 1. Empezamos con la referencia a la colección
        var query: Query = requestsCollection
        // 2. Si se especifica un filtro de estado, lo aplicamos
        if (statusFilter != null) {
            query = query.whereEqualTo("status", statusFilter.name)
        }

        // 3. Aplicamos el ordenamiento
        query = query.orderBy(orderBy, direction)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Si el error es de permisos (usuario no autenticado), no crashear
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al escuchar las solicitudes."))
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                Timber.d("📦 Documentos recibidos: ${snapshot.size()}")
                val requests = snapshot.documents.mapNotNull {
                    val request = it.toMaterialRequest()
                    Timber.d("Documento ${it.id}: ${if (request != null) "✅ OK" else "❌ NULL"}")
                    request
                }
                Timber.d("✅ Solicitudes parseadas: ${requests.size}")
                trySend(Resource.Success(requests))
            }
        }

        awaitClose { subscription.remove() }
    }

    /**
     * Crea una nueva solicitud de materiales en Firestore.
     *
     * @param request Objeto MaterialRequest que contiene los detalles de la solicitud.
     * @return Recurso que indica el éxito o error de la operación.
     */
    override suspend fun createMaterialRequest(request: MaterialRequest): Resource<Unit> {
        return try {
            // Usamos el ID del objeto request como el ID del nuevo documento en Firestore.
            // Este ID debe ser generado antes de llamar a esta función (ej. con UUID.randomUUID()).
            requestsCollection.document(request.id).set(request.toFirestoreMap()).await()
            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error("Error de base de datos: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Ocurrió un error inesperado al crear la solicitud.")
        }
    }

    /**
     * Actualiza el estado de una solicitud existente con notas del administrador.
     * Usado por el administrador para aprobar o rechazar.
     * @param requestId ID de la solicitud a actualizar.
     * @param status Nuevo estado de la solicitud.
     * @param adminNotes Notas opcionales del administrador.
     * @return Recurso que indica el éxito o error de la operación.
     */
    override suspend fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
        adminNotes: String?
    ): Resource<Unit> {
        return try {
            val requestRef = requestsCollection.document(requestId)
            val updates = mutableMapOf<String, Any?>(
                "status" to status
            )
            if (adminNotes != null) {
                updates["adminNotes"] = adminNotes
            }
            if (status == RequestStatus.APROBADO) {
                updates["approvalDate"] = Date()
            } else if (status == RequestStatus.RECHAZADO) {
                updates["rejectionDate"] = Date()
            }

            requestRef.update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al actualizar la solicitud")
        }
    }
    /**
     * Versión simplificada de updateRequestStatus sin notas del administrador.
     * @param requestId ID de la solicitud a actualizar.
     * @param newStatus Nuevo estado de la solicitud.
     * @return Recurso que indica el éxito o error de la operación.
     */
    override suspend fun updateRequestStatus(requestId: String, newStatus: RequestStatus): Resource<Unit> {
        // Delegar a la versión existente que acepta adminNotes (sin notas por defecto)
        return updateRequestStatus(requestId, status = newStatus, adminNotes = null)
    }

    /**
     * Obtiene las solicitudes de materiales para un trabajador específico en tiempo real.
     * @param workerId ID del trabajador cuyas solicitudes se van a obtener.
     * @return Flujo que emite recursos con la lista de solicitudes del trabajador.
     */
    override fun getRequestsForWorker(workerId: String): Flow<Resource<List<MaterialRequest>>> = callbackFlow {
        val query = requestsCollection
            .whereEqualTo("workerId", workerId)
            .orderBy("requestDate", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))

                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al obtener tus solicitudes."))

                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val requests = snapshot.documents.mapNotNull { it.toMaterialRequest() }
                trySend(Resource.Success(requests))
            }
        }

        awaitClose { subscription.remove() }
    }

    /**
     * Obtiene la lista de todas las solicitudes de materiales en tiempo real.
     * @return Flujo que emite recursos con la lista de todas las solicitudes de materiales.
     */
    override fun getAllRequests(): Flow<Resource<List<MaterialRequest>>> {
        return requestsCollection.snapshots().map { snapshot ->
            try {
                val requests = snapshot.toObjects<MaterialRequest>()
                Resource.Success(requests)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Error al obtener todas las solicitudes")
            }
        }
    }

    /**
     * Obtiene la lista de todas las bodegas en tiempo real.
     * @return Flujo que emite recursos con la lista de bodegas.
     */
    override fun getWarehouses(): Flow<Resource<List<Warehouse>>> {
        return warehousesCol.snapshots().map { snapshot ->
            try {
                val warehouses = snapshot.toObjects<Warehouse>()
                Resource.Success(warehouses)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Error al obtener bodegas")
            }
        }
    }

    /**
     * Crea una nueva bodega (fija o móvil).
     * @param name Nombre de la bodega.
     * @param type Tipo de bodega (fija o móvil).
     * @return Recurso que indica el éxito o error de la operación.
     */
    override suspend fun createWarehouse(name: String, type: WarehouseType): Resource<Unit> {
        return try {
            val newWarehouse = Warehouse(
                name = name,
                type = type
            )
            warehousesCol.add(newWarehouse).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al crear la bodega")
        }
    }

    /**
     * Obtiene la lista de todas las definiciones de materiales en tiempo real.
     * @return Flujo que emite recursos con la lista de materiales.
     */
    override fun getMaterials(): Flow<Resource<List<Material>>> {
        return materialsCol.snapshots().map { snapshot ->
            try {
                val materials = snapshot.toObjects<Material>()
                Resource.Success(materials)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Error al obtener definiciones de materiales")
            }
        }
    }

    /**
     * Crea una nueva definición de material.
     * @param name Nombre del material.
     * @param description Descripción del material.
     * @return Recurso que indica el éxito o error de la operación.
     */
    override suspend fun createMaterialDefinition(name: String, description: String): Resource<Unit> {
        return try {
            val newMaterial = Material(
                name = name,
                description = description
            )
            materialsCol.add(newMaterial).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al crear el material")
        }
    }

    /**
     * Obtiene el stock de una bodega específica en tiempo real.
     * @param warehouseId ID de la bodega cuyo stock se va a obtener.
     * @return Flujo que emite recursos con la lista de items en stock de la bodega.
     */
    override fun getStockForWarehouse(warehouseId: String): Flow<Resource<List<StockItem>>> {
        // Consultamos la sub-colección "stock" dentro del documento de la bodega
        return warehousesCol.document(warehouseId)
            .collection(STOCK_SUBCOLLECTION)
            .snapshots()
            .map { snapshot ->
                try {
                    val stockItems = snapshot.toObjects<StockItem>()
                    Resource.Success(stockItems)
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Error al obtener el stock de la bodega")
                }
            }
    }

    /**
     * Transfiere stock de un material entre dos bodegas.
     * @param fromWarehouseId ID de la bodega de origen.
     * @param toWarehouseId ID de la bodega de destino.
     * @param material Material a transferir.
     * @param quantityToTransfer Cantidad a transferir.
     * @return Recurso que indica el éxito o error de la operación.
     */
    override suspend fun transferStock(
        fromWarehouseId: String,
        toWarehouseId: String,
        material: Material,
        quantityToTransfer: Int
    ): Resource<Unit> {
        if (quantityToTransfer <= 0) {
            return Resource.Error("La cantidad a transferir debe ser positiva")
        }
        return try {
            firestore.runTransaction { transaction ->
                // Definimos las referencias a los documentos de stock
                // Usamos el ID del Material como ID del documento de StockItem para eficiencia
                val fromStockRef = warehousesCol.document(fromWarehouseId)
                    .collection(STOCK_SUBCOLLECTION).document(material.id)

                val toStockRef = warehousesCol.document(toWarehouseId)
                    .collection(STOCK_SUBCOLLECTION).document(material.id)

                // 1. Obtener el stock de origen y validar existencia
                val fromStockDoc = transaction.get(fromStockRef)

                // Validar que el documento existe primero
                if (!fromStockDoc.exists()) {
                    throw Exception("El material '${material.name}' no existe en la bodega de origen")
                }

                val fromStockItem = fromStockDoc.toObject(StockItem::class.java)
                    ?: throw Exception("Error al leer el stock del material '${material.name}'")

                // Validar cantidad disponible
                if (fromStockItem.quantity < quantityToTransfer) {
                    throw Exception("Cantidad insuficiente de '${material.name}' en bodega de origen. Disponible: ${fromStockItem.quantity}, Solicitado: $quantityToTransfer")
                }

                // 2. Restar del origen
                val newFromQuantity = fromStockItem.quantity - quantityToTransfer
                transaction.update(fromStockRef, "quantity", newFromQuantity)

                // 3. Obtener el stock de destino (puede no existir)
                val toStockDoc = transaction.get(toStockRef)
                val toStockItem = toStockDoc.toObject(StockItem::class.java)

                if (toStockItem == null) {
                    // Si no existe, creamos el nuevo StockItem en el destino
                    val newStockItem = StockItem(
                        id = material.id,
                        materialId = material.id,
                        materialName = material.name,
                        quantity = quantityToTransfer
                    )
                    transaction.set(toStockRef, newStockItem)
                } else {
                    // Si existe, solo actualizamos la cantidad
                    val newToQuantity = toStockItem.quantity + quantityToTransfer
                    transaction.update(toStockRef, "quantity", newToQuantity)
                }

                null // Requerido por la transacción
            }.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error durante la transferencia de stock")
        }
    }

    /**
     * Entrega el material solicitado en una solicitud aprobada.
     * Actualiza los stocks de la bodega central y la bodega móvil del trabajador.
     * @param requestId ID de la solicitud a entregar.
     * @param centralWarehouseId ID de la bodega central desde donde se entrega el material.
     * @param adminNotes Notas opcionales del administrador sobre la entrega.
     * @return Recurso que indica el éxito o error de la operación.
     */
    override suspend fun deliverMaterialRequest(
        requestId: String,
        centralWarehouseId: String,
        adminNotes: String?
    ): Resource<Unit> {
        android.util.Log.d("InventoryRepo", "═══════════════════════════════════")
        android.util.Log.d("InventoryRepo", "📦 INICIANDO ENTREGA DE MATERIAL")
        android.util.Log.d("InventoryRepo", "Request ID: $requestId")
        android.util.Log.d("InventoryRepo", "Bodega Central ID: $centralWarehouseId")
        android.util.Log.d("InventoryRepo", "═══════════════════════════════════")

        return try {
            // Usamos una transacción para garantizar atomicidad
            firestore.runTransaction { transaction ->
                android.util.Log.d("InventoryRepo", "🔄 Iniciando transacción de Firestore")

                // 1. Obtener la solicitud
                val requestRef = requestsCollection.document(requestId)
                val requestDoc = transaction.get(requestRef)

                if (!requestDoc.exists()) {
                    android.util.Log.e("InventoryRepo", "❌ La solicitud no existe: $requestId")
                    throw Exception("La solicitud no existe")
                }

                android.util.Log.d("InventoryRepo", "✅ Solicitud encontrada")

                // Parseamos manualmente los campos necesarios
                val status = requestDoc.getString("status")?.let { RequestStatus.valueOf(it) }
                val warehouseId = requestDoc.getString("warehouseId")

                android.util.Log.d("InventoryRepo", "Estado actual: $status")
                android.util.Log.d("InventoryRepo", "Bodega destino: $warehouseId")

                @Suppress("UNCHECKED_CAST")
                val itemsList = requestDoc.get("items") as? List<Map<String, Any>> ?: emptyList()
                android.util.Log.d("InventoryRepo", "Cantidad de items: ${itemsList.size}")

                // Validaciones
                if (status != RequestStatus.APROBADO) {
                    android.util.Log.e("InventoryRepo", "❌ Estado incorrecto: $status (debe ser APROBADO)")
                    throw Exception("Solo se pueden entregar solicitudes APROBADAS. Estado actual: $status")
                }

                if (warehouseId == null) {
                    android.util.Log.e("InventoryRepo", "❌ warehouseId es null")
                    throw Exception("warehouseId no encontrado en la solicitud")
                }

                if (itemsList.isEmpty()) {
                    android.util.Log.e("InventoryRepo", "❌ La lista de items está vacía")
                    throw Exception("La solicitud no tiene materiales")
                }
                android.util.Log.d("InventoryRepo", "✅ Validaciones pasadas, procesando ${itemsList.size} items")

                // 2. Leer TODOS los stocks (central y móvil) de TODOS los items PRIMERO
                data class StockData(
                    val materialId: String,
                    val materialName: String,
                    val quantity: Int,
                    val centralStockDoc: com.google.firebase.firestore.DocumentSnapshot,
                    val mobileStockDoc: com.google.firebase.firestore.DocumentSnapshot,
                    val centralStockRef: com.google.firebase.firestore.DocumentReference,
                    val mobileStockRef: com.google.firebase.firestore.DocumentReference
                )

                val stockDataList = mutableListOf<StockData>()

                // 2. Procesar cada material de la solicitud
                itemsList.forEachIndexed { index, itemMap ->
                    android.util.Log.d("InventoryRepo", "───────────────────────────────────")
                    android.util.Log.d("InventoryRepo", "📦 Procesando item ${index + 1}/${itemsList.size}")

                    val materialId = itemMap["materialId"] as? String
                        ?: throw Exception("materialId faltante en item ${index + 1}")
                    val materialName = itemMap["materialName"] as? String
                        ?: throw Exception("materialName faltante en item ${index + 1}")
                    val quantity = (itemMap["quantity"] as? Long)?.toInt()
                        ?: throw Exception("quantity faltante en item ${index + 1}")

                    android.util.Log.d("InventoryRepo", "Material: $materialName (ID: $materialId)")
                    android.util.Log.d("InventoryRepo", "Cantidad solicitada: $quantity")

                    // 3. Verificar stock disponible en Bodega Central
                    val centralStockRef = warehousesCol.document(centralWarehouseId)
                        .collection(STOCK_SUBCOLLECTION)
                        .document(materialId)

                    val mobileStockRef = warehousesCol.document(warehouseId)
                        .collection(STOCK_SUBCOLLECTION)
                        .document(materialId)

                    val centralStockDoc = transaction.get(centralStockRef)
                    val mobileStockDoc = transaction.get(mobileStockRef)

                    if (!centralStockDoc.exists()) {
                        android.util.Log.e("InventoryRepo", "❌ Material '$materialName' no existe en Bodega Central")
                        throw Exception("El material '$materialName' no existe en la Bodega Central")
                    }

                    val availableQuantity = centralStockDoc.getLong("quantity")?.toInt() ?: 0
                    android.util.Log.d("InventoryRepo", "Stock disponible en Bodega Central: $availableQuantity")

                    if (availableQuantity < quantity) {
                        android.util.Log.e("InventoryRepo", "❌ Stock insuficiente: Disponible=$availableQuantity, Solicitado=$quantity")
                        throw Exception("Stock insuficiente de '$materialName' en Bodega Central. Disponible: $availableQuantity, Solicitado: $quantity")
                    }

                    stockDataList.add(StockData(
                        materialId = materialId,
                        materialName = materialName,
                        quantity = quantity,
                        centralStockDoc = centralStockDoc,
                        mobileStockDoc = mobileStockDoc,
                        centralStockRef = centralStockRef,
                        mobileStockRef = mobileStockRef
                    ))
                }
                Timber.tag("InventoryRepo").d("✅ Todas las lecturas completadas")

                stockDataList.forEachIndexed { index, stockData ->
                Timber.tag("InventoryRepo").d("───────────────────────────────────")
                    Timber.tag("InventoryRepo")
                        .d("✍️ Escribiendo item ${index + 1}/${stockDataList.size}")
                android.util.Log.d("InventoryRepo", "Material: ${stockData.materialName}")

                val availableQuantity = stockData.centralStockDoc.getLong("quantity")?.toInt() ?: 0
                val newCentralQuantity = availableQuantity - stockData.quantity

                android.util.Log.d("InventoryRepo", "🔽 Restando de Bodega Central: $availableQuantity - ${stockData.quantity} = $newCentralQuantity")

                // Actualizar stock central

                android.util.Log.d("InventoryRepo", "🔽 Restando de Bodega Central: $availableQuantity - ${stockData.quantity} = $newCentralQuantity")
                    if (newCentralQuantity == 0) {
                        // Si queda en 0, eliminamos el documento
                        android.util.Log.d("InventoryRepo", "🗑️ Stock quedó en 0, eliminando documento")
                        transaction.delete(stockData.centralStockRef)
                    } else {
                        transaction.update(stockData.centralStockRef, "quantity", newCentralQuantity)
                    }

                // Actualizar stock móvil
                if (stockData.mobileStockDoc.exists()) {
                    val currentQuantity = stockData.mobileStockDoc.getLong("quantity")?.toInt() ?: 0
                    val newMobileQuantity = currentQuantity + stockData.quantity
                    android.util.Log.d("InventoryRepo", "🔼 Sumando a Bodega Móvil: $currentQuantity + ${stockData.quantity} = $newMobileQuantity")
                    transaction.update(stockData.mobileStockRef, "quantity", newMobileQuantity)
                } else {
                    android.util.Log.d("InventoryRepo", "➕ Creando nuevo stock en Bodega Móvil: ${stockData.quantity} unidades")
                    val newStockItem = StockItem(
                        id = stockData.materialId,
                        materialId = stockData.materialId,
                        materialName = stockData.materialName,
                        quantity = stockData.quantity
                    )
                    transaction.set(stockData.mobileStockRef, newStockItem)
                }

                android.util.Log.d("InventoryRepo", "✅ Item ${index + 1} escrito correctamente")
            }
                android.util.Log.d("InventoryRepo", "───────────────────────────────────")
                android.util.Log.d("InventoryRepo", "🔄 Actualizando estado de solicitud a ENTREGADO")

                // 6. Actualizar el estado de la solicitud a ENTREGADO
                val updates = mutableMapOf<String, Any?>(
                    "status" to RequestStatus.ENTREGADO.name,
                    "deliveryDate" to Date()
                )

                if (adminNotes != null) {
                    updates["adminNotes"] = adminNotes
                }

                transaction.update(requestRef, updates)

                null // Requerido por runTransaction
            }.await()
            android.util.Log.d("InventoryRepo", "═══════════════════════════════════")
            android.util.Log.d("InventoryRepo", "✅ ENTREGA COMPLETADA EXITOSAMENTE")
            android.util.Log.d("InventoryRepo", "═══════════════════════════════════")
            Resource.Success(Unit)

        } catch (e: Exception) {
            android.util.Log.e("InventoryRepo", "═══════════════════════════════════")
            android.util.Log.e("InventoryRepo", "❌ ERROR EN ENTREGA DE MATERIAL")
            android.util.Log.e("InventoryRepo", "Mensaje: ${e.message}")
            android.util.Log.e("InventoryRepo", "Stack trace:", e)
            android.util.Log.e("InventoryRepo", "═══════════════════════════════════")
            Resource.Error(e.message ?: "Error al entregar el material")
        }
    }

    /**
     * Agrega stock a una bodega específica.
     * @param warehouseId ID de la bodega donde se agregará el stock.
     * @param material Material al que se le agregará stock.
     * @param quantity Cantidad a agregar.
     * @return Recurso que indica el éxito o error de la operación.
     */
    override suspend fun addStockToWarehouse(
        warehouseId: String,
        material: Material,
        quantity: Int
    ): Resource<Unit> {
        if (quantity <= 0) {
            return Resource.Error("La cantidad debe ser positiva")
        }

        return try {
            val stockRef = warehousesCol.document(warehouseId)
                .collection(STOCK_SUBCOLLECTION)
                .document(material.id)

            firestore.runTransaction { transaction ->
                val stockDoc = transaction.get(stockRef)

                if (stockDoc.exists()) {
                    // Ya existe, incrementar
                    val currentQty = stockDoc.getLong("quantity")?.toInt() ?: 0
                    transaction.update(stockRef, "quantity", currentQty + quantity)
                } else {
                    // No existe, crear
                    val newStock = StockItem(
                        id = material.id,
                        materialId = material.id,
                        materialName = material.name,
                        quantity = quantity
                    )
                    transaction.set(stockRef, newStock)
                }
            }.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al agregar stock")
        }
    }

    /**
     * Obtiene el inventario consolidado de todos los materiales en todas las bodegas.
     * @return Flujo que emite recursos con la lista de inventario consolidado.
     */
    override fun getConsolidatedInventory(): Flow<Resource<List<ConsolidatedStock>>> = callbackFlow {
        val materialsQuery = firestore.collection("materials")
        val warehousesQuery = firestore.collection("warehouses")

        val materialsListener = materialsQuery.addSnapshotListener { materialsSnapshot, materialsError ->
            if (materialsError != null) {
                if (materialsError is FirebaseFirestoreException &&
                    materialsError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))

                } else {
                    trySend(Resource.Error(materialsError.localizedMessage ?: "Error al cargar inventario"))
                }
                return@addSnapshotListener
            }

            if (materialsSnapshot != null) {
                // Por cada material, consultar el stock en todas las bodegas
                viewModelScope.launch {
                    try {
                        val consolidatedList = mutableListOf<ConsolidatedStock>()

                        for (materialDoc in materialsSnapshot.documents) {
                            val materialId = materialDoc.id
                            val materialName = materialDoc.getString("name") ?: "Sin nombre"

                            // Obtener todas las bodegas
                            val warehousesSnapshot = warehousesQuery.get().await()
                            val warehouseStockList = mutableListOf<WarehouseStock>()
                            var totalQuantity = 0

                            for (warehouseDoc in warehousesSnapshot.documents) {
                                val warehouseId = warehouseDoc.id
                                val warehouseName = warehouseDoc.getString("name") ?: "Sin nombre"

                                // Buscar el stock de este material en esta bodega
                                val stockDoc = firestore
                                    .collection("warehouses")
                                    .document(warehouseId)
                                    .collection("stock")
                                    .document(materialId)
                                    .get()
                                    .await()

                                val quantity = stockDoc.getLong("quantity")?.toInt() ?: 0

                                if (quantity > 0) {
                                    warehouseStockList.add(
                                        WarehouseStock(
                                            warehouseId = warehouseId,
                                            warehouseName = warehouseName,
                                            quantity = quantity
                                        )
                                    )
                                    totalQuantity += quantity
                                }
                            }

                            consolidatedList.add(
                                ConsolidatedStock(
                                    materialId = materialId,
                                    materialName = materialName,
                                    totalQuantity = totalQuantity,
                                    warehouseBreakdown = warehouseStockList
                                )
                            )
                        }

                        trySend(Resource.Success(consolidatedList))
                    } catch (e: Exception) {
                        trySend(Resource.Error(e.localizedMessage ?: "Error al consolidar inventario"))
                    }
                }
            }
        }

        awaitClose { materialsListener.remove() }
    }

    /**
     * Función de extensión privada para convertir un MaterialRequest en un Map
     * que Firestore pueda entender y almacenar.
     * @return Mapa con los campos del MaterialRequest.
     */
    private fun MaterialRequest.toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "workerId" to workerId,
            "workerName" to workerName,
            "warehouseId" to warehouseId,
            "items" to items.map { item ->
                mapOf(
                    "materialId" to item.materialId,
                    "materialName" to item.materialName,
                    "quantity" to item.quantity
                )
            },
            "status" to status.name, // Guardamos el enum como un String
            "requestDate" to requestDate,
            "approvalDate" to approvalDate,
            "rejectionDate" to rejectionDate,
            "deliveryDate" to deliveryDate,
            "adminNotes" to adminNotes
        )
    }

    /**
     * Función de extensión para convertir un DocumentSnapshot de Firestore
     * en nuestro objeto de dominio MaterialRequest.
     * @return Objeto MaterialRequest o null si ocurre un error durante la conversión.
     */
    private fun DocumentSnapshot.toMaterialRequest(): MaterialRequest? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val itemsList = get("items") as? List<Map<String, Any>> ?: emptyList()

            val items = itemsList.map { itemMap ->
                RequestItem(
                    materialId = itemMap["materialId"] as? String ?: "",
                    materialName = itemMap["materialName"] as? String ?: "",
                    quantity = (itemMap["quantity"] as? Long)?.toInt() ?: 0
                )
            }
            MaterialRequest(
                id = getString("id")!!,
                workerId = getString("workerId")!!,
                workerName = getString("workerName")!!,
                warehouseId = getString("warehouseId")!!,
                items = items,
                status = RequestStatus.valueOf(getString("status")!!),
                requestDate = getDate("requestDate")!!,
                approvalDate = getDate("approvalDate"),
                rejectionDate = getDate("rejectionDate"),
                deliveryDate = getDate("deliveryDate"),
                adminNotes = getString("adminNotes")

            )
        } catch (e: Exception) {
            null
        }
    }

    // Paginación para solicitudes de materiales

    // Para vista de ADMINISTRADOR (todas las solicitudes)
    /**
     * Obtiene las solicitudes de materiales paginadas para el administrador.
     * Permite ordenar y filtrar por estado.
     *
     * @param orderBy Campo por el cual ordenar.
     * @param direction Dirección de ordenamiento (ASCENDENTE o DESCENDENTE).
     * @param statusFilter Filtro opcional por estado de la solicitud.
     * @return Flujo que emite datos paginados de MaterialRequest.
     */
    override fun getMaterialRequestsPaged(
        orderBy: String,
        direction: Query.Direction,
        statusFilter: RequestStatus?
    ): Flow<PagingData<MaterialRequest>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                MaterialRequestPagingSource(
                    requestsCollection = requestsCollection,
                    workerId = null,  // null = todas las solicitudes
                    statusFilter = statusFilter
                )
            }
        ).flow
    }

    // Para vista de TRABAJADOR (solo sus solicitudes)
    /**
     * Obtiene las solicitudes de materiales paginadas para un trabajador específico.
     *
     * @param workerId ID del trabajador cuyas solicitudes se van a obtener.
     * @return Flujo que emite datos paginados de MaterialRequest.
     */
    override fun getRequestsForWorkerPaged(workerId: String): Flow<PagingData<MaterialRequest>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                MaterialRequestPagingSource(
                    requestsCollection = requestsCollection,
                    workerId = workerId,
                    statusFilter = null
                )
            }
        ).flow
    }
}