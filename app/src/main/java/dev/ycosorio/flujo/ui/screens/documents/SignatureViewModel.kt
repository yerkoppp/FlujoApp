package dev.ycosorio.flujo.ui.screens.documents

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.repository.DocumentRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SignatureViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val storage: FirebaseStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val assignmentId: String = checkNotNull(savedStateHandle["assignmentId"])

    private val _signatureState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val signatureState = _signatureState.asStateFlow()

    fun saveSignature(bitmap: Bitmap) {
        viewModelScope.launch {
            _signatureState.value = Resource.Loading()
            try {
                // 1. Convertir el Bitmap a bytes
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val data = baos.toByteArray()

                // 2. Definir dónde se guardará en Storage
                val signatureRef = storage.reference.child("signatures/${assignmentId}/${UUID.randomUUID()}.png")

                // 3. Subir los bytes
                val uploadTask = signatureRef.putBytes(data).await()
                val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                // 4. Guardar la URL en Firestore a través del repositorio
                val result = documentRepository.markDocumentAsSigned(assignmentId, downloadUrl)
                _signatureState.value = result

            } catch (e: Exception) {
                _signatureState.value = Resource.Error(e.localizedMessage ?: "Error al guardar la firma")
            }
        }
    }
}