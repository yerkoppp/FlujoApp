package dev.ycosorio.flujo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.AuthUser
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile = _currentUserProfile.asStateFlow()

    val currentUser = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )



    /**
     * Verifica si el usuario autenticado existe en Firestore.
     * Si NO existe, significa que NO está autorizado.
     */
    fun checkUserExists(authUser: AuthUser, onAuthorized: (User) -> Unit, onUnauthorized: () -> Unit) {
        viewModelScope.launch {
            when (val result = userRepository.getUser(authUser.uid)) {
                is Resource.Success -> {
                    // ✅ Usuario existe en Firestore → Autorizado
                    result.data?.let {
                        _currentUserProfile.value = it // Cache del usuario
                        onAuthorized(it)
                    }
                }
                is Resource.Error -> {
                    // ❌ Usuario NO existe en Firestore → No autorizado
                    onUnauthorized()
                }
                else -> {}
            }
        }
    }
}