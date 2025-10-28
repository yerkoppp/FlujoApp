package dev.ycosorio.flujo.ui.screens.admin.users.usermanagament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de gestión de usuarios del administrador.
 * Se encarga de obtener la lista de trabajadores y manejar el estado de la UI.
 *
 * @property userRepository El repositorio para acceder a los datos de los usuarios.
 */
@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // Flujo de estado privado para la lista de usuarios.
    private val _usersState = MutableStateFlow<Resource<List<User>>>(Resource.Loading())
    // Flujo público e inmutable que la UI observará.
    val usersState = _usersState.asStateFlow()

    init {
        // Carga los trabajadores tan pronto como el ViewModel es creado.
        loadWorkers()
    }

    /**
     * Carga la lista de todos los trabajadores desde el repositorio.
     * Escucha el Flow y actualiza el estado de la UI (_usersState) con los resultados.
     */
    private fun loadWorkers() {
        viewModelScope.launch {
            userRepository.getAllWorkers().collect { userList ->
                // Cuando el Flow emite una nueva lista, la envolvemos en un Resource.Success
                // y actualizamos nuestro estado.
                _usersState.value = Resource.Success(userList)
            }
        }
    }
}