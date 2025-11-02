package dev.ycosorio.flujo.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val userState = _userState.asStateFlow()

    fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.currentUser.collect { authUser ->
                if (authUser != null) {
                    Log.d("DashboardViewModel", "🔍 Cargando usuario: ${authUser.email}")
                    _userState.value = Resource.Loading()
                    _userState.value = userRepository.getUserByEmail(authUser.email ?: "")
                } else {
                    Log.w("DashboardViewModel", "⚠️ No hay usuario autenticado")
                    _userState.value = Resource.Error("No hay usuario autenticado")
                }
            }
        }
    }
}