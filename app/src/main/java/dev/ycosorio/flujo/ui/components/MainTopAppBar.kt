package dev.ycosorio.flujo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    user: User?,
    onProfileClicked: () -> Unit,
    //onSignOutClicked: () -> Unit,
    //onUserManagementClicked: () -> Unit,
    //onToggleUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    //var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = "Hola ${user?.name}") },
        modifier = modifier,
        actions = {
            // --- BOTÓN DE DEBUG PARA CAMBIAR ROL ---
           /* if (BuildConfig.DEBUG) {
            IconButton(onClick = onToggleUser) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Cambiar Rol (Debug)"
                )
            }
        }*/
            // ----------------------------------------
            if (user != null) {
                IconButton(onClick = onProfileClicked) {
                    UserAvatar(user = user)
                }
/*
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = "Mi Perfil") },
                        onClick = {
                            showMenu = false
                            onProfileClicked()
                        },
                        leadingIcon = { Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null) }
                    )

                    // Solo mostrar si es ADMINISTRADOR
                    if (user.role == Role.ADMINISTRADOR) {
                        DropdownMenuItem(
                            text = { Text(text = "Gestión de Usuarios") },
                            onClick = {
                                showMenu = false
                                onUserManagementClicked()
                            },
                            leadingIcon = { Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null) }
                        )
                    }
                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text(text = "Cerrar Sesión") },
                        onClick = {
                            showMenu = false
                            onSignOutClicked()
                        },
                        leadingIcon = { Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null) }
                    )
                }*/
            }
        }
    )
}