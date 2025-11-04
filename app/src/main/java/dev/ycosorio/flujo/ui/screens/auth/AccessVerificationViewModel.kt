package dev.ycosorio.flujo.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val userRepository: UserRepository
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
                        Log.d("AccessVerificationVM", "✅ Usuario encontrado: ${result.data?.name}")
                        lastVerifiedEmail = email
                    }
                    is Resource.Error -> {
                        Log.e("AccessVerificationVM", "❌ Error: ${result.message}")
                        lastVerifiedEmail = null
                    }
                    else -> {
                        Log.w("AccessVerificationVM", "⚠️ Estado inesperado")
                        lastVerifiedEmail = null
                    }
                }
                _verificationState.value = result

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

    /**
     * Resetea el estado de verificación (llamar al volver a la pantalla de login)
     */
    fun resetVerification() {
        Log.d("AccessVerificationVM", "🔄 Reseteando verificación")
        _verificationState.value = Resource.Idle()
        lastVerifiedEmail = null
    }

}