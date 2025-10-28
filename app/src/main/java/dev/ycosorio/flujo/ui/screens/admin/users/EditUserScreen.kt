package dev.ycosorio.flujo.ui.screens.admin.users

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    user: User,
    viewModel: EditUserViewModel,
    onUserUpdatedSuccessfully: () -> Unit,
    onBackPressed: () -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var phoneNumber by remember { mutableStateOf(user.phoneNumber ?: "") }
    var position by remember { mutableStateOf(user.position) }
    var area by remember { mutableStateOf(user.area) }

    val updateUserState by viewModel.updateUserState.collectAsState()

    LaunchedEffect(updateUserState) {
        if (updateUserState is Resource.Success) {
            onUserUpdatedSuccessfully()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Trabajador") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Teléfono (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = position,
                onValueChange = { position = it },
                label = { Text("Cargo") },
                placeholder = { Text("Ej: Técnico de Campo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = { Text("Área") },
                placeholder = { Text("Ej: Instalaciones") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.updateUser(
                        user.copy(
                            name = name,
                            email = email,
                            phoneNumber = phoneNumber.ifBlank { null },
                            position = position,
                            area = area
                        )
                    )
                },
                enabled = updateUserState !is Resource.Loading && updateUserState !is Resource.Success,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (updateUserState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("GUARDAR CAMBIOS")
                }
            }

            if (updateUserState is Resource.Error) {
                Text(
                    text = (updateUserState as Resource.Error<Unit>).message ?: "Error desconocido",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}