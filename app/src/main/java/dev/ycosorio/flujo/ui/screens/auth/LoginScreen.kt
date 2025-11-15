package dev.ycosorio.flujo.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.decode.ImageSource
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import dev.ycosorio.flujo.ui.theme.FlujoAppTheme
import dev.ycosorio.flujo.R
import dev.ycosorio.flujo.ui.AppViewModel
import timber.log.Timber

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    // Resetear el estado al entrar a la pantalla de login
    LaunchedEffect(Unit) {
        Timber.d("🔄 Reseteando estado de autenticación")
        appViewModel.clearUserProfile()
    }

    // Configurar proveedores de Auth UI
    val providers = remember {
        listOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
    }

    // Intent de Auth UI
    val signInIntent = remember {
        AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.logo_transp)
            .enableAnonymousUsersAutoUpgrade()
            .build()
    }

    // Launcher para Auth UI
    val launcher = rememberLauncherForActivityResult(
        contract = FirebaseAuthUIActivityResultContract()
    ) { result ->
        Timber.d("Auth result code: ${result.resultCode}")
        // El resultado de FirebaseUI puede tener errores de credenciales
        // pero eso no impide que la autenticación sea exitosa
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Timber.d("✅ Login exitoso")
        } else {
            Timber.w("⚠️ Result code: ${result.resultCode}")
            // Aún así, verificamos si hay usuario autenticado
        }
    }

    // Navegar automáticamente cuando hay usuario
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Timber.d("✅ Usuario detectado: ${currentUser?.email}")
            onLoginSuccess()
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_transp),
                        contentDescription = "Logo de la app",
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Botón de inicio de sesión
                Button(
                    onClick = {
                        Timber.d("🚀 Iniciando flujo de login")
                        launcher.launch(signInIntent)
                              },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp)
                ) {
                    Text(
                        text = "INGRESAR",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = "Inicia sesión con tu cuenta de Google",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}