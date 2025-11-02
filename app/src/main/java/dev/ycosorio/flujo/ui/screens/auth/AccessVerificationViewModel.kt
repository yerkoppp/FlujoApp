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

    private val _verificationState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val verificationState = _verificationState.asStateFlow()

    fun verifyUserAccess(authUser: AuthUser) {
        viewModelScope.launch {
            try{
                Log.d("AccessVerificationVM", "🔍 Buscando usuario: ${authUser.email}")
                _verificationState.value = Resource.Loading()

                // Buscar por email
                val result = withTimeout(15_000L) {
                    if (authUser.email != null) {
                        val normalizedEmail = authUser.email.trim().lowercase()
                        Log.d("AccessVerificationVM", "Email normalizado: $normalizedEmail")

                        userRepository.getUserByEmail(normalizedEmail)
                    } else {
                        Log.e("AccessVerificationVM", "❌ Email es null")
                        Resource.Error("Email no disponible")
                    }
                }

                when (result) {
                    is Resource.Success -> {
                        Log.d("AccessVerificationVM", "✅ Usuario encontrado: ${result.data?.name}")
                    }
                    is Resource.Error -> {
                        Log.e("AccessVerificationVM", "❌ Error: ${result.message}")
                    }
                    else -> {}
                }
                _verificationState.value = result
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AccessVerificationVM", "⏱️ Timeout al verificar usuario")
                _verificationState.value = Resource.Error(
                    "No se pudo verificar tu acceso. Verifica tu conexión a internet."
                )
            } catch (e: Exception) {
                Log.e("AccessVerificationVM", "💥 Excepción: ${e.message}", e)
                _verificationState.value = Resource.Error(
                    "Error de conexión: ${e.message}"
                )
            }
        }
    }
}