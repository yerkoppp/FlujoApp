package dev.ycosorio.flujo.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ycosorio.flujo.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    user: User?,
    onProfileClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(text = "Hola ${user?.name}") },
        modifier = modifier,
        actions = {
            // ----------------------------------------
            if (user != null) {
                IconButton(onClick = onProfileClicked) {
                    UserAvatar(user = user)
                }
            }
        }
    )
}