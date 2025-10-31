package dev.ycosorio.flujo.utils

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Un objeto singleton para simular el estado de autenticación en toda la app.
 * Esto nos permite "cambiar de usuario" (Admin/Trabajador) sin implementar Firebase Auth.
 */
object SimulationAuth {
    // IDs de usuario de prueba (debes tenerlos creados en tu Firestore)
    const val ADMIN_ID = "admin_001"
    const val WORKER_ID = "worker_001"

    // El StateFlow que la UI y los ViewModels observarán.
    // Comienza como Admin por defecto.
    val currentUserId = MutableStateFlow(ADMIN_ID)

    /**
     * Cambia el usuario actual entre Admin y Trabajador.
     */
    fun toggleUser() {
        if (currentUserId.value == ADMIN_ID) {
            currentUserId.value = WORKER_ID
        } else {
            currentUserId.value = ADMIN_ID
        }
    }
}