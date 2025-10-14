package dev.ycosorio.flujo.ui.screens.admin.users

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.components.UserItem
import dev.ycosorio.flujo.utils.Resource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon

@Composable
fun UserManagementScreen(
    viewModel: UserManagementViewModel, // Recibiremos el ViewModel aquí
    onAddUserClicked: () -> Unit // Lambda para navegar a la nueva pantalla
) {
    // Observamos el estado del ViewModel. 'collectAsState' hace que la UI se recomponga
    // automáticamente cada vez que el estado cambia.
    val uiState by viewModel.usersState.collectAsState()
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddUserClicked) {
                Icon(Icons.Default.Add, contentDescription = "Añadir usuario")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Usamos un 'when' para decidir qué mostrar en pantalla según el estado.
            when (uiState) {
                is Resource.Loading -> {
                    CircularProgressIndicator() // Muestra un círculo de carga.
                }
                is Resource.Success -> {
                    val users = (uiState as Resource.Success<List<User>>).data
                    if (users.isNullOrEmpty()) {
                        Text("No se encontraron trabajadores.")
                    } else {
                        UserList(users = users) // Muestra la lista de usuarios.
                    }
                }
                is Resource.Error -> {
                    val message = (uiState as Resource.Error<List<User>>).message
                    Text("Error: $message") // Muestra el mensaje de error.
                }
            }
        }
    }
}

@Composable
private fun UserList(users: List<User>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(users) { user ->
            UserItem(user = user)
        }
    }
}
