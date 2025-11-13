package dev.ycosorio.flujo.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.Message
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.MessageRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _receivedMessages = MutableStateFlow<Resource<List<Message>>>(Resource.Loading())
    val receivedMessages: StateFlow<Resource<List<Message>>> = _receivedMessages.asStateFlow()

    private val _sentMessages = MutableStateFlow<Resource<List<Message>>>(Resource.Loading())
    val sentMessages: StateFlow<Resource<List<Message>>> = _sentMessages.asStateFlow()

    private val _workers = MutableStateFlow<Resource<List<User>>>(Resource.Loading())
    val workers: StateFlow<Resource<List<User>>> = _workers.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun loadCurrentUser(userId: String) {
        viewModelScope.launch {
            userRepository.getUserById(userId).collect { resource ->
                if (resource is Resource.Success) {
                    _currentUser.value = resource.data
                }
            }
        }
    }

    fun loadReceivedMessages(userId: String) {
        viewModelScope.launch {
            messageRepository.getReceivedMessages(userId).collect { resource ->
                _receivedMessages.value = resource
            }
        }
    }

    fun loadSentMessages(userId: String) {
        viewModelScope.launch {
            messageRepository.getSentMessages(userId).collect { resource ->
                _sentMessages.value = resource
            }
        }
    }

    fun loadWorkers() {
        viewModelScope.launch {
            userRepository.getUsersByRole(Role.TRABAJADOR).collect { resource ->
                _workers.value = resource
            }
        }
    }

    fun sendMessage(
        senderId: String,
        senderName: String,
        recipientIds: List<String>,
        subject: String,
        content: String,
        isToAllWorkers: Boolean = false,
        onResult: (Resource<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val message = Message(
                senderId = senderId,
                senderName = senderName,
                recipientIds = recipientIds,
                subject = subject,
                content = content,
                isToAllWorkers = isToAllWorkers
            )
            val result = messageRepository.sendMessage(message)
            onResult(result)
        }
    }

    fun markAsRead(messageId: String, userId: String) {
        viewModelScope.launch {
            messageRepository.markAsRead(messageId, userId)
        }
    }

    fun deleteMessage(messageId: String, onResult: (Resource<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = messageRepository.deleteMessage(messageId)
            onResult(result)
        }
    }
}