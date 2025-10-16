package dev.ycosorio.flujo.ui.screens.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddUserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _addUserState = MutableStateFlow<Resource<Unit>>(Resource.Success(Unit)) // Inicia en éxito (inactivo)
    val addUserState = _addUserState.asStateFlow()

    fun createUser(name: String, email: String, position: String, area: String) {
        viewModelScope.launch {
            // Validación básica
            if (name.isBlank() || email.isBlank() || position.isBlank() || area.isBlank()) {
                _addUserState.value = Resource.Error("Todos los campos son obligatorios.")
                return@launch
            }

            _addUserState.value = Resource.Loading()

            val newUser = User(
                uid = "", // El repositorio se encargará de esto con Auth
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