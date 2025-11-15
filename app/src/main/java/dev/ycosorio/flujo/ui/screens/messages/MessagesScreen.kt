package dev.ycosorio.flujo.ui.screens.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.ycosorio.flujo.domain.model.Message
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.navigation.Routes
import dev.ycosorio.flujo.utils.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    userId: String,
    viewModel: MessagesViewModel = hiltViewModel(),
    onNavigateToCompose: (User) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(userId) {
        viewModel.loadCurrentUser(userId)
        //viewModel.loadReceivedMessages(userId)
        //viewModel.loadSentMessages(userId)
    }

    val currentUser by viewModel.currentUser.collectAsState()
    val receivedMessagesPaged = viewModel.getReceivedMessagesPaged(userId).collectAsLazyPagingItems()
    val sentMessagesPaged = viewModel.getSentMessagesPaged(userId).collectAsLazyPagingItems()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mensajes") },
                actions = {
                    // ✅ Solo mostrar botón si el usuario está cargado
                    IconButton(
                        onClick = {
                            currentUser?.let { user ->
                                onNavigateToCompose(user)
                            }
                        },
                        enabled = currentUser != null
                    ) {
                        Icon(Icons.Default.Add,
                            "Nuevo mensaje")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val tabTitles = listOf("Recibidos", "Enviados")

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        // 1. Pestaña seleccionada
                        selected = selectedTab == index,
                        // 2. Acción al hacer click
                        onClick = { selectedTab = index },
                        // 3. Contenido de la pestaña
                        text = { Text(text = title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> MessageListPaged(
                    messages = receivedMessagesPaged,
                    currentUserId = userId,
                    isReceived = true,
                    onMarkAsRead = { messageId ->
                        viewModel.markAsRead(messageId, userId)
                    }
                )
                1 -> MessageListPaged(
                    messages = sentMessagesPaged,
                    currentUserId = userId,
                    isReceived = false
                )
            }
        }
    }
}

// ✅ NUEVO: Composable para lista paginada
@Composable
fun MessageListPaged(
    messages: androidx.paging.compose.LazyPagingItems<Message>,
    currentUserId: String,
    isReceived: Boolean,
    onMarkAsRead: ((String) -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Mostrar estado de carga inicial
        if (messages.loadState.refresh is LoadState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Mostrar error si ocurre
        if (messages.loadState.refresh is LoadState.Error) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Error al cargar mensajes",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { messages.retry() }) {
                    Text("Reintentar")
                }
            }
        }

        // Mostrar lista vacía
        if (messages.loadState.refresh is LoadState.NotLoading && messages.itemCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay mensajes")
            }
        }

        // ✅ Lista paginada
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = messages.itemCount,
                key = messages.itemKey { it.id }
            ) { index ->
                val message = messages[index]
                if (message != null) {
                    MessageItem(
                        message = message,
                        currentUserId = currentUserId,
                        isReceived = isReceived,
                        onMarkAsRead = onMarkAsRead
                    )
                }
            }

            // Mostrar loading al cargar más items
            if (messages.loadState.append is LoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Mostrar error al cargar más
            if (messages.loadState.append is LoadState.Error) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error al cargar más mensajes")
                        Button(onClick = { messages.retry() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageList(
    messages: Resource<List<Message>>,
    currentUserId: String,
    isReceived: Boolean,
    onMarkAsRead: ((String) -> Unit)? = null
) {
    when (messages) {
        is Resource.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is Resource.Success -> {
            if (messages.data.isNullOrEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay mensajes")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.data) { message ->
                        MessageItem(
                            message = message,
                            currentUserId = currentUserId,
                            isReceived = isReceived,
                            onMarkAsRead = onMarkAsRead
                        )
                    }
                }
            }
        }
        is Resource.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${messages.message}")
            }
        }

        is Resource.Idle -> {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageItem(
    message: Message,
    currentUserId: String,
    isReceived: Boolean,
    onMarkAsRead: ((String) -> Unit)? = null
) {
    val isRead = message.isRead[currentUserId] ?: false
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
                if (!isRead && isReceived && onMarkAsRead != null) {
                    onMarkAsRead(message.id)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isRead || !isReceived)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isReceived) "De: ${message.senderName}" else "Para: ${getRecipientText(message)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isRead || !isReceived) FontWeight.Normal else FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat(
                        "dd/MM/yy HH:mm",
                        Locale.getDefault())
                        .format(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isRead || !isReceived) FontWeight.Normal else FontWeight.Bold
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun getRecipientText(message: Message): String {
    return when {
        message.isToAllWorkers -> "Todos los trabajadores"
        message.recipientIds.size > 1 ->
            "${message.recipientIds.size} destinatarios"
        else -> "1 destinatario"
    }
}