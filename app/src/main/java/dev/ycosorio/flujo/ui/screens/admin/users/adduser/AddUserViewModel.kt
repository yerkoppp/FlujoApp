package dev.ycosorio.flujo.ui.screens.admin.users.adduser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddUserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _addUserState =
        MutableStateFlow<Resource<Unit>>(Resource.Idle()) // Inicia en éxito (inactivo)
    val addUserState = _addUserState.asStateFlow()

    fun createUser(name: String, email: String, position: String, area: String) {
        viewModelScope.launch {
            // Validación básica
            if (name.isBlank()) {
                _addUserState.value = Resource.Error("El nombre es obligatorio.")
                return@launch
            }
            if (email.isBlank() || !email.contains("@")) {
                _addUserState.value = Resource.Error("El email es obligatorio y debe ser válido.")
                return@launch
            }
            if (position.isBlank()) {
                _addUserState.value = Resource.Error("El cargo es obligatorio.")
                return@launch
            }
            if (area.isBlank()) {
                _addUserState.value = Resource.Error("El área es obligatoria.")
                return@launch
            }

            _addUserState.value = Resource.Loading()

            val newUser = User(
                uid = "user_${UUID.randomUUID()}",
                name = name,
                email = email,
                position = position,
                area = area,
                role = Role.TRABAJADOR,
                contractStartDate = Date()
            )

            // El resultado de la operación del repositorio actualizará el estado
            _addUserState.value = userRepository.createUser(newUser)
        }
    }
}