package dev.ycosorio.flujo.domain.model

/**
 * Representa a un usuario autenticado en el sistema.
 *
 * Esta clase de datos contiene la información básica de un usuario después de un
 * proceso de autenticación exitoso.
 *
 * @property uid El identificador único del usuario.
 * @property email La dirección de correo electrónico del usuario. Puede ser nulo si no está disponible.
 * @property displayName El nombre para mostrar del usuario. Puede ser nulo si no está configurado.
 * @property photoUrl La URL de la foto de perfil del usuario. Puede ser nulo si no está disponible.
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)