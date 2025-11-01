package dev.ycosorio.flujo.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.firebase.ui.auth.AuthUI
import dev.ycosorio.flujo.domain.model.AuthUser
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.launch

@Composable
fun AccessVerificationScreen(
    authUser: AuthUser?,
    viewModel: AccessVerificationViewModel = hiltViewModel(),
    onAccessGranted: () -> Unit,
    onAccessDenied: () -> Unit
) {
    val context = LocalContext.current
    val verificationState by viewModel.verificationState.collectAsState()

    // Verificar acceso cuando hay un usuario autenticado
    LaunchedEffect(authUser) {
        authUser?.let {
            viewModel.verifyUserAccess(it)
        }
    }

    // Manejar resultado de verificación
    LaunchedEffect(verificationState) {
        when (verificationState) {
            is Resource.Success -> {
                onAccessGranted()
            }
            is Resource.Error -> {
                // Cerrar sesión y volver al login
                AuthUI.getInstance()
                    .signOut(context)
                    .addOnCompleteListener {
                        onAccessDenied()
                    }
            }
            else -> {}
        }
    }

    // UI durante la verificación
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = verificationState) {
            is Resource.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Verificando acceso...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            is Resource.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        "⚠️ Acceso Denegado",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Text(
                        state.message ?: "Tu cuenta no está registrada en el sistema.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "Contacta al administrador para solicitar acceso.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    CircularProgressIndicator(modifier = Modifier.size(24.dp))

                    Text(
                        "Cerrando sesión...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {}
        }
    }
}
