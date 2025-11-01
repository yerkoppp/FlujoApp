package dev.ycosorio.flujo.ui.screens.auth

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
import javax.inject.Inject

@HiltViewModel
class AccessVerificationViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _verificationState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val verificationState = _verificationState.asStateFlow()

    fun verifyUserAccess(authUser: AuthUser) {
        viewModelScope.launch {
            _verificationState.value = Resource.Loading()

            // Buscar por email
            val result = if (authUser.email != null) {
                userRepository.getUserByEmail(authUser.email)
            } else {
                Resource.Error("Email no disponible")
            }

            _verificationState.value = result
        }
    }
}