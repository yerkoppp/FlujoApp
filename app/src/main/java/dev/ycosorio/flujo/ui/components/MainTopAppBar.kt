package dev.ycosorio.flujo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.ycosorio.flujo.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    user: User?,
    onProfileClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Flujo") },
        modifier = modifier,
        actions = {
            if (user != null) {
                IconButton(onClick = { showMenu = true }) {
                    UserAvatar(user = user) // Reutilizamos el avatar que ya creamos
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Mi Perfil") },
                        onClick = {
                            showMenu = false
                            onProfileClicked()
                        },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Cerrar Sesión") },
                        onClick = {
                            showMenu = false
                            onSignOutClicked()
                        },
                        leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
                    )
                }
            }
        }
    )
}