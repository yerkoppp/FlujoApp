package dev.ycosorio.flujo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.utils.Resource
import dev.ycosorio.flujo.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.withTimeout

/**
 * Implementación del UserRepository que se comunica con Firebase Firestore.
 * Esta clase contiene la lógica para leer y escribir datos de usuario en la nube,
 * implementando el contrato definido en la interfaz UserRepository.
 *
 * @property firestore Instancia de FirebaseFirestore inyectada para la comunicación con la base de datos.
 */
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore // Inyectaremos la instancia de Firestore aquí
) : UserRepository {

    /**
     * Referencia a la colección "users" en Firestore para reutilizarla en las funciones.
     */
    private val usersCollection = firestore.collection("users")

    /**
     * Obtiene todos los usuarios con el rol de TRABAJADOR en tiempo real.
     * @return Un Flow que emite la lista de trabajadores cada vez que hay un cambio en Firestore.
     */
    override fun getAllWorkers(): Flow<List<User>> = callbackFlow {
        // Creamos una consulta a Firestore para obtener solo los usuarios con el rol de TRABAJADOR
        val query = usersCollection.whereEqualTo("role", Role.TRABAJADOR.name)

        // addSnapshotListener es el listener de Firestore que se dispara cada vez que hay un cambio.
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Si hay un error, lo cerramos y notificamos al flow.
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Convertimos todos los documentos recibidos a una lista de Users.
                val userList = snapshot.documents.mapNotNull { it.toUser() }
                // Emitimos la nueva lista al flow.
                trySend(userList)
            }
        }

        // Este bloque se ejecuta cuando el flow ya no es necesario (ej: el usuario sale de la pantalla).
        // Es crucial para detener el listener y evitar fugas de memoria.
        awaitClose { subscription.remove() }
    }

    /**
     * Obtiene todos los usuarios que coinciden con un cargo específico en tiempo real.
     * @param position El cargo a filtrar, ej: "Técnico de Campo".
     * @return Un Flow que emite la lista de trabajadores filtrada y se actualiza con los cambios.
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
                close(error)
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
     * Obtiene los datos de un usuario específico por su ID.
     * @param uid El ID único del usuario a buscar (coincide con el ID del documento en Firestore).
     * @return El objeto User si se encuentra, o null si no existe el documento.
     */
    override suspend fun getUser(uid: String): Resource<User> {
        return try {
            val document = usersCollection.document(uid).get().await()
            val user = document.toUser() // Usamos nuestra función traductora
            if(user != null){
                Log.d("UserRepository", "Usuario $uid cargado exitosamente.") // <-- AÑADE ESTA LÍNEA
                Resource.Success(user)
            } else {
                Log.e("UserRepository", "Error al mapear: toUser() devolvió null para $uid. ¿Datos incompletos en Firestore?") // <-- AÑADE ESTA LÍNEA
                Resource.Error("No se pudieron encontrar los datos del usuario.")
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e("UserRepository", "Error de Firestore: ${e.code.name}", e) // <-- AÑADE ESTA LÍNEA
            // Errores específicos de Firebase
            when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE -> Resource.Error("No hay conexión a internet.")
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> Resource.Error("No tienes permisos para realizar esta acción.")
                else -> Resource.Error("Error inesperado: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
            // Otros errores
            Log.e("UserRepository", "Error general en getUser", e) // <-- AÑADE ESTA LÍNEA
            Resource.Error("Ocurrió un error desconocido.")
        }
    }


    /**
     * Crea un nuevo documento de usuario en Firestore.
     * El ID del documento será el UID del objeto User.
     * @param user El objeto User con los datos del nuevo trabajador.
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
                .set(user.toFirestoreMap())
                .await()
            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error("Error de base de datos: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Ocurrió un error inesperado al crear el usuario.")
        }
    }

    /**
     * Actualiza un documento de usuario existente en Firestore.
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
            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error("Error al actualizar la base de datos: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Ocurrió un error inesperado al actualizar el usuario.")
        }
    }

    /**
     * Elimina un documento de usuario de Firestore.
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
            usersCollection.document(uid).delete().await()
            Resource.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error("Error de base de datos al eliminar: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Ocurrió un error inesperado al eliminar el usuario.")
        }
    }

    override suspend fun getUserByEmail(email: String): Resource<User> {
        return try {
            Log.d("UserRepository", "🔍 Buscando email: $email")
            val normalizedEmail = email.trim().lowercase()
            val query = withTimeout(12_000L) {
                usersCollection
                    .whereEqualTo("email", normalizedEmail)
                    .limit(1)
                    .get()
                    .await()
            }

            Log.d("UserRepository", "📦 Documentos encontrados: ${query.documents.size}")

            if (query.documents.isEmpty()) {
                Log.w("UserRepository", "❌ Usuario no encontrado")
                Resource.Error("Usuario no registrado en el sistema.")
            } else {
                val user = query.documents.first().toUser()
                if (user != null) {
                    Log.d("UserRepository", "✅ Usuario cargado: ${user.name}")
                    Resource.Success(user)
                } else {
                    Log.e("UserRepository", "❌ Error al mapear usuario")
                    Resource.Error("Error al cargar datos del usuario.")
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e("UserRepository", "⏱️ Timeout en Firestore")
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
            Log.e("UserRepository", "💥 Error general", e)
            Resource.Error("Error desconocido: ${e.localizedMessage}")
        }
    }
}
/**
 * Función de extensión privada para convertir un DocumentSnapshot de Firestore
 * en nuestro objeto de dominio User. Actúa como un traductor.
 * @return Un objeto User si la conversión es exitosa, o null si faltan datos.
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
            assignedPcId = assignedPcId
        )
    } catch (e: Exception) {
        // Si algún campo obligatorio es nulo o el tipo es incorrecto, la conversión falla.
        // Puedes añadir un log aquí: Log.e("FirestoreMapper", "Error mapping user", e)
        null
    }
}

/**
 * Función de extensión privada para convertir nuestro objeto de dominio User
 * en un Map que Firestore pueda entender y almacenar.
 * @return Un Map<String, Any> listo para ser guardado en Firestore.
 */
private fun User.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "uid" to uid,
        "name" to name,
        "email" to email,
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
        "assignedPcId" to assignedPcId
    )
}
