package dev.ycosorio.flujo.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val userState by viewModel.userState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }

    // Cargar datos iniciales
    LaunchedEffect(userState) {
        if (userState is Resource.Success) {
            (userState as Resource.Success<User>).data?.let { user ->
                name = user.name
                phoneNumber = user.phoneNumber ?: ""
                photoUrl = user.photoUrl ?: ""
            }
        }
    }

    // Navegar atrás si se guarda exitosamente
    LaunchedEffect(updateState) {
        if (updateState is Resource.Success) {
            onSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Teléfono") },
                placeholder = { Text("+56 9 1234 5678") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = photoUrl,
                onValueChange = { photoUrl = it },
                label = { Text("URL de foto de perfil") },
                placeholder = { Text("https://...") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("Ingresa la URL de tu foto de perfil")
                }
            )

            Spacer(Modifier.weight(1f))

            if (updateState is Resource.Error) {
                Text(
                    text = (updateState as Resource.Error).message ?: "Error",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val user = (userState as? Resource.Success)?.data
                    user?.let {
                        viewModel.updateProfile(
                            it.copy(
                                name = name,
                                phoneNumber = phoneNumber.ifBlank { null },
                                photoUrl = photoUrl.ifBlank { null }
                            )
                        )
                    }
                },
                enabled = updateState !is Resource.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (updateState is Resource.Loading) {
                    CircularProgressIndicator(
                        Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("GUARDAR CAMBIOS")
                }
            }
        }
    }
}