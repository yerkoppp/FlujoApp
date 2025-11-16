package dev.ycosorio.flujo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.AuthUser
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile = _currentUserProfile.asStateFlow()

    // Evento para abrir notificaciones
    private val _shouldOpenNotifications = MutableStateFlow(false)
    val shouldOpenNotifications = _shouldOpenNotifications.asStateFlow()

    fun triggerOpenNotifications() {
        _shouldOpenNotifications.value = true
    }

    fun resetNotificationsFlag() {
        _shouldOpenNotifications.value = false
    }
    val currentUser = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Limpia el perfil de usuario actual (llamar al cerrar sesión)
     */
    fun clearUserProfile() {
        Timber.d("🧹 Limpiando perfil de usuario")
        _currentUserProfile.value = null
    }

    /**
     * Verifica si el usuario autenticado existe en Firestore.
     * Si NO existe, significa que NO está autorizado.
     */
    fun checkUserExists(
        authUser: AuthUser,
        onAuthorized: (User) -> Unit,
        onUnauthorized: () -> Unit
    ) {
        viewModelScope.launch {
            Timber.d("🔍 Verificando usuario: ${authUser.email}")

            when (val result = userRepository.getUserByEmail(authUser.email ?: "")) {
                is Resource.Success -> {
                    // ✅ Usuario existe en Firestore → Autorizado
                    result.data?.let { user ->
                        Timber.d("✅ Usuario autorizado: ${user.name}")
                        _currentUserProfile.value = user // Cache del usuario
                        onAuthorized(user)
                    }
                }
                is Resource.Error -> {
                    Timber.e("❌ Error al verificar usuario: ${result.message}")

                    // ❌ Usuario NO existe en Firestore → No autorizado
                    onUnauthorized()
                }
                else -> {
                    Timber.w("⏳ Estado inesperado")
                }
            }
        }
    }
}