package dev.ycosorio.flujo.ui.screens.admin.users

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    viewModel: AddUserViewModel, // <-- CAMBIO: Ahora usa AddUserViewModel
    onUserAddedSuccessfully: () -> Unit
) {
    val name by remember { mutableStateOf("") }
    val email by remember { mutableStateOf("") }
    val position by remember { mutableStateOf("") }
    val area by remember { mutableStateOf("") }

    // Observamos el estado de la operación de creación
    val addUserState by viewModel.addUserState.collectAsState()

    // Este efecto se dispara cuando el estado cambia a Success
    LaunchedEffect(addUserState) {
        if (addUserState is Resource.Success) {
            onUserAddedSuccessfully()
        }
    }

    Scaffold(
        topBar = { /* ... */ }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ... (todos los OutlinedTextField se mantienen igual) ...

            Button(
                onClick = {
                    viewModel.createUser(name, email, position, area)
                },
                // Deshabilitamos el botón mientras se está guardando
                enabled = addUserState !is Resource.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (addUserState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("GUARDAR TRABAJADOR")
                }
            }

            // Mostramos un mensaje de error si la operación falla
            if (addUserState is Resource.Error) {
                Text(
                    text = (addUserState as Resource.Error<Unit>).message ?: "Error desconocido",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}