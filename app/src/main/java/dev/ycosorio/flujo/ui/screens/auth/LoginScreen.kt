package dev.ycosorio.flujo.ui.screens.auth

import android.util.Log
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

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

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
        Log.d("LoginScreen", "Auth result code: ${result.resultCode}")
        // El resultado de FirebaseUI puede tener errores de credenciales
        // pero eso no impide que la autenticación sea exitosa
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Log.d("LoginScreen", "✅ Login exitoso")
        } else {
            Log.w("LoginScreen", "⚠️ Result code: ${result.resultCode}")
            // Aún así, verificamos si hay usuario autenticado
        }
    }

    // Navegar automáticamente cuando hay usuario
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Log.d("LoginScreen", "✅ Usuario detectado: ${currentUser?.email}")
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
                        Log.d("LoginScreen", "🚀 Iniciando flujo de login")
                        launcher.launch(signInIntent)
                              },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Iniciar sesión con Google",
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