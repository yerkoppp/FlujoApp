package dev.ycosorio.flujo.data.repository

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

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val requestsCollection = firestore.collection(MATERIAL_REQUESTS_COLLECTION)

    private val warehousesCol = firestore.collection(WAREHOUSES_COLLECTION)
    private val materialsCol = firestore.collection(MATERIALS_COLLECTION)

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
                    close()  // ✅ Cerrar sin error
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al escuchar las solicitudes."))
                    close(error)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                android.util.Log.d("InventoryRepo", "📦 Documentos recibidos: ${snapshot.size()}")
                val requests = snapshot.documents.mapNotNull {
                    val request = it.toMaterialRequest()
                    android.util.Log.d("InventoryRepo", "Documento ${it.id}: ${if (request != null) "✅ OK" else "❌ NULL"}")
                    request
                }
                android.util.Log.d("InventoryRepo", "✅ Solicitudes parseadas: ${requests.size}")
                trySend(Resource.Success(requests))
            }
        }

        awaitClose { subscription.remove() }
    }

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
     */
    override suspend fun updateRequestStatus(requestId: String, newStatus: RequestStatus): Resource<Unit> {
        // Delegar a la versión existente que acepta adminNotes (sin notas por defecto)
        return updateRequestStatus(requestId, status = newStatus, adminNotes = null)
    }

    /**
     * Obtiene las solicitudes de materiales para un trabajador específico en tiempo real.
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
                    close()  // ✅ Cerrar sin error
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al obtener tus solicitudes."))
                    close(error)
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

                // 1. Obtener el stock de origen
                val fromStockDoc = transaction.get(fromStockRef)
                val fromStockItem = fromStockDoc.toObject(StockItem::class.java)

                if (fromStockItem == null || fromStockItem.quantity < quantityToTransfer) {
                    throw Exception("Cantidad insuficiente en la bodega de origen. Disponible: ${fromStockItem?.quantity ?: 0}")
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

    override suspend fun deliverMaterialRequest(
        requestId: String,
        centralWarehouseId: String,
        adminNotes: String?
    ): Resource<Unit> {
        return try {
            // Usamos una transacción para garantizar atomicidad
            firestore.runTransaction { transaction ->
                // 1. Obtener la solicitud
                val requestRef = requestsCollection.document(requestId)
                val requestDoc = transaction.get(requestRef)

                if (!requestDoc.exists()) {
                    throw Exception("La solicitud no existe")
                }

                // Parseamos manualmente los campos necesarios
                val status = requestDoc.getString("status")?.let { RequestStatus.valueOf(it) }
                val warehouseId = requestDoc.getString("warehouseId")

                @Suppress("UNCHECKED_CAST")
                val itemsList = requestDoc.get("items") as? List<Map<String, Any>> ?: emptyList()

                // Validaciones
                if (status != RequestStatus.APROBADO) {
                    throw Exception("Solo se pueden entregar solicitudes APROBADAS. Estado actual: $status")
                }

                if (warehouseId == null) {
                    throw Exception("warehouseId no encontrado en la solicitud")
                }

                if (itemsList.isEmpty()) {
                    throw Exception("La solicitud no tiene materiales")
                }

                // 2. Procesar cada material de la solicitud
                itemsList.forEach { itemMap ->
                    val materialId = itemMap["materialId"] as? String
                        ?: throw Exception("materialId faltante en item")
                    val materialName = itemMap["materialName"] as? String
                        ?: throw Exception("materialName faltante en item")
                    val quantity = (itemMap["quantity"] as? Long)?.toInt()
                        ?: throw Exception("quantity faltante en item")

                    // 3. Verificar stock disponible en Bodega Central
                    val centralStockRef = warehousesCol.document(centralWarehouseId)
                        .collection(STOCK_SUBCOLLECTION)
                        .document(materialId)

                    val centralStockDoc = transaction.get(centralStockRef)

                    if (!centralStockDoc.exists()) {
                        throw Exception("El material '$materialName' no existe en la Bodega Central")
                    }

                    val availableQuantity = centralStockDoc.getLong("quantity")?.toInt() ?: 0

                    if (availableQuantity < quantity) {
                        throw Exception("Stock insuficiente de '$materialName' en Bodega Central. Disponible: $availableQuantity, Solicitado: $quantity")
                    }

                    // 4. Restar stock de Bodega Central
                    val newCentralQuantity = availableQuantity - quantity
                    if (newCentralQuantity == 0) {
                        // Si queda en 0, eliminamos el documento
                        transaction.delete(centralStockRef)
                    } else {
                        transaction.update(centralStockRef, "quantity", newCentralQuantity)
                    }

                    // 5. Sumar stock a Bodega Móvil (destino)
                    val mobileStockRef = warehousesCol.document(warehouseId)
                        .collection(STOCK_SUBCOLLECTION)
                        .document(materialId)

                    val mobileStockDoc = transaction.get(mobileStockRef)

                    if (mobileStockDoc.exists()) {
                        // Si ya existe, sumamos
                        val currentQuantity = mobileStockDoc.getLong("quantity")?.toInt() ?: 0
                        val newMobileQuantity = currentQuantity + quantity
                        transaction.update(mobileStockRef, "quantity", newMobileQuantity)
                    } else {
                        // Si no existe, creamos el StockItem
                        val newStockItem = StockItem(
                            id = materialId,
                            materialId = materialId,
                            materialName = materialName,
                            quantity = quantity
                        )
                        transaction.set(mobileStockRef, newStockItem)
                    }
                }

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

            Resource.Success(Unit)

        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al entregar el material")
        }
    }

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

    override fun getConsolidatedInventory(): Flow<Resource<List<ConsolidatedStock>>> = callbackFlow {
        val materialsQuery = firestore.collection("materials")
        val warehousesQuery = firestore.collection("warehouses")

        val materialsListener = materialsQuery.addSnapshotListener { materialsSnapshot, materialsError ->
            if (materialsError != null) {
                if (materialsError is FirebaseFirestoreException &&
                    materialsError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))
                    close()
                } else {
                    trySend(Resource.Error(materialsError.localizedMessage ?: "Error al cargar inventario"))
                    close(materialsError)
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
}