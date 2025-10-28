package dev.ycosorio.flujo.ui.screens.admin.users.adduser

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.utils.Resource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.ycosorio.flujo.ui.screens.admin.users.adduser.AddUserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    viewModel: AddUserViewModel,
    onUserAddedSuccessfully: () -> Unit,
    onBackPressed: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }

    // Observamos el estado de la operación de creación
    val addUserState by viewModel.addUserState.collectAsState()

    // Este efecto se dispara cuando el estado cambia a Success
    LaunchedEffect(addUserState) {
        if (addUserState is Resource.Success) {
            onUserAddedSuccessfully()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Añadir Trabajador") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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
                    viewModel.createUser(name, email, position, area)
                },
                // Deshabilitamos el botón mientras se está guardando
                enabled = addUserState !is Resource.Loading && addUserState !is Resource.Success,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (addUserState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("GUARDAR TRABAJADOR")
                }
            }

            // Mostramos un mensaje de error si la operación falla
            if (addUserState is Resource.Error) {
                Text(
                    text = (addUserState as Resource.Error<Unit>)
                        .message ?: "Error desconocido",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}