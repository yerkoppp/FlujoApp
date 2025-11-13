package dev.ycosorio.flujo.ui.screens.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeMessageScreen(
    userId: String,
    userName: String,
    userRole: Role,
    viewModel: MessagesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedWorkers by remember { mutableStateOf(setOf<String>()) }
    var sendToAll by remember { mutableStateOf(false) }
    var showWorkerSelector by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (userRole == Role.ADMINISTRADOR) {
            viewModel.loadWorkers()
        } else {
            // Si es trabajador, cargar lista de admins
            viewModel.loadAdmins()
        }
    }

    val workers by viewModel.workers.collectAsState()
    val admins by viewModel.admins.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Mensaje") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (subject.isNotBlank() && content.isNotBlank()) {
                                isSending = true
                                val recipientIds = when {
                                    userRole == Role.TRABAJADOR -> {
                                        // ✅ Obtener UIDs de TODOS los administradores
                                        (admins as? Resource.Success)?.data?.map { it.uid } ?: emptyList()
                                    }
                                    sendToAll -> {
                                        (workers as? Resource.Success)?.data?.map { it.uid } ?: emptyList()
                                    }
                                    else -> selectedWorkers.toList()
                                }

                                viewModel.sendMessage(
                                    senderId = userId,
                                    senderName = userName,
                                    recipientIds = recipientIds,
                                    subject = subject,
                                    content = content,
                                    isToAllWorkers = sendToAll
                                ) { result ->
                                    isSending = false
                                    if (result is Resource.Success) {
                                        onNavigateBack()
                                    }
                                }
                            }
                        },
                        enabled = !isSending && subject.isNotBlank() &&
                                content.isNotBlank() &&
                                (
                                        (userRole == Role.TRABAJADOR && (admins as? Resource.Success)
                                            ?.data?.isNotEmpty() == true) ||
                                                (userRole == Role.ADMINISTRADOR &&
                                                        (sendToAll || selectedWorkers.isNotEmpty()))
                                        )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text("ENVIAR")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Para trabajadores: mensaje al administrador
            if (userRole == Role.TRABAJADOR) {
                Card {
                    ListItem(
                        headlineContent = {
                            when (admins) {
                                is Resource.Success -> {
                                    val adminCount = (admins as Resource.Success).data?.size ?: 0
                                    Text(if (adminCount > 1)
                                        "Para: Todos los Administradores ($adminCount)" else "Para: Administrador")
                                }
                                is Resource.Loading -> Text("Para: Cargando...")
                                else -> Text("Para: Administrador")
                            }
                        }
                    )
                }
            }

            // Para administradores: selector de destinatarios
            if (userRole == Role.ADMINISTRADOR) {
                Card {
                    Column {
                        ListItem(
                            headlineContent = { Text("Destinatarios") },
                            trailingContent = {
                                TextButton(onClick = { showWorkerSelector = !showWorkerSelector }) {
                                    Text(if (showWorkerSelector) "Ocultar" else "Seleccionar")
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sendToAll,
                                onCheckedChange = {
                                    sendToAll = it
                                    if (it) selectedWorkers = emptySet()
                                }
                            )
                            Text("Enviar a todos los trabajadores")
                        }

                        if (showWorkerSelector && !sendToAll) {
                            when (workers) {
                                is Resource.Success -> {
                                    val workersList = (workers as Resource.Success).data ?: emptyList()
                                    LazyColumn(
                                        modifier = Modifier.heightIn(max = 200.dp)
                                    ) {
                                        items(workersList) { worker ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = selectedWorkers.contains(worker.uid),
                                                    onCheckedChange = { checked ->
                                                        selectedWorkers = if (checked) {
                                                            selectedWorkers + worker.uid
                                                        } else {
                                                            selectedWorkers - worker.uid
                                                        }
                                                    }
                                                )
                                                Column {
                                                    Text(worker.name)
                                                    Text(
                                                        text = worker.position,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                is Resource.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                is Resource.Error -> {
                                    Text(
                                        text = "Error al cargar trabajadores",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }

                                is Resource.Idle -> {
                                }
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Asunto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Mensaje") },
                modifier = Modifier
                    .fillMaxWidth(),
                minLines = 5
            )
        }
    }
}
