package dev.ycosorio.flujo.ui.screens.admin.users.adduser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.utils.Resource
import dev.ycosorio.flujo.utils.isValidEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AddUserViewModel @Inject constructor(
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _addUserState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val addUserState = _addUserState.asStateFlow()

    fun createUser(name: String, email: String, position: String, area: String) {
        viewModelScope.launch {
            // Validaciones
            if (name.isBlank()) {
                _addUserState.value = Resource.Error("El nombre es obligatorio.")
                return@launch
            }
            if (email.isBlank() || !email.isValidEmail()) {
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

            try {
                Timber.d("📤 Llamando a Cloud Function...")

                // Formatear fecha en ISO 8601 para compatibilidad con backend
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val contractStartDate = dateFormatter.format(Date())

                val data = hashMapOf(
                    "email" to email.trim().lowercase(),
                    "name" to name,
                    "position" to position,
                    "area" to area,
                    "contractStartDate" to contractStartDate
                )

                val result = functions
                    .getHttpsCallable("createWorker")
                    .call(data)
                    .await()

                Timber.d("✅ Respuesta: ${result.data}")
                _addUserState.value = Resource.Success(Unit)

            } catch (e: com.google.firebase.functions.FirebaseFunctionsException) {
                Timber.e(e, "❌ Error de Function: ${e.code}")

                val errorMessage = when (e.code) {
                    com.google.firebase.functions.FirebaseFunctionsException.Code.ALREADY_EXISTS ->
                        "Ya existe un usuario con este email"
                    com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                        "No tienes permisos para crear usuarios"
                    com.google.firebase.functions.FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                        "Datos inválidos: ${e.message}"
                    else -> "Error al crear el trabajador: ${e.message}"
                }

                _addUserState.value = Resource.Error(errorMessage)

            } catch (e: Exception) {
                Timber.e(e, "❌ Error general")
                _addUserState.value = Resource.Error(
                    e.localizedMessage ?: "Error inesperado al crear el trabajador"
                )
            }
        }
    }

    fun resetState() {
        _addUserState.value = Resource.Idle()
    }
}