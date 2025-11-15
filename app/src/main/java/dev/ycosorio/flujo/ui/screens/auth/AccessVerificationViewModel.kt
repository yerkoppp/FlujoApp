package dev.ycosorio.flujo.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import dev.ycosorio.flujo.domain.model.AuthUser
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccessVerificationViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _verificationState = MutableStateFlow<Resource<User>>(Resource.Idle())
    val verificationState = _verificationState.asStateFlow()

    // Cache del último email verificado para evitar verificaciones duplicadas
    private var lastVerifiedEmail: String? = null

    /**
     * Mutex para evitar verificaciones concurrentes
     */
    private val verificationMutex = Mutex()

    /**
     * Función de ayuda para reintentos con backoff exponencial
     */
    private suspend fun <T> retryWithExponentialBackoff(
        maxAttempts: Int = 3,
        initialDelay: Long = 2000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Timber.w("Intento ${attempt + 1} falló, reintentando en ${currentDelay}ms")
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block() // Último intento
    }

    fun verifyUserAccess(authUser: AuthUser) {
        viewModelScope.launch {
            verificationMutex.withLock {
                val email = authUser.email?.trim()?.lowercase()

                // Si es el mismo email que acabamos de verificar, no verificar de nuevo
                if (email == lastVerifiedEmail && _verificationState.value is Resource.Success) {
                    Timber.d("⚡ Usando resultado en caché para: $email")
                    return@withLock
                }
                try{
                    Timber.d("🔍 Iniciando verificación: $email")
                    _verificationState.value = Resource.Loading()

                    if (email.isNullOrBlank()) {
                        Timber.e("❌ Email vacío o nulo")
                        _verificationState.value = Resource.Error("Email no disponible")
                        lastVerifiedEmail = null
                        return@launch
                    }
                    val uid = authUser.uid
                    // Buscar por id
                    val result = retryWithExponentialBackoff(
                        maxAttempts = 3,
                        initialDelay = 2000L
                    ) {
                        withTimeout(30_000L) {
                            userRepository.getUser(uid)
                        }
                    }

                    when (result) {
                        is Resource.Success -> {
                            if (result.data != null) {
                                Timber.d("✅ Usuario encontrado: ${result.data?.name}")
                                lastVerifiedEmail = email
                                _verificationState.value = result
                            } else {
                                // --- Caso 2: Usuario NUEVO (data es null) ---
                                // ¡Este es el momento de provisionar!
                                Timber.i("ℹ️ Usuario no encontrado. Intentando provisionar...")
                                provisionNewUser(uid, email)
                            }
                        }
                        is Resource.Error -> {
                            Timber.e("❌ Error: ${result.message}")
                            lastVerifiedEmail = null
                            _verificationState.value = result
                        }
                        else -> {
                            Timber.w("⚠️ Estado inesperado")
                            lastVerifiedEmail = null
                            _verificationState.value = result
                        }
                    }

                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Timber.e("⏱️ Timeout al verificar usuario")
                    lastVerifiedEmail = null
                    _verificationState.value = Resource.Error(
                        "No se pudo verificar tu acceso. Verifica tu conexión a internet."
                    )
                } catch (e: Exception) {
                    Timber.e(e, "💥 Excepción: ${e.message}")
                    lastVerifiedEmail = null
                    _verificationState.value = Resource.Error(
                        "Error de conexión: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Provisión de una nueva cuenta de usuario llamando a la función en la nube.
     */
    private fun provisionNewUser(uid: String, emailForCache: String?) {
        Timber.d("📤 Llamando a 'provisionUserAccount'...")
        _verificationState.value = Resource.Loading() // Mantener el estado de carga

        // Esta función no necesita enviar 'data' porque el backend
        // ya sabe quiénes somos por el token de autenticación (request.auth)
        functions.getHttpsCallable("provisionUserAccount")
            .call()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // ¡ÉXITO! La función encontró la invitación y creó el documento en 'users'.
                    val message = (task.result?.data as? Map<String, Any>)?.get("message") as? String
                    Timber.d("✅ Provisión exitosa: $message")

                    // Ahora que el usuario existe, volvemos a buscar sus datos
                    viewModelScope.launch {
                        refetchProvisionedUser(uid, emailForCache)
                    }
                } else {
                    // ¡FALLO! La función falló (ej: no encontró invitación)
                    lastVerifiedEmail = null
                    val exception = task.exception
                    Timber.e(exception, "❌ Error en provisión")

                    // Traducir el error para el usuario
                    val errorMessage = if (exception is FirebaseFunctionsException) {
                        when (exception.code) {
                            FirebaseFunctionsException.Code.NOT_FOUND ->
                                "No se encontró una invitación para tu email. Contacta a un administrador."
                            FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                                "Error de autenticación. Intenta iniciar sesión de nuevo."
                            else ->
                                exception.message ?: "Error al activar tu cuenta."
                        }
                    } else {
                        exception?.localizedMessage ?: "Error desconocido al activar la cuenta."
                    }
                    _verificationState.value = Resource.Error(errorMessage)
                }
            }
    }

    /**
     * Vuelve a buscar al usuario provisionado después de llamar a la función en la nube.
     */
    private suspend fun refetchProvisionedUser(uid: String, emailForCache: String?) {
        try {
            Timber.d("🔄 Volviendo a buscar al usuario provisionado...")
            val newUserResult = retryWithExponentialBackoff(
                maxAttempts = 3,
                initialDelay = 2000L
            ) {
                withTimeout(30_000L) {
                    userRepository.getUser(uid)
                }
            }

            if (newUserResult is Resource.Success && newUserResult.data != null) {
                Timber.d("🎉 ¡Usuario provisionado y cargado! ${newUserResult.data.name}")
                lastVerifiedEmail = emailForCache
                _verificationState.value = newUserResult // ¡Ahora sí! Resource.Success(user)
            } else {
                Timber.e("🚨 ¡Falló la re-búsqueda después de provisión! Esto no debería pasar.")
                lastVerifiedEmail = null
                _verificationState.value = Resource.Error("Error al cargar tu cuenta después de la activación. Intenta reiniciar la app.")
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 Excepción en re-búsqueda: ${e.message}")
            lastVerifiedEmail = null
            _verificationState.value = Resource.Error(e.localizedMessage ?: "Error al cargar cuenta.")
        }
    }

    /**
     * Resetea el estado de verificación (llamar al volver a la pantalla de login)
     */
    fun resetVerification() {
        Timber.d("🔄 Reseteando verificación")
        _verificationState.value = Resource.Idle()
        lastVerifiedEmail = null
    }

}