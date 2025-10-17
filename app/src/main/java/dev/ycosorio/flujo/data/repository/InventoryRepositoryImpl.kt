package dev.ycosorio.flujo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.util.Date

/**
 * Implementación del InventoryRepository que se comunica con Firebase Firestore.
 *
 * @property firestore Instancia de FirebaseFirestore inyectada para la comunicación con la base de datos.
 */
class InventoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : InventoryRepository {

    private val requestsCollection = firestore.collection("material_requests")

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