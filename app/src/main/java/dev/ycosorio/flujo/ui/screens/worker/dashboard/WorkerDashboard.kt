package dev.ycosorio.flujo.ui.screens.worker.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.theme.Typography
import dev.ycosorio.flujo.ui.components.MaterialRequestItem
import dev.ycosorio.flujo.ui.navigation.Routes
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardAction
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDashboard(
    user: User,
    navController: NavHostController,
    viewModel: WorkerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumen del Trabajador") },
                actions = {
                    NotificationBadge(count = uiState.pendingDocuments.size) {
                        navController.navigate(Routes.Notifications.route)
                    }
                },
                modifier = Modifier.fillMaxHeight(0.1f)
            )
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
                item {
                    WorkerGrid(navController = navController, user = user)
                }

                // --- 1. Tarjeta de Acción Rápida ---
                item {
                    Text(
                        text = "Acciones Rápidas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CompactQuickAction(
                            icon = Icons.Default.PostAdd,
                            description = "Nueva Solicitud",
                            onClick = { navController.navigate(Routes.CreateRequest.route) }
                        )
                        CompactQuickAction(
                            icon = Icons.Default.RequestQuote,
                            description = "Rendir Gastos",
                            onClick = { navController.navigate(Routes.ExpenseReportList.route) }
                        )
                        CompactQuickAction(
                            icon = Icons.Default.AddComment,
                            description = "Enviar Mensaje",
                            onClick = { navController.navigate(Routes.ComposeMessage.route) }
                        )
                        // Puedes añadir más si es necesario
                    }
                }
                // --- 2. Tareas Pendientes (Documentos) ---
                item {
                    Text(
                        text = "Tareas Pendientes (${uiState.pendingDocuments.size})", // Mostrar el conteo
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val displayedPending = uiState.pendingDocuments.take(2)
                if (displayedPending.isEmpty()) {
                    item {
                        Text(
                            text = "No tienes documentos pendientes por firmar.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(displayedPending, key = { it.id }) { document ->
                        DocumentPendingItem(
                            documentName = document.documentTitle,
                            onClick = {
                                navController.navigate(Routes.Signature.createRoute(document.id))
                            }
                        )
                    }

                    // Botón para ver más si hay pendientes ocultos
                    if (uiState.pendingDocuments.size > 2) {
                        item {
                            ActionTextButton(
                                text = "Ver todos los ${uiState.pendingDocuments.size} documentos pendientes",
                                onClick = { navController.navigate(Routes.AssignDocument.route) }
                            )
                        }
                    }
                }
                /*
                                if (uiState.pendingDocuments.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No tienes documentos pendientes por firmar.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    items(uiState.pendingDocuments, key = { it.id }) { document ->
                                        DocumentPendingItem(
                                            documentName = document.documentTitle,
                                            onClick = {
                                                // Navega a la pantalla de firma
                                                navController.navigate(Routes.Signature.createRoute(document.id))
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
                }*/
                // --- 4. Solicitudes Recientes (Máx. 2) ---
                item {
                    Text(
                        text = "Solicitudes Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // [CAMBIO LÓGICO]: Solo mostramos 2 solicitudes recientes
                val displayedRequests = uiState.recentRequests.take(2)
                if (displayedRequests.isEmpty()) {
                    item {
                        Text(
                            text = "No has realizado solicitudes recientemente.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(displayedRequests, key = { it.id }) { request ->
                        MaterialRequestItem(
                            role = Role.TRABAJADOR,
                            request = request,
                            onApprove = {},
                            onReject = {}
                        )
                    }


                    if (uiState.recentRequests.size > 2) {
                        item {
                            ActionTextButton(
                                text = "Ver historial completo de solicitudes",
                                onClick = { Routes.MaterialRequests.route }
                            )
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactQuickAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(72.dp), // Tamaño fijo y compacto
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer // Alto contraste
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// Botón de texto para "Ver más"
@Composable
private fun ActionTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        textAlign = TextAlign.Center
    )
}

// Tarjeta de Notificación en el TopAppBar
@Composable
private fun NotificationBadge(count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clickable { onClick() }.padding(end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = "Notificaciones",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )
        if (count > 0) {
            // Un pequeño círculo rojo para indicar pendientes
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .align(Alignment.TopEnd)
            ) {
                // Opcional: Mostrar el número si el círculo es lo suficientemente grande
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

@Composable
fun WorkerGrid(
    navController: NavHostController,
    user: User
) {
    val dashboardActions = listOf(
        DashboardAction(
            title = "Rendiciones",
            icon = Icons.Default.Money,
            onClick = { navController.navigate(Routes.ExpenseReportList.route) }
        ),
        DashboardAction(
            title = "Mensajes",
            icon = Icons.AutoMirrored.Filled.Message,
            onClick = { navController.navigate(Routes.Messages.createRoute(user.uid)) }
        ),
        DashboardAction(
            title = "Almacén", // Añadimos Almacén/Inventario como navegación
            icon = Icons.Default.Warehouse,
            onClick = { /* TODO: Navegar a Inventario si aplica */ }
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dashboardActions.forEach { action ->
            // Usamos el mismo DashboardCard, pero con un Modifier.weight(1f)
            DashboardWorkerCard(
                action = action,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardWorkerCard(
    action: DashboardAction,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = action.onClick,
        modifier = modifier
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(action.icon, contentDescription = action.title, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(12.dp))
            Text(action.title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

