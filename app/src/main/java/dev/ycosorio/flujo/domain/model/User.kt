package dev.ycosorio.flujo.domain.model

import com.google.firebase.Timestamp
import java.util.Date

/**
 * Representa el modelo de datos de un usuario en la capa de negocio.
 * Contiene la información fundamental y las asignaciones del trabajador.
 *
 * @property uid El identificador único del usuario, generalmente provisto por el sistema de autenticación (ej. Firebase Auth).
 * @property name El nombre completo del usuario.
 * @property email La dirección de correo electrónico del usuario.
 * @property phoneNumber El número de teléfono de contacto del usuario. Puede ser nulo.
 * @property photoUrl La URL de la foto de perfil del usuario. Puede ser nulo.
 * @property role El rol del usuario en el sistema (ej. ADMIN, WORKER), que determina sus permisos.
 * @property position El cargo o puesto que ocupa el usuario en la empresa (ej. "Jefe de Obra", "Eléctrico").
 * @property area El área o departamento al que pertenece el usuario (ej. "Construcción", "Administración").
 * @property contractStartDate La fecha de inicio del contrato del usuario.
 * @property contractEndDate La fecha de finalización del contrato del usuario. Es nulo si el contrato es indefinido.
 * @property assignedVehicleId El ID del vehículo asignado al usuario. Es nulo si no tiene uno.
 * @property assignedPhoneId El ID del teléfono móvil de empresa asignado al usuario. Es nulo si no tiene uno.
 * @property assignedPcId El ID del ordenador o equipo informático asignado al usuario. Es nulo si no tiene uno.
 * @property fcmToken El token de Firebase Cloud Messaging para enviar notificaciones push al dispositivo del usuario. Es nulo si no se ha registrado.
 * @property tokenUpdatedAt La fecha y hora de la última actualización del `fcmToken`.
 */
data class User(
    val uid: String,
    val name: String,
    val email: String,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val role: Role,
    val position: String,
    val area: String,
    val contractStartDate: Date,
    val contractEndDate: Date? = null,
    val assignedVehicleId: String? = null,
    val assignedPhoneId: String? = null,
    val assignedPcId: String? = null,
    val fcmToken: String? = null,
    val tokenUpdatedAt: Timestamp? = null
)