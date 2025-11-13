package dev.ycosorio.flujo.domain.repository

import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Define el contrato para las operaciones de datos relacionadas con el usuario.
 * Esta interfaz es la única puerta de entrada a los datos de usuario para la capa de negocio (domain).
 */
interface UserRepository {

    /**
     * Obtiene todos los usuarios con el rol de TRABAJADOR en tiempo real.
     * @return Un Flow que emite la lista de trabajadores cada vez que hay un cambio.
     */
    fun getAllWorkers(): Flow<List<User>>

    /**
     * Obtiene todos los usuarios que coinciden con un cargo específico en tiempo real.
     * @param position El cargo a filtrar, ej: "Técnico de Campo".
     * @return Un Flow que emite la lista de trabajadores filtrada.
     */
    fun getUsersByPosition(position: String): Flow<List<User>>

    /**
     * Obtiene los datos de un usuario específico por su ID.
     * @param uid El ID único del usuario a buscar.
     * @return Un Resource que contendrá el User en caso de éxito o un mensaje de error.
     */
    suspend fun getUser(uid: String): Resource<User>

    /**
     * Crea un nuevo usuario en el sistema (función de administrador).
     * @param user El objeto User con los datos del nuevo trabajador.
     * @return Un Resource que indica si la operación fue exitosa o falló.
     */
    suspend fun createUser(user: User): Resource<Unit>

    /**
     * Actualiza los datos de un usuario existente.
     * @param user El objeto User con los datos actualizados.
     * @return Un Resource que indica si la operación fue exitosa o falló.
     */
    suspend fun updateUser(user: User): Resource<Unit>

    /**
     * Elimina a un usuario del sistema (función de administrador).
     * @param uid El ID único del usuario a eliminar.
     * @return Un Resource que indica si la operación fue exitosa o falló.
     */
    suspend fun deleteUser(uid: String): Resource<Unit>

    /**
     * Obtiene un usuario por su email (usado para vincular con Firebase Auth).
     */
    suspend fun getUserByEmail(email: String): Resource<User>

    /**
     * Obtiene un usuario por su ID en tiempo real.
     */
    fun getUserById(uid: String): Flow<Resource<User>>

    /**
     * Obtiene una lista de usuarios según su rol.
     * @param role El rol de los usuarios a obtener.
     * @return Un Flow que emite un Resource con la lista de usuarios.
     */
    fun getUsersByRole(role: Role): Flow<Resource<List<User>>>
}