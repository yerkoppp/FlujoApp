package dev.ycosorio.flujo.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import dev.ycosorio.flujo.domain.model.AuthUser
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
    fun verifyUserAccess(authUser: AuthUser) {
        val email = authUser.email?.trim()?.lowercase()

        // Si es el mismo email que acabamos de verificar, no verificar de nuevo
        if (email == lastVerifiedEmail && _verificationState.value is Resource.Success) {
            Log.d("AccessVerificationVM", "⚡ Usando resultado en caché para: $email")
            return
        }
        viewModelScope.launch {
            try{
                Log.d("AccessVerificationVM", "🔍 Iniciando verificación: $email")
                _verificationState.value = Resource.Loading()

                if (email.isNullOrBlank()) {
                    Log.e("AccessVerificationVM", "❌ Email vacío o nulo")
                    _verificationState.value = Resource.Error("Email no disponible")
                    lastVerifiedEmail = null
                    return@launch
                }
                val uid = authUser.uid
                // Buscar por id
                val result = withTimeout(15_000L) {
                    userRepository.getUser(uid)
                }

                when (result) {
                    is Resource.Success -> {
                        if (result.data != null) {
                        Log.d("AccessVerificationVM", "✅ Usuario encontrado: ${result.data?.name}")
                        lastVerifiedEmail = email
                            _verificationState.value = result
                        } else {
                            // --- Caso 2: Usuario NUEVO (data es null) ---
                            // ¡Este es el momento de provisionar!
                            Log.i("AccessVerificationVM", "ℹ️ Usuario no encontrado. Intentando provisionar...")
                            provisionNewUser(uid, email)
                        }
                    }
                    is Resource.Error -> {
                        Log.e("AccessVerificationVM", "❌ Error: ${result.message}")
                        lastVerifiedEmail = null
                        _verificationState.value = result
                    }
                    else -> {
                        Log.w("AccessVerificationVM", "⚠️ Estado inesperado")
                        lastVerifiedEmail = null
                        _verificationState.value = result
                    }
                }

            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AccessVerificationVM", "⏱️ Timeout al verificar usuario")
                lastVerifiedEmail = null
                _verificationState.value = Resource.Error(
                    "No se pudo verificar tu acceso. Verifica tu conexión a internet."
                )
            } catch (e: Exception) {
                Log.e("AccessVerificationVM", "💥 Excepción: ${e.message}", e)
                lastVerifiedEmail = null
                _verificationState.value = Resource.Error(
                    "Error de conexión: ${e.localizedMessage}"
                )
            }
        }
    }

    // Función para llamar a la Cloud Function de provisión
    private fun provisionNewUser(uid: String, emailForCache: String?) {
        Log.d("AccessVerificationVM", "📤 Llamando a 'provisionUserAccount'...")
        _verificationState.value = Resource.Loading() // Mantener el estado de carga

        // Esta función no necesita enviar 'data' porque el backend
        // ya sabe quiénes somos por el token de autenticación (request.auth)
        functions.getHttpsCallable("provisionUserAccount")
            .call()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // ¡ÉXITO! La función encontró la invitación y creó el documento en 'users'.
                    val message = (task.result?.data as? Map<String, Any>)?.get("message") as? String
                    Log.d("AccessVerificationVM", "✅ Provisión exitosa: $message")

                    // Ahora que el usuario existe, volvemos a buscar sus datos
                    viewModelScope.launch {
                        refetchProvisionedUser(uid, emailForCache)
                    }
                } else {
                    // ¡FALLO! La función falló (ej: no encontró invitación)
                    lastVerifiedEmail = null
                    val exception = task.exception
                    Log.e("AccessVerificationVM", "❌ Error en provisión", exception)

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

    // CAMBIO: Nueva función de ayuda para volver a buscar al usuario después de provisionar
    private suspend fun refetchProvisionedUser(uid: String, emailForCache: String?) {
        try {
            Log.d("AccessVerificationVM", "🔄 Volviendo a buscar al usuario provisionado...")
            val newUserResult = withTimeout(10_000L) {
                userRepository.getUser(uid)
            }

            if (newUserResult is Resource.Success && newUserResult.data != null) {
                Log.d("AccessVerificationVM", "🎉 ¡Usuario provisionado y cargado! ${newUserResult.data.name}")
                lastVerifiedEmail = emailForCache
                _verificationState.value = newUserResult // ¡Ahora sí! Resource.Success(user)
            } else {
                Log.e("AccessVerificationVM", "🚨 ¡Falló la re-búsqueda después de provisión! Esto no debería pasar.")
                lastVerifiedEmail = null
                _verificationState.value = Resource.Error("Error al cargar tu cuenta después de la activación. Intenta reiniciar la app.")
            }
        } catch (e: Exception) {
            Log.e("AccessVerificationVM", "💥 Excepción en re-búsqueda: ${e.message}", e)
            lastVerifiedEmail = null
            _verificationState.value = Resource.Error(e.localizedMessage ?: "Error al cargar cuenta.")
        }
    }

    /**
     * Resetea el estado de verificación (llamar al volver a la pantalla de login)
     */
    fun resetVerification() {
        Log.d("AccessVerificationVM", "🔄 Reseteando verificación")
        _verificationState.value = Resource.Idle()
        lastVerifiedEmail = null
    }

}