package dev.ycosorio.flujo.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import dev.ycosorio.flujo.utils.SimulationAuth
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

    // A futuro, este ID vendrá del servicio de autenticación de Firebase.
    private val _currentUserId = SimulationAuth.currentUserId

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { authUser ->
                //Busca usuario por email
                authUser?.let { loadCurrentUser(it.email ?: "") }
            }
        }
    }

    private fun loadCurrentUser(email: String) {
        viewModelScope.launch {

            _userState.value = Resource.Loading()
            _userState.value = userRepository.getUserByEmail(email)

        }
    }
}