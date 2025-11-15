package dev.ycosorio.flujo.domain.model

import com.google.firebase.Timestamp
import java.util.Date

/**
 * Representa el modelo de datos de un usuario en la capa de negocio.
 * Contiene la información fundamental y las asignaciones del trabajador.
 */
data class User(
    val uid: String,
    val name: String,
    val email: String,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val role: Role,
    val position: String, // Cargo del trabajador, ej: "Técnico de Campo"
    val area: String, // Área a la que pertenece, ej: "Instalaciones"
    val contractStartDate: Date, // Fecha de inicio de contrato
    val contractEndDate: Date? = null, // Fecha de término, nulo si es indefinido
    val assignedVehicleId: String? = null,
    val assignedPhoneId: String? = null,
    val assignedPcId: String? = null,
    val fcmToken: String? = null,
    val tokenUpdatedAt: Timestamp? = null
)