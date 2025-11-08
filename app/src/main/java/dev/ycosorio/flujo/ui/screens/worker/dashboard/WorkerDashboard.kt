package dev.ycosorio.flujo.ui.screens.worker.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.theme.Typography
import dev.ycosorio.flujo.ui.components.MaterialRequestItem
import dev.ycosorio.flujo.ui.navigation.BottomNavItem
import dev.ycosorio.flujo.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDashboard(
    user: User,
    navController: NavHostController,
    viewModel: WorkerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold (
        topBar = {
            TopAppBar(title = { Text("Resumen del Trabajador") })
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Error")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 1. Tarjeta de Acción Rápida ---
                item {
                    QuickActionCard(
                        title = "Nueva Solicitud de Material",
                        onClick = {
                            // Navega a la pantalla de crear solicitud
                            navController.navigate(Routes.CreateRequest.route)
                        }
                    )
                }

                // --- 2. Tareas Pendientes (Documentos) ---
                item {
                    Text(
                        text = "Tareas Pendientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.pendingDocuments.isEmpty()) {
                    item {
                        Text(
                            text = "No tienes documentos pendientes por firmar.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(uiState.pendingDocuments, key = { it.workerId }) { document ->
                        DocumentPendingItem(
                            documentName = document.documentTitle,
                            onClick = {
                                // Navega a la pantalla de firma
                                navController.navigate("${Routes.Signature.route}/${document.id}")
                            }
                        )
                    }
                }

                // --- 3. Estado de Solicitudes Recientes ---
                item {
                    Text(
                        text = "Solicitudes Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.recentRequests.isEmpty()) {
                    item {
                        Text(
                            text = "No has realizado solicitudes recientemente.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(uiState.recentRequests, key = { it.id }) { request ->
                        // Reutilizamos tu Composable
                        MaterialRequestItem(
                            role = Role.TRABAJADOR, //
                            request = request,
                            onApprove = {}, // El trabajador no puede aprobar
                            onReject = {}  // El trabajador no puede rechazar
                        )
                    }
                }
            }
        }
    }
}


// --- Composables locales para el Dashboard ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentPendingItem(
    documentName: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(documentName) },
            supportingContent = { Text("Pendiente de firma") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Pendiente",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )
    }
}