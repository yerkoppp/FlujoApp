package dev.ycosorio.flujo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.R
import dev.ycosorio.flujo.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    user: User?,
    onProfileClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // --- 1. Logo ---
                Image(
                    painter = painterResource(id = R.drawable.icono),
                    contentDescription = "Logo de Flujo",
                    modifier = Modifier.size(32.dp) // Ajusta el tamaño como necesites
                )

                Spacer(modifier = Modifier.width(12.dp))

                // --- 2. Saludo ---
                Text(
                    text = "¡Hola, ${user?.name}!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier,
        actions = {
            // ----------------------------------------
            if (user != null) {
                IconButton(onClick = onProfileClicked) {
                    UserAvatar(user = user)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun UserAvatar(user: User, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = R.drawable.ic_logo_round),
        contentDescription = "Perfil de ${user.name}",
        modifier = modifier.size(36.dp)
    )
}
