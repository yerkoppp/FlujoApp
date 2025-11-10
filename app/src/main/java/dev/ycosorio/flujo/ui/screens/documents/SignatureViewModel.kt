package dev.ycosorio.flujo.ui.screens.documents

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.DocumentRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class SignatureViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val storage: FirebaseStorage,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val assignmentId: String = checkNotNull(savedStateHandle["assignmentId"])

    private val _signatureState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val signatureState = _signatureState.asStateFlow()

    private val _captureError = MutableStateFlow<String?>(null)
    val captureError = _captureError.asStateFlow()

    fun saveSignature(bitmap: Bitmap) {
        viewModelScope.launch {
            _signatureState.value = Resource.Loading()
            Log.d("SignatureViewModel", "🎯 Iniciando guardado de firma para assignment: $assignmentId")

            try {
                // Obtener el UID del usuario actual
                val userId = authRepository.getCurrentUser()?.uid
                    ?: throw IllegalStateException("Usuario no autenticado")
                // 1. Convertir el Bitmap a bytes
                // 1. Convertir el Bitmap a bytes
                Log.d("SignatureViewModel", "📸 Convirtiendo bitmap a bytes...")

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val data = baos.toByteArray()
                Log.d("SignatureViewModel", "✅ Bitmap convertido: ${data.size} bytes")


                // 2. Definir dónde se guardará en Storage
                val signatureRef = storage.reference.child("signatures/${userId}/${UUID.randomUUID()}.png")
                Log.d("SignatureViewModel", "📁 Ruta en Storage: ${signatureRef.path}")

                // 3. Subir los bytes
                Log.d("SignatureViewModel", "⬆️ Subiendo a Firebase Storage...")

                val uploadTask = signatureRef.putBytes(data).await()
                Log.d("SignatureViewModel", "✅ Subida completada")

                Log.d("SignatureViewModel", "🔗 Obteniendo URL de descarga...")

                val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
                Log.d("SignatureViewModel", "✅ URL obtenida: $downloadUrl")

                // 4. Guardar la URL en Firestore a través del repositorio
                Log.d("SignatureViewModel", "💾 Guardando en Firestore...")
                val result = documentRepository.markDocumentAsSigned(assignmentId, downloadUrl)
                Log.d("SignatureViewModel", "📊 Resultado del repositorio: $result")

                _signatureState.value = result
                Log.d("SignatureViewModel", "✅ Estado actualizado a: $result")

            } catch (e: Exception) {
                Log.e("SignatureViewModel", "❌ Error al guardar la firma", e)

                _signatureState.value = Resource.Error(e.localizedMessage ?: "Error al guardar la firma")
            }
        }
    }

    fun setCaptureError(message: String) {
        _captureError.value = message
    }

    fun clearCaptureError() {
        _captureError.value = null
    }
}