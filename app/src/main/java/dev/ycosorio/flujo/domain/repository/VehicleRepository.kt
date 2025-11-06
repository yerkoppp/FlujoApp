package dev.ycosorio.flujo.domain.repository

import dev.ycosorio.flujo.domain.model.Vehicle
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun getVehicles(): Flow<Resource<List<Vehicle>>>
    suspend fun createVehicle(plate: String, description: String): Resource<Unit>
    suspend fun deleteVehicle(vehicleId: String): Resource<Unit>

    // Lógica de transferencia y asignación
    suspend fun assignUserToVehicle(userId: String, vehicleId: String): Resource<Unit>
    suspend fun removeUserFromVehicle(userId: String, vehicleId: String): Resource<Unit>

}