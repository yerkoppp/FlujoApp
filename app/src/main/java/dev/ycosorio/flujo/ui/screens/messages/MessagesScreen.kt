package dev.ycosorio.flujo.ui.screens.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ycosorio.flujo.domain.model.Message
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.utils.Resource
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    userId: String,
    viewModel: MessagesViewModel = hiltViewModel(),
    onNavigateToCompose: (User) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(userId) {
        viewModel.loadCurrentUser(userId)
        viewModel.loadReceivedMessages(userId)
        viewModel.loadSentMessages(userId)
    }

    val currentUser by viewModel.currentUser.collectAsState()
    val receivedMessages by viewModel.receivedMessages.collectAsStateWithLifecycle()
    val sentMessages by viewModel.sentMessages.collectAsStateWithLifecycle()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mensajes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
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
                0 -> MessageList(
                    messages = receivedMessages,
                    currentUserId = userId,
                    isReceived = true,
                    onMarkAsRead = { messageId ->
                        viewModel.markAsRead(messageId, userId)
                    }
                )
                1 -> MessageList(
                    messages = sentMessages,
                    currentUserId = userId,
                    isReceived = false
                )
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
        ),
        elevation = CardDefaults.cardElevation(2.dp),
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