package dev.ycosorio.flujo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.model.InventoryItem
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow
import com.google.firebase.firestore.Query
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
                trySend(Resource.Error(error.localizedMessage ?: "Error al escuchar las solicitudes."))
                close(error)
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

    override suspend fun updateRequestStatus(requestId: String, newStatus: RequestStatus): Resource<Unit> {
        return try {
            // Creamos un mapa solo con el campo que queremos actualizar para ser más eficientes.
            val updates = mapOf(
                "status" to newStatus.name,
                // Opcional: También podríamos actualizar la fecha de aprobación aquí.
                "approvalDate" to Date()
            )
            requestsCollection.document(requestId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error("Error de base de datos: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Ocurrió un error inesperado al actualizar la solicitud.")
        }
    }

    override fun getRequestsForWorker(workerId: String): Flow<Resource<List<MaterialRequest>>> = callbackFlow {
        val query = requestsCollection
            .whereEqualTo("workerId", workerId)
            .orderBy("requestDate", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.localizedMessage ?: "Error al obtener tus solicitudes."))
                close(error)
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

    override suspend fun createMaterial(name: String, initialStock: Int): Resource<Unit> {
        return try {
            // Creamos el nuevo item
            val newItem = InventoryItem(
                id = "", // El ID será asignado por Firestore
                name = name,
                quantity = initialStock,
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
        "materialId" to materialId,
        "materialName" to materialName,
        "quantity" to quantity,
        "status" to status.name, // Guardamos el enum como un String
        "requestDate" to requestDate,
        "approvalDate" to approvalDate,
        "pickupDate" to pickupDate
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
            materialId = getString("materialId")!!,
            materialName = getString("materialName")!!,
            quantity = getLong("quantity")?.toInt() ?: 0,
            status = RequestStatus.valueOf(getString("status")!!),
            requestDate = getDate("requestDate")!!,
            approvalDate = getDate("approvalDate"),
            pickupDate = getDate("pickupDate")
        )
    } catch (e: Exception) {
        // Si algún campo falta o es incorrecto, no incluimos la solicitud en la lista.
        null
    }
}