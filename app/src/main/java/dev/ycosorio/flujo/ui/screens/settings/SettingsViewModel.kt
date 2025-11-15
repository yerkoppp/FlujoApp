package dev.ycosorio.flujo.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.ui.AppViewModel
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _userState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val userState = _userState.asStateFlow()

    // Estado para controlar el proceso de logout
    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState = _logoutState.asStateFlow()

    init {
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            loadCurrentUser(uid)
        } else {
            _userState.value = Resource.Error("Usuario no autenticado.")
        }
    }

    private fun loadCurrentUser(uid: String) {
        viewModelScope.launch {
            _userState.value = userRepository.getUser(uid)
        }
    }

    fun performLogout(context: Context, appViewModel: AppViewModel) {
        viewModelScope.launch {
            try {
                Timber.d("🚪 Iniciando cierre de sesión")
                _logoutState.value = LogoutState.Loading

                // 1. Limpiar caché de repositorios
                if (userRepository is dev.ycosorio.flujo.data.repository.UserRepositoryImpl) {
                    userRepository.clearCache()
                }

                // 2. Dar tiempo a que se cancelen listeners
                delay(500)

                // 3. Limpiar el perfil de usuario
                appViewModel.clearUserProfile()

                // 4. Cerrar sesión en Firebase
                AuthUI.getInstance().signOut(context).await()

                Timber.d("✅ Sesión cerrada exitosamente")
                _logoutState.value = LogoutState.Success

            } catch (e: Exception) {
                Timber.e(e, "❌ Error al cerrar sesión")
                _logoutState.value = LogoutState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

// ✅ Estados del proceso de logout
sealed class LogoutState {
    object Idle : LogoutState()
    object Loading : LogoutState()
    object Success : LogoutState()
    data class Error(val message: String) : LogoutState()
}