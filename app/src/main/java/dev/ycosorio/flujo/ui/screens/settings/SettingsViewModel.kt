package dev.ycosorio.flujo.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _userState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val userState = _userState.asStateFlow()

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
}