package dev.ycosorio.flujo.utils

import kotlinx.coroutines.flow.MutableStateFlow
import dev.ycosorio.flujo.BuildConfig

/**
 * ⚠️ ADVERTENCIA DE SEGURIDAD ⚠️
 *
 * Este objeto es ÚNICAMENTE para desarrollo y testing local.
 * Permite simular usuarios sin Firebase Auth para desarrollo rápido.
 *
 * NUNCA debe usarse en producción. Configurar ProGuard/R8 para removerlo:
 *
 * En proguard-rules.pro:
 * -assumenosideeffects class dev.ycosorio.flujo.utils.SimulationAuth {
 *     *;
 * }
 *
 * TODO: Eliminar este archivo completamente cuando Firebase Auth esté implementado
 */
object SimulationAuth {
    // IDs de usuario de prueba (solo para desarrollo local)
    // ⚠️ NUNCA usar estos IDs en producción
    const val ADMIN_ID = if (BuildConfig.DEBUG) "admin_001" else ""
    const val WORKER_ID = if (BuildConfig.DEBUG) "worker_001" else ""

    // El StateFlow que la UI y los ViewModels observarán
    // Solo funciona en modo DEBUG
    val currentUserId = MutableStateFlow(
        if (BuildConfig.DEBUG) ADMIN_ID else ""
    )

    /**
     * Cambia el usuario actual entre Admin y Trabajador.
     */
    fun toggleUser() {

        // Solo permitir cambio en modo DEBUG
        if (!BuildConfig.DEBUG) {
            return
        }

        if (currentUserId.value == ADMIN_ID) {
            currentUserId.value = WORKER_ID
        } else {
            currentUserId.value = ADMIN_ID
        }
    }
}