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
import dev.ycosorio.flujo.utils.Resource
import timber.log.Timber

@Composable
fun AccessVerificationScreen(
    authUser: AuthUser?,
    viewModel: AccessVerificationViewModel = hiltViewModel(),
    onAccessGranted: () -> Unit,
    onAccessDenied: () -> Unit
) {
    val context = LocalContext.current
    val verificationState by viewModel.verificationState.collectAsState()

    // Variable para rastrear si ya se cerró sesión
    var hasSignedOut by remember { mutableStateOf(false) }

    // Resetear estado al entrar a la pantalla
    LaunchedEffect(Unit) {
        Timber.d("🔄 Pantalla iniciada, reseteando estado")
        viewModel.resetVerification()
    }

    // Verificar acceso cuando hay un usuario autenticado
    LaunchedEffect(authUser?.uid) {
        Timber.d("👤 AuthUser cambió: ${authUser?.email}")

        if (authUser != null && verificationState !is Resource.Success) {
            Timber.d("🔍 Verificando usuario: ${authUser.email}")
            viewModel.verifyUserAccess(authUser)
        }
    }

    // Manejar resultado de verificación
    LaunchedEffect(verificationState) {
        Timber.d("Estado: ${verificationState::class.simpleName}")

        when (val state = verificationState) {
            is Resource.Success -> {
                Timber.d("✅ Acceso concedido")
                kotlinx.coroutines.delay(1000)
                onAccessGranted()
            }
            is Resource.Error -> {
                val isConnectionError = state.message?.contains("conexión", ignoreCase = true) == true ||
                        state.message?.contains("tiempo de espera", ignoreCase = true) == true

                // Solo cerrar sesión si NO es error de conexión
                if (!isConnectionError && !hasSignedOut) {
                    Timber.e("❌ Acceso denegado: ${state.message}")
                    hasSignedOut = true

                    kotlinx.coroutines.delay(2000)

                    AuthUI.getInstance()
                        .signOut(context)
                        .addOnCompleteListener {
                            Timber.d("🚪 Sesión cerrada")
                            onAccessDenied()
                        }
                }
            }
            else -> {
                Timber.d("⏳ Verificando...")
            }
        }
    }

    // UI durante la verificación
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = verificationState) {
            is Resource.Idle, is Resource.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Verificando acceso...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        authUser?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is Resource.Success -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        "✅",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        "Acceso Concedido",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Bienvenido, ${state.data?.name}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            is Resource.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        "⚠️",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        "Acceso Denegado",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        state.message ?: "Tu cuenta no está registrada en el sistema.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    // Mostrar botón de reintentar si es error de conexión
                    if (state.message?.contains("conexión", ignoreCase = true) == true ||
                        state.message?.contains("tiempo de espera", ignoreCase = true) == true) {

                        Button(
                            onClick = {
                                authUser?.let {
                                    viewModel.resetVerification()
                                    viewModel.verifyUserAccess(it)
                                }
                            }
                        ) {
                            Text("Reintentar")
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Verifica que tengas conexión a internet",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!hasSignedOut) {
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
            }
        }
    }
}