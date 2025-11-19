package dev.ycosorio.flujo.data.repository

import androidx.collection.LruCache
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementación del repositorio de usuarios utilizando Firebase Firestore.
 *
 * @property firestore La instancia de FirebaseFirestore para interactuar con la base de datos.
 */
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    /**
     * Referencia a la colección "users" en Firestore para reutilizarla en las funciones.
     */
    private val usersCollection = firestore.collection("users")

    /** Caché LRU para almacenar usuarios por email y reducir llamadas a Firestore.
     * Tamaño máximo de 50 entradas.
     */
    private val emailCache = object : LruCache<String, User>(50) {
        override fun sizeOf(key: String, value: User): Int = 1
    }

    /**
     * Obtiene todos los usuarios con el rol de TRABAJADOR en tiempo real.
     * @return Un Flow que emite la lista de trabajadores y se actualiza con los cambios.
     */
    override fun getAllWorkers(): Flow<List<User>> = callbackFlow {
        // Creamos una consulta a Firestore para obtener solo los usuarios con el rol de TRABAJADOR
        val query = usersCollection.whereEqualTo("role", Role.TRABAJADOR.name)

        // addSnapshotListener es el listener de Firestore que se dispara cada vez que hay un cambio.
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Manejar PERMISSION_DENIED sin propagar (ocurre al cerrar sesión)
                if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(emptyList())  // Enviar lista vacía
                } else {
                    Timber.e(error, "Error al obtener trabajadores: ${error.message}")
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Convertimos todos los documentos recibidos a una lista de Users.
                val userList = snapshot.documents.mapNotNull { it.toUser() }
                // Emitimos la nueva lista al flow.
                trySend(userList)
            }
        }

        awaitClose { subscription.remove() }
    }

    /**
     * Obtiene los usuarios con el rol de TRABAJADOR y una posición específica en tiempo real.
     * @param position La posición laboral a filtrar (por ejemplo, "TÉCNICO", "SUPERVISOR").
     * @return Un Flow que emite la lista de trabajadores con la posición dada y se actualiza con los cambios.
     */
    override fun getUsersByPosition(position: String): Flow<List<User>> = callbackFlow {
        // La consulta tiene DOS condiciones:
        // 1. El rol debe ser TRABAJADOR.
        // 2. El campo "position" debe ser igual al parámetro recibido.
        val query = usersCollection
            .whereEqualTo("role", Role.TRABAJADOR.name)
            .whereEqualTo("position", position)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val userList = snapshot.documents.mapNotNull { it.toUser() }
                trySend(userList)
            }
        }

        // Se cancela la suscripción cuando el Flow ya no se usa.
        awaitClose { subscription.remove() }
    }

    /**
     * Obtiene un usuario por su UID.
     * @param uid El ID único del usuario a obtener.
     * @return Un Resource que contiene el usuario si se encuentra, o un error si ocurre algún problema.
     */
    override suspend fun getUser(uid: String): Resource<User> {
        return try {
            val document = usersCollection.document(uid).get().await()
            val user = document.toUser() // Usamos nuestra función traductora
            if(user != null){
                Timber.d("Usuario $uid cargado exitosamente.")
                Resource.Success(user)
            } else {
                Timber.e("Error al mapear: toUser() devolvió null para $uid. ¿Datos incompletos en Firestore?")
                Resource.Error("No se pudieron encontrar los datos del usuario.")
            }
        } catch (e: FirebaseFirestoreException) {
            Timber.e(e, "Error de Firestore: ${e.code.name}") // <-- AÑADE ESTA LÍNEA
            // Errores específicos de Firebase
            when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE -> Resource.Error("No hay conexión a internet.")
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> Resource.Error("No tienes permisos para realizar esta acción.")
                else -> Resource.Error("Error inesperado: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
            // Otros errores
            Timber.e(e, "Error general en getUser") // <-- AÑADE ESTA LÍNEA
            Resource.Error("Ocurrió un error desconocido.")
        }
    }


    /**
     * Crea un nuevo documento de usuario en Firestore.
     * @param user El objeto User que se va a crear.
     * @return Un Result que indica si la operación fue exitosa o si ocurrió un error.
     */
    override suspend fun createUser(user: User): Resource<Unit> {
        return try {
            val userWithNormalizedEmail = user.copy(
                email = user.email.trim().lowercase()
            )
            // Usamos el uid del usuario como ID del documento para una fácil vinculación
            usersCollection
                .document(user.uid)
                .set(userWithNormalizedEmail.toFirestoreMap())
                .await()
            // Actualizar caché
            emailCache.put(userWithNormalizedEmail.email, userWithNormalizedEmail)

            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error("Error de base de datos: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Ocurrió un error inesperado al crear el usuario.")
        }
    }

    /**
     * Actualiza un documento de usuario en Firestore.
     * @param user El objeto User con los datos actualizados.
     * @return Un Result que indica si la operación fue exitosa o si ocurrió un error.
     */
    override suspend fun updateUser(user: User): Resource<Unit> {
        return try {
            val document = usersCollection.document(user.uid).get().await()
            if (!document.exists()) {
                return Resource.Error("El usuario no existe.")
            }

            usersCollection.document(user.uid).update(user.toFirestoreMap()).await()

            // Actualizar caché
            emailCache.put(user.email.lowercase(), user)
            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error("Error al actualizar la base de datos: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Ocurrió un error inesperado al actualizar el usuario.")
        }
    }

    /**
     * Elimina un documento de usuario en Firestore.
     * @param uid El ID único del usuario a eliminar.
     * @return Un Result que indica si la operación fue exitosa o si ocurrió un error.
     */
    override suspend fun deleteUser(uid: String): Resource<Unit> {
        return try {
            // Verifica que el usuario existe primero
            val document = usersCollection.document(uid).get().await()
            if (!document.exists()) {
                return Resource.Error("El usuario no existe.")
            }
            val user = document.toUser()
            usersCollection.document(uid).delete().await()
            // Limpiar caché
            user?.let { emailCache.remove(it.email.lowercase()) }

            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error("Error de base de datos al eliminar: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Ocurrió un error inesperado al eliminar el usuario.")
        }
    }

    /**
     * Obtiene un usuario por su correo electrónico.
     * @param email El correo electrónico del usuario a buscar.
     * @return Un Resource que contiene el usuario si se encuentra, o un error si ocurre algún problema.
     */
    override suspend fun getUserByEmail(email: String): Resource<User> {
        return try {
            Timber.d("🔍 Buscando email: $email")
            val normalizedEmail = email.trim().lowercase()

            // Verificar caché primero
            emailCache.get(normalizedEmail)?.let { cachedUser ->
                Timber.d("⚡ Usuario encontrado en caché: ${cachedUser.name}")
                return Resource.Success(cachedUser)
            }

            // Si no está en caché, buscar en Firestore
            val query = withTimeout(30_000L) {
                usersCollection
                    .whereEqualTo("email", normalizedEmail)
                    .limit(1)
                    .get()
                    .await()
            }

            Timber.d("📦 Documentos encontrados: ${query.documents.size}")

            if (query.documents.isEmpty()) {
                Timber.w("❌ Usuario no encontrado")
                // Limpiar entrada de caché si existía
                emailCache.remove(normalizedEmail)
                Resource.Error("Usuario no registrado en el sistema.")
            } else {
                val user = query.documents.first().toUser()
                if (user != null) {
                    Timber.d("✅ Usuario cargado: ${user.name}")
                    // Guardar en caché
                    emailCache.put(normalizedEmail, user)
                    Resource.Success(user)
                } else {
                    Timber.e("❌ Error al mapear usuario")
                    Resource.Error("Error al cargar datos del usuario.")
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Timber.e("⏱️ Timeout en Firestore")
            Resource.Error("Tiempo de espera agotado. Verifica tu conexión.")
        } catch (e: FirebaseFirestoreException) {
            when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    Resource.Error("No hay conexión a internet.")
                FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                    Resource.Error("Configuración de base de datos incompleta. Contacta al administrador.")
                else -> Resource.Error("Error: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 Error general")
            Resource.Error("Error desconocido: ${e.localizedMessage}")
        }
    }

    /**
     * Obtiene un usuario por su UID en tiempo real.
     * @param uid El ID único del usuario a obtener.
     * @return Un Flow que emite el usuario y se actualiza con los cambios.
     */
    override fun getUserById(uid: String): Flow<Resource<User>> = callbackFlow {
        val listener = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error al obtener usuario"))
                    return@addSnapshotListener
                }

                val user = snapshot?.toUser()
                if (user != null) {
                    trySend(Resource.Success(user))
                } else {
                    trySend(Resource.Error("Usuario no encontrado"))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene usuarios por su rol en tiempo real.
     * @param role El rol de los usuarios a obtener.
     * @return Un Flow que emite la lista de usuarios con el rol dado y se actualiza con los cambios.
     */
    override fun getUsersByRole(role: Role): Flow<Resource<List<User>>> = callbackFlow {
        val listener = usersCollection
            .whereEqualTo("role", role.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error al obtener usuarios"))
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { it.toUser() } ?: emptyList()
                trySend(Resource.Success(users))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Actualiza el token FCM de un usuario en Firestore.
     * @param userId El ID único del usuario.
     * @param token El nuevo token FCM a actualizar.
     * @return Un Result que indica si la operación fue exitosa o si ocurrió un error.
     */
    override suspend fun updateFCMToken(userId: String, token: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "fcmToken" to token,
                        "tokenUpdatedAt" to Timestamp.now()
                    )
                ).await()
            Timber.d("✅ Token FCM actualizado para usuario: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Limpia la caché de usuarios almacenada en memoria.
     */
    fun clearCache() {
        Timber.d("🧹 Limpiando caché de usuarios")
        emailCache.evictAll()
    }

}

/**
 * Función de extensión privada para convertir un DocumentSnapshot de Firestore
 * en nuestro objeto de dominio User.
 * @return Un objeto User o null si ocurre un error durante la conversión.
 */
private fun DocumentSnapshot.toUser(): User? {
    return try {
        val uid = getString("uid") ?: throw IllegalStateException("uid es obligatorio")
        val name = getString("name") ?: throw IllegalStateException("name es obligatorio")
        val email = getString("email") ?: throw IllegalStateException("email es obligatorio")
        val roleString = getString("role") ?: throw IllegalStateException("role es obligatorio")
        val role = Role.valueOf(roleString)
        val position = getString("position") ?: throw IllegalStateException("position es obligatorio")
        val area = getString("area") ?: throw IllegalStateException("area es obligatorio")
        // Convertimos el String del rol al enum Role
        val contractStartDate = getDate("contractStartDate") ?: throw IllegalStateException("contractStartDate es obligatorio")
        val contractEndDate = getDate("contractEndDate")
        val phoneNumber = getString("phoneNumber")
        val photoUrl = getString("photoUrl")
        val assignedVehicleId = getString("assignedVehicleId")
        val assignedPhoneId = getString("assignedPhoneId")
        val assignedPcId = getString("assignedPcId")
        val fcmToken = getString("fcmToken")
        val tokenUpdatedAt = getTimestamp("tokenUpdatedAt")

        User(
            uid = uid,
            name = name,
            email = email,
            phoneNumber = phoneNumber,
            photoUrl = photoUrl,
            role = role,
            position = position,
            area = area,
            contractStartDate = contractStartDate,
            contractEndDate = contractEndDate,
            assignedVehicleId = assignedVehicleId,
            assignedPhoneId = assignedPhoneId,
            assignedPcId = assignedPcId,
            fcmToken = fcmToken,
            tokenUpdatedAt = tokenUpdatedAt
        )
    } catch (e: Exception) {
        Timber.e(e, "Error al mapear documento ${this.id} a User. Datos: ${this.data}")
        null
    }
}


/**
 * Función de extensión privada para convertir un objeto User
 * en un Map<String, Any?> adecuado para almacenar en Firestore.
 * @return Un mapa con los campos del usuario.
 */
private fun User.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "uid" to uid,
        "name" to name,
        "email" to email.trim().lowercase(),
        "phoneNumber" to phoneNumber,
        "photoUrl" to photoUrl,
        "role" to role.name, // Guardamos el enum como un String
        "position" to position,
        "area" to area,
        "contractStartDate" to contractStartDate,
        // Manejamos los campos que pueden ser nulos
        "contractEndDate" to contractEndDate,
        "assignedVehicleId" to assignedVehicleId,
        "assignedPhoneId" to assignedPhoneId,
        "assignedPcId" to assignedPcId,
        "fcmToken" to fcmToken,
        "tokenUpdatedAt" to tokenUpdatedAt
    )
}
