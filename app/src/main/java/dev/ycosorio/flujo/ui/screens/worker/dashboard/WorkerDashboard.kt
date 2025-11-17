package dev.ycosorio.flujo.ui.screens.worker.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.ycosorio.flujo.domain.model.DocumentAssignment
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.components.MaterialRequestItem
import dev.ycosorio.flujo.ui.navigation.Routes
import dev.ycosorio.flujo.ui.screens.admin.dashboard.DashboardAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDashboard(
    user: User,
    navController: NavHostController,
    viewModel: WorkerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).fillMaxWidth(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bienvenido a Flujo",
                        fontWeight = FontWeight.Bold // Un toque más de énfasis
                    )
                },
                actions = {
                    NotificationBadge(
                        count = uiState.pendingDocuments.size,
                        tint = MaterialTheme.colorScheme.primary
                    ) {
                        navController.navigate(Routes.Notifications.createRoute(user.uid))
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                   containerColor = MaterialTheme.colorScheme.background,
                   titleContentColor = MaterialTheme.colorScheme.primary,
                   actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                scrollBehavior = scrollBehavior
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // --- 1. ACCIONES REQUERIDAS (Lo más urgente) ---
                item {
                    RequiredActionsSection(
                        pendingDocuments = uiState.pendingDocuments,
                        navController = navController
                    )
                }

                // --- 2. ACCIONES FRECUENTES (Navegación consolidada) ---
                item {
                    FrequentActionsSection(
                        user = user,
                        navController = navController
                    )
                }

                // --- 3. ESTADO: SOLICITUDES RECIENTES (Actividad) ---
                item {
                    RecentRequestsSection(
                        recentRequests = uiState.recentRequests,
                        navController = navController
                    )
                }
            }
        }
    }
}

// --- SECCIÓN 1: ACCIONES REQUERIDAS ---
@Composable
private fun RequiredActionsSection(
    pendingDocuments: List<DocumentAssignment>,
    navController: NavHostController
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Acciones Requeridas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val displayedPending = pendingDocuments.take(2)

        if (displayedPending.isEmpty()) {
            // Tarjeta de "Todo bien". Refuerzo positivo.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "¡Estás al día! No tienes tareas pendientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            displayedPending.forEach { request ->
                DocumentPendingItem(
                    documentName = "Solicitud de material #${request.status}",
                    onClick = {
                        navController.navigate(Routes.DocumentDetail.createRoute(request.id))
                    }
                )
            }

            if (pendingDocuments.size > 2) {
                ActionTextButton(
                    text = "Ver todos los ${pendingDocuments.size} documentos pendientes",
                    onClick = { navController.navigate(Routes.AssignDocument.route) }
                )
            }
        }
    }
}

// --- SECCIÓN 2: ACCIONES FRECUENTES ---
@Composable
private fun FrequentActionsSection(
    user: User,
    navController: NavHostController
) {
    val actions = listOf(
        DashboardAction(
            title = "Solicitud",
            icon = Icons.Default.PostAdd,
            onClick = { navController.navigate(Routes.CreateRequest.route) }
        ),
        DashboardAction(
            title = "Rendir",
            icon = Icons.Default.RequestQuote,
            onClick = { navController.navigate(Routes.CreateExpenseReport.route) }
        ),
        DashboardAction(
            title = "Mensajes",
            icon = Icons.AutoMirrored.Filled.Message,
            onClick = { navController.navigate(Routes.Messages.createRoute(user.uid)) }
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Acciones Frecuentes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(actions, key = { it.title }) { action ->
                ActionChip(
                    action = action
                )
            }
        }
    }
}

// --- SECCIÓN 3: SOLICITUDES RECIENTES ---
@Composable
private fun RecentRequestsSection(
    recentRequests: List<MaterialRequest>,
    navController: NavHostController
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Solicitudes Recientes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val displayedRequests = recentRequests.take(2)

        if (displayedRequests.isEmpty()) {
            Text(
                text = "No has realizado solicitudes recientemente.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayedRequests.forEach { request ->
                    MaterialRequestItem(
                        role = Role.TRABAJADOR,
                        request = request,
                        onApprove = {},
                        onReject = {}
                    )
                }
            }

            if (recentRequests.size > 2) {
                Spacer(modifier = Modifier.height(8.dp))
                ActionTextButton(
                    text = "Ver historial completo de solicitudes",
                    onClick = { navController.navigate(Routes.WorkerRequests.route) }
                )
            }
        }
    }
}


// --- COMPONENTES REUTILIZABLES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionChip(
    action: DashboardAction,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = action.onClick,
        modifier = modifier.size(width = 96.dp, height = 96.dp) // Tamaño consistente
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary // Tinte primario para la acción
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelMedium, // Texto más pequeño pero claro
                textAlign = TextAlign.Center
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
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        ListItem(
            headlineContent = {
                Text(documentName, fontWeight = FontWeight.SemiBold)
            },
            supportingContent = { Text("Pendiente de firma") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Pendiente",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent // El color lo da la Card
            )
        )
    }
}

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


@Composable
private fun NotificationBadge(
    count: Int,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.clickable { onClick() }.padding(end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = "Notificaciones",
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

data class DashboardAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
