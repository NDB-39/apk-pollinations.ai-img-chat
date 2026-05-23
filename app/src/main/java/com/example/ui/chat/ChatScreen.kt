package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.local.MessageEntity
import com.example.data.local.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.LocalGlobalSettings
import com.example.ui.components.SessionDrawer
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ChatViewModel(
    private val repository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val sessions = repository.getSessions(0).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var currentSessionId by mutableStateOf<Int?>(null)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<MessageEntity>> = snapshotFlow { currentSessionId }
        .flatMapLatest { sessionId ->
            if (sessionId != null) repository.getMessages(sessionId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var isLoading by mutableStateOf(false)
        private set

    var textModels = listOf<String>()
        private set

    var currentModel by mutableStateOf("")

    init {
        viewModelScope.launch {
            val modelsStr = settingsRepository.textModelsFlow.first()
            textModels = modelsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (textModels.isNotEmpty() && currentModel.isEmpty()) {
                currentModel = textModels.first()
            }
        }
        viewModelScope.launch {
            repository.getSessions(0).firstOrNull()?.let { s ->
                if (s.isEmpty()) {
                    createNewSession()
                } else {
                    currentSessionId = s.first().id
                }
            }
        }
    }

    fun createNewSession(title: String = "Chat mới") {
        viewModelScope.launch {
            currentSessionId = repository.createSession(title, 0)
        }
    }

    fun selectSession(id: Int) {
        currentSessionId = id
    }

    fun deleteSession(id: Int) {
        viewModelScope.launch {
            repository.deleteSession(id)
            if (currentSessionId == id) {
                val latest = sessions.value.firstOrNull { it.id != id }
                currentSessionId = latest?.id
                if (currentSessionId == null) {
                    createNewSession()
                }
            }
        }
    }

    fun sendMessage(content: String) {
        val sessionId = currentSessionId ?: return
        if (content.isBlank()) return
        
        val currentMsgs = messages.value
        val isFirstMessage = currentMsgs.isEmpty()

        viewModelScope.launch {
            isLoading = true
            repository.saveUserMessage(sessionId, content)
            
            if (isFirstMessage) {
                val title = if (content.length > 20) content.take(20) + "..." else content
                repository.updateSessionTitle(sessionId, title)
            }
            
            try {
                val systemPrompt = settingsRepository.textSystemPromptFlow.first()
                val response = repository.sendMessageSync(content, currentModel, systemPrompt)
                repository.saveBotTextMessage(sessionId, response)
            } catch (e: Exception) {
                repository.saveBotTextMessage(sessionId, "Error: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch { 
            repository.clearAllSessions(0)
            createNewSession()
        }
    }
}

@Composable
fun provideChatViewModel(appRepository: AppRepository, settingsRepository: SettingsRepository): ChatViewModel {
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(appRepository, settingsRepository) as T
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(appRepository: AppRepository, settingsRepository: SettingsRepository) {
    val viewModel = provideChatViewModel(appRepository, settingsRepository)
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val sessions by viewModel.sessions.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val globalSettings = LocalGlobalSettings.current
    val fontSize by globalSettings.fontSize.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Xác nhận") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ lịch sử nhắn tin không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    SessionDrawer(
        drawerState = drawerState,
        sessions = sessions,
        currentSessionId = viewModel.currentSessionId,
        onSessionSelected = { 
            viewModel.selectSession(it)
            scope.launch { drawerState.close() }
        },
        onNewSession = { 
            viewModel.createNewSession()
            scope.launch { drawerState.close() }
        },
        onDeleteSession = { viewModel.deleteSession(it) }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Chat") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Xóa lịch sử")
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.width(150.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.currentModel,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            viewModel.textModels.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        viewModel.currentModel = selectionOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(message = message, fontSize = fontSize)
                    }
                    if (viewModel.isLoading) {
                        item {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Nhập tin nhắn...") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        enabled = inputText.isNotBlank() && !viewModel.isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity, 
    fontSize: Float,
    onImageClick: ((String) -> Unit)? = null
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (message.type == 0) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    color = textColor,
                    fontSize = fontSize.sp
                )
            } else {
                AsyncImage(
                    model = message.content,
                    contentDescription = "Generated Image",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .then(if (onImageClick != null) Modifier.clickable { onImageClick(message.content) } else Modifier)
                )
            }
        }
    }
}
