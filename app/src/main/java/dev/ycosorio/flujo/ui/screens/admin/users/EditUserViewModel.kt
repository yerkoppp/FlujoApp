package dev.ycosorio.flujo.ui.screens.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import dev.ycosorio.flujo.utils.isValidEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

@HiltViewModel
class EditUserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _updateUserState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val updateUserState = _updateUserState.asStateFlow()

    private val _deleteUserState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val deleteUserState = _deleteUserState.asStateFlow()

    fun updateUser(user: User) {
        viewModelScope.launch {
            // Validaciones
            if (user.name.isBlank()) {
                _updateUserState.value = Resource.Error("El nombre es obligatorio.")
                return@launch
            }
            if (user.email.isBlank() || !user.email.isValidEmail()) {
                _updateUserState.value = Resource.Error("El email es obligatorio y debe ser válido.")
                return@launch
            }
            if (user.position.isBlank()) {
                _updateUserState.value = Resource.Error("El cargo es obligatorio.")
                return@launch
            }
            if (user.area.isBlank()) {
                _updateUserState.value = Resource.Error("El área es obligatoria.")
                return@launch
            }

            _updateUserState.value = Resource.Loading()
            _updateUserState.value = userRepository.updateUser(user)
        }
    }

 /*   fun deleteUser(uid: String) {
        viewModelScope.launch {
            _deleteUserState.value = Resource.Loading()
            _deleteUserState.value = userRepository.deleteUser(uid)
        }
    }*/

    fun deleteUser(uid: String) {
        viewModelScope.launch {
            _deleteUserState.value = Resource.Loading()

            try {
                val data = hashMapOf("userId" to uid)
                functions.getHttpsCallable("deleteWorker").call(data).await()
                _deleteUserState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _deleteUserState.value = Resource.Error(e.localizedMessage ?: "Error")
            }
        }
    }

    fun resetUpdateState() {
        _updateUserState.value = Resource.Idle()
    }

    fun resetDeleteState() {
        _deleteUserState.value = Resource.Idle()
    }
}