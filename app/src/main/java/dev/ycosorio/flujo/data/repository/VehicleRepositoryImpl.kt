package dev.ycosorio.flujo.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import dev.ycosorio.flujo.domain.model.Vehicle
import dev.ycosorio.flujo.domain.repository.VehicleRepository
import dev.ycosorio.flujo.utils.FirestoreConstants.USERS_COLLECTION
import dev.ycosorio.flujo.utils.FirestoreConstants.VEHICLES_COLLECTION
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class VehicleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : VehicleRepository {

    private val vehiclesCollection = firestore.collection(VEHICLES_COLLECTION)
    private val usersCollection = firestore.collection(USERS_COLLECTION)

    override fun getVehicles(): Flow<Resource<List<Vehicle>>> {
        return vehiclesCollection.snapshots().map { snapshot ->
            try {
                val vehicles = snapshot.documents.mapNotNull {
                    it.toObject<Vehicle>()
                }
                Resource.Success(vehicles)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Error al obtener vehículos")
            }
        }
    }

    override suspend fun createVehicle(plate: String, description: String): Resource<Unit> {
        return try {
            val newVehicleRef = vehiclesCollection.document()
            val newVehicle = Vehicle(
                id = newVehicleRef.id,
                plate = plate,
                description = description,
                userIds = emptyList(),
                maxUsers = 6 // Valor por defecto
            )
            newVehicleRef.set(newVehicle).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al crear el vehículo")
        }
    }

    override suspend fun deleteVehicle(vehicleId: String): Resource<Unit> {
        return try {
            // Transacción para asegurar que desasignamos a los usuarios antes de borrar
            firestore.runTransaction { transaction ->
                val vehicleRef = vehiclesCollection.document(vehicleId)
                val vehicleDoc = transaction.get(vehicleRef)
                val vehicle = vehicleDoc.toObject<Vehicle>()

                if (vehicle != null) {
                    // 1. Desasignar a todos los usuarios de este vehículo
                    vehicle.userIds.forEach { userId ->
                        val userRef = usersCollection.document(userId)
                        transaction.update(userRef, "assignedVehicleId", null)
                    }
                }
                // 2. Borrar el vehículo
                transaction.delete(vehicleRef)
                null // Requerido por runTransaction
            }.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al eliminar el vehículo")
        }
    }

    override suspend fun assignUserToVehicle(userId: String, vehicleId: String): Resource<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val vehicleRef = vehiclesCollection.document(vehicleId)
                val userRef = usersCollection.document(userId)

                val vehicleDoc = transaction.get(vehicleRef)
                val vehicle = vehicleDoc.toObject<Vehicle>()
                    ?: throw IllegalStateException("El vehículo no existe")

                // Validar la regla de negocio (1-6 usuarios)
                if (vehicle.userIds.size >= vehicle.maxUsers) {
                    throw IllegalStateException("El vehículo ya está lleno (Máx: ${vehicle.maxUsers})")
                }

                // 1. Actualizar el vehículo (añadir userId a la lista)
                transaction.update(vehicleRef, "userIds", FieldValue.arrayUnion(userId))

                // 2. Actualizar el usuario (asignar vehicleId)
                transaction.update(userRef, "assignedVehicleId", vehicleId)

                null // Requerido por runTransaction
            }.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al asignar usuario")
        }
    }

    override suspend fun removeUserFromVehicle(userId: String, vehicleId: String): Resource<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val vehicleRef = vehiclesCollection.document(vehicleId)
                val userRef = usersCollection.document(userId)

                // 1. Actualizar el vehículo (quitar userId de la lista)
                transaction.update(vehicleRef, "userIds", FieldValue.arrayRemove(userId))

                // 2. Actualizar el usuario (quitar vehicleId)
                transaction.update(userRef, "assignedVehicleId", null)

                null // Requerido por runTransaction
            }.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al remover usuario")
        }
    }

    override suspend fun transferVehicleToWarehouse(vehicleId: String, warehouseId: String): Resource<Unit> {
        return try {
            // 1. Validar que la bodega existe y es MOBILE
            val warehouseDoc = firestore.collection("warehouses")
                .document(warehouseId)
                .get()
                .await()

            if (!warehouseDoc.exists()) {
                throw IllegalStateException("La bodega no existe")
            }

            val warehouseType = warehouseDoc.getString("type")

            if (warehouseType != "MOBILE") {
                throw IllegalStateException("Solo se pueden asignar bodegas móviles a vehículos")
            }

            // 2. Verificar que no esté asignada a otro vehículo
            val assignedVehicles = vehiclesCollection
                .whereEqualTo("assignedWarehouseId", warehouseId)
                .get()
                .await()

            val alreadyAssigned = assignedVehicles.documents.any { it.id != vehicleId }

            if (alreadyAssigned) {
                throw IllegalStateException("Esta bodega móvil ya está asignada a otro vehículo")
            }

            // 3. Asignar bodega
            vehiclesCollection.document(vehicleId)
                .update("assignedWarehouseId", warehouseId)
                .await()

            Resource.Success(Unit)
        } catch (e: IllegalStateException) {
            Resource.Error(e.message ?: "Error de validación")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al asignar bodega")
        }
    }

    override suspend fun removeWarehouseFromVehicle(vehicleId: String): Resource<Unit> {
        return try {
            vehiclesCollection.document(vehicleId)
                .update("assignedWarehouseId", null)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al desasignar bodega")
        }
    }


    override suspend fun getVehicle(vehicleId: String): Resource<Vehicle?> {
        return try {
            val vehicleDoc = vehiclesCollection.document(vehicleId).get().await()
            val vehicle = vehicleDoc.toObject<Vehicle>()
            Resource.Success(vehicle)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al obtener el vehículo")
        }
    }
}