package dev.ycosorio.flujo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import dev.ycosorio.flujo.data.preferences.UserPreferencesData
import dev.ycosorio.flujo.data.preferences.UserPreferencesRepository
import dev.ycosorio.flujo.ui.navigation.AppNavigation
import dev.ycosorio.flujo.ui.theme.FlujoAppTheme
import dev.ycosorio.flujo.ui.AppViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Timber.tag("FCM_TOKEN").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Timber.tag("FCM_TOKEN").d("Token: $token")
                Timber.tag("FCM_TOKEN").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            } else {
                Timber.tag("FCM_TOKEN").e(task.exception, "Error al obtener token")
            }
        }

        enableEdgeToEdge()
        handleNotificationIntent(intent)

        setContent {
            val preferences by userPreferencesRepository.userPreferences.collectAsState(
                initial = UserPreferencesData()
            )

            FlujoAppTheme (
                appTheme = preferences.theme,
                fontScale = preferences.fontScale,
                contrastLevel = preferences.contrastLevel
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                    )
                }
            }
        }

        askNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        Timber.tag("MainActivity").d("📱 onNewIntent llamado")
        handleNotificationIntent(intent)  // ✅ Manejar aquí también
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val openNotifications = intent?.getBooleanExtra("openNotifications", false) ?: false
        Timber.tag("MainActivity").d("🔔 openNotifications: $openNotifications")

        if (openNotifications) {
            Timber.tag("MainActivity").d("✅ Disparando evento de notificaciones")

            // Obtener el ViewModel inyectado
            lifecycleScope.launch {
                // Pequeño delay para asegurar que la UI está lista
                kotlinx.coroutines.delay(300)

                // Disparar el evento en el ViewModel
                val viewModel: AppViewModel by viewModels()
                viewModel.triggerOpenNotifications()
            }

            // Limpiar el extra
            intent?.removeExtra("openNotifications")
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido
        } else {
            // Permiso denegado - puedes mostrar un mensaje
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya tiene permiso
                }
                else -> {
                    // Pedir permiso
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
