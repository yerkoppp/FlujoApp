package dev.ycosorio.flujo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.model.InventoryItem
import dev.ycosorio.flujo.domain.model.Warehouse
import dev.ycosorio.flujo.domain.model.WarehouseType
import dev.ycosorio.flujo.domain.model.Material
import dev.ycosorio.flujo.domain.model.StockItem
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.map
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import dev.ycosorio.flujo.utils.FirestoreConstants.INVENTORY_COLLECTION
import dev.ycosorio.flujo.utils.FirestoreConstants.MATERIAL_REQUESTS_COLLECTION
import dev.ycosorio.flujo.utils.FirestoreConstants.MATERIALS_COLLECTION
import dev.ycosorio.flujo.utils.FirestoreConstants.WAREHOUSES_COLLECTION
import dev.ycosorio.flujo.utils.FirestoreConstants.STOCK_SUBCOLLECTION
import java.util.Date

/**
 * Implementación del InventoryRepository que se comunica con Firebase Firestore.
 *
 * @property firestore Instancia de FirebaseFirestore inyectada para la comunicación con la base de datos.
 */
class InventoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : InventoryRepository {

    private val inventoryCollection = firestore.collection(INVENTORY_COLLECTION)
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
                // Manejar PERMISSION_DENIED sin propagar el error (ocurre al cerrar sesión)
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))
                    close()  // Cerrar sin propagar el error
                } else {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al escuchar las solicitudes."))
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

 override suspend fun updateRequestStatus(requestId: String, newStatus: RequestStatus): Resource<Unit> {
     // Delegar a la versión existente que acepta adminNotes (sin notas por defecto)
     return updateRequestStatus(requestId, status = newStatus, adminNotes = null)
 }

    override fun getRequestsForWorker(workerId: String): Flow<Resource<List<MaterialRequest>>> = callbackFlow {
        val query = requestsCollection
            .whereEqualTo("workerId", workerId)
            .orderBy("requestDate", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Manejar PERMISSION_DENIED sin propagar el error (ocurre al cerrar sesión)
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(Resource.Error("Sesión finalizada"))
                    close()  // Cerrar sin propagar el error
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
     * Implementación de la función que faltaba.
     * Escucha en tiempo real los cambios en la colección "inventory".
     */
    override fun getAvailableMaterials(): Flow<Resource<List<InventoryItem>>> {
        return inventoryCollection.snapshots().map { snapshot ->
            try {
                // Convierte los documentos a objetos InventoryItem
                // (esto funciona si tu data class tiene @DocumentId val id: String)
                val materials = snapshot.documents.mapNotNull { it.toObject(InventoryItem::class.java) }
                Resource.Success(materials)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Error al obtener materiales")
            }
        }
    }

    override suspend fun createMaterial(name: String, initialQuantity: Int): Resource<Unit> {
        return try {
            // Creamos el nuevo item
            val newItem = InventoryItem(
                id = "", // El ID será asignado por Firestore
                name = name,
                quantity = initialQuantity,
                locationId = "almacen_central" // Ubicación por defecto

            )
            // Dejamos que Firestore asigne el ID
            inventoryCollection.add(newItem).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al crear el material")
        }
    }

    override suspend fun addStockToMaterial(itemId: String, amountToAdd: Int): Resource<Unit> {
        if (amountToAdd <= 0) {
            return Resource.Error("La cantidad debe ser positiva")
        }
        return try {
            val itemRef = inventoryCollection.document(itemId)
            // Usamos FieldValue.increment para añadir stock de forma segura (atómica)
            itemRef.update("stock", FieldValue.increment(amountToAdd.toLong())).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al actualizar stock")
        }
    }

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
                val materialId = requestDoc.getString("materialId")
                val materialName = requestDoc.getString("materialName")
                val quantity = requestDoc.getLong("quantity")?.toInt()

                // Validaciones
                if (status != RequestStatus.APROBADO) {
                    throw Exception("Solo se pueden entregar solicitudes APROBADAS. Estado actual: $status")
                }

                if (warehouseId == null || materialId == null || materialName == null || quantity == null) {
                    throw Exception("Datos incompletos en la solicitud")
                }

                // 2. Verificar stock disponible en Bodega Central
                val centralStockRef = warehousesCol.document(centralWarehouseId)
                    .collection(STOCK_SUBCOLLECTION)
                    .document(materialId)

                val centralStockDoc = transaction.get(centralStockRef)

                if (!centralStockDoc.exists()) {
                    throw Exception("El material no existe en la Bodega Central")
                }

                val availableQuantity = centralStockDoc.getLong("quantity")?.toInt() ?: 0

                if (availableQuantity < quantity) {
                    throw Exception("Stock insuficiente en Bodega Central. Disponible: $availableQuantity, Solicitado: $quantity")
                }

                // 3. Restar stock de Bodega Central
                val newCentralQuantity = availableQuantity - quantity
                transaction.update(centralStockRef, "quantity", newCentralQuantity)

                // 4. Sumar stock a Bodega Móvil (destino)
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

                // 5. Actualizar el estado de la solicitud a ENTREGADO
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
        "materialId" to materialId,
        "materialName" to materialName,
        "quantity" to quantity,
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
        MaterialRequest(
            id = getString("id")!!,
            workerId = getString("workerId")!!,
            workerName = getString("workerName")!!,
            warehouseId = getString("warehouseId")!!,
            materialId = getString("materialId")!!,
            materialName = getString("materialName")!!,
            quantity = getLong("quantity")?.toInt() ?: 0,
            status = RequestStatus.valueOf(getString("status")!!),
            requestDate = getDate("requestDate")!!,
            approvalDate = getDate("approvalDate"),
            rejectionDate = getDate("rejectionDate"),
            deliveryDate = getDate("deliveryDate"),
            adminNotes = getString("adminNotes")

        )
    } catch (e: Exception) {
        // Si algún campo falta o es incorrecto, no incluimos la solicitud en la lista.
        null
    }
}