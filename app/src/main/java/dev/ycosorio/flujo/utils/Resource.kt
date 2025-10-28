package dev.ycosorio.flujo.utils

/**
 * Una clase sellada para encapsular los resultados de las operaciones de datos.
 * Nos permite manejar los estados de Carga, Éxito y Error de forma explícita.
 * @param T El tipo de dato que se devolverá en caso de éxito.
 * @param message Un mensaje de error opcional.
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    /**
     * Representa un estado de éxito con los datos obtenidos.
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Representa un estado de error con un mensaje descriptivo para el usuario.
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * Representa un estado de carga, por ejemplo, mientras se espera la respuesta de la red.
     */
    class Loading<T> : Resource<T>()

    /**
     * Representa un estado inactivo, antes de que se inicie cualquier operación.
     */
    class Idle<T> : Resource<T>()
}