package com.example.ui.image

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.app.DownloadManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.content.ContentValues
import com.example.LocalGlobalSettings
import com.example.data.AppRepository
import com.example.data.local.MessageEntity
import com.example.data.local.SettingsRepository
import com.example.ui.chat.MessageBubble
import com.example.ui.components.SessionDrawer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ImageRenderViewModel(
    private val repository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val sessions = repository.getSessions(1).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    var isEnhancing by mutableStateOf(false)
        private set

    var imageModels = listOf<String>()
        private set
    var textModels = listOf<String>()
        private set

    var currentImageModel by mutableStateOf("")
    var currentTextModel by mutableStateOf("")

    init {
        viewModelScope.launch {
            val modelsStr = settingsRepository.imageModelsFlow.first()
            imageModels = modelsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (imageModels.isNotEmpty() && currentImageModel.isEmpty()) {
                currentImageModel = imageModels.first()
            }
        }
        viewModelScope.launch {
            val tModelsStr = settingsRepository.textModelsFlow.first()
            textModels = tModelsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (textModels.isNotEmpty() && currentTextModel.isEmpty()) {
                currentTextModel = textModels.first()
            }
        }
        viewModelScope.launch {
            repository.getSessions(1).firstOrNull()?.let { s ->
                if (s.isEmpty()) {
                    createNewSession()
                } else {
                    currentSessionId = s.first().id
                }
            }
        }
    }

    fun createNewSession(title: String = "Tạo ảnh mới") {
        viewModelScope.launch {
            currentSessionId = repository.createSession(title, 1)
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

    fun enhancePrompt(content: String, onEnhanced: (String) -> Unit) {
        if (content.isBlank()) return
        viewModelScope.launch {
            isEnhancing = true
            try {
                val systemPrompt = settingsRepository.imageSystemPromptFlow.first()
                val promptInstruction = "$systemPrompt\nTarget render model: $currentImageModel"
                
                val interpolationModel = if (currentTextModel.isNotEmpty()) currentTextModel else "openai"
                val enhancedPrompt = repository.sendMessageSync(content, interpolationModel, promptInstruction)
                
                onEnhanced(enhancedPrompt)
            } catch (e: Exception) {
                onEnhanced(content)
            } finally {
                isEnhancing = false
            }
        }
    }

    fun generateImage(content: String) {
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
                val imageUrl = repository.buildImageUrl(content, currentImageModel)
                repository.saveBotImageMessage(sessionId, imageUrl)
            } catch (e: Exception) {
                repository.saveBotTextMessage(sessionId, "Error: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch { 
            repository.clearAllSessions(1)
            createNewSession()
        }
    }
}

@Composable
fun provideImageRenderViewModel(appRepository: AppRepository, settingsRepository: SettingsRepository): ImageRenderViewModel {
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ImageRenderViewModel(appRepository, settingsRepository) as T
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageRenderScreen(appRepository: AppRepository, settingsRepository: SettingsRepository) {
    val viewModel = provideImageRenderViewModel(appRepository, settingsRepository)
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val sessions by viewModel.sessions.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val globalSettings = LocalGlobalSettings.current
    val fontSize by globalSettings.fontSize.collectAsState()
    var showEnhanceModelDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (fullscreenImageUrl != null) {
        FullScreenImageViewer(
            imageUrl = fullscreenImageUrl!!,
            onDismiss = { fullscreenImageUrl = null }
        )
    }

    if (showEnhanceModelDialog) {
        var tempTextModel by remember { mutableStateOf(viewModel.currentTextModel) }
        var tempImageModel by remember { mutableStateOf(viewModel.currentImageModel) }
        var expandedText by remember { mutableStateOf(false) }
        var expandedRender by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEnhanceModelDialog = false },
            title = { Text("Tùy chọn Nội suy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Model Nội suy (Text)", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = expandedText,
                            onExpandedChange = { expandedText = it }
                        ) {
                            OutlinedTextField(
                                value = tempTextModel,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedText,
                                onDismissRequest = { expandedText = false }
                            ) {
                                viewModel.textModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            tempTextModel = model
                                            expandedText = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        Text("Model Tạo ảnh (Render)", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = expandedRender,
                            onExpandedChange = { expandedRender = it }
                        ) {
                            OutlinedTextField(
                                value = tempImageModel,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedRender,
                                onDismissRequest = { expandedRender = false }
                            ) {
                                viewModel.imageModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            tempImageModel = model
                                            expandedRender = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.currentTextModel = tempTextModel
                        viewModel.currentImageModel = tempImageModel
                        viewModel.enhancePrompt(inputText) { newPrompt ->
                            inputText = newPrompt
                        }
                        showEnhanceModelDialog = false
                    }
                ) {
                    Text("Nội suy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnhanceModelDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Xác nhận") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ lịch sử tạo ảnh không?") },
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
                    title = { Text("Tạo Ảnh") },
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages) { message ->
                        CollapsibleMessageBubble(
                            message = message, 
                            fontSize = fontSize,
                            onImageClick = { fullscreenImageUrl = it }
                        )
                    }
                    if (viewModel.isLoading || viewModel.isEnhancing) {
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
                        placeholder = { Text("Nhập mô tả ảnh...") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            showEnhanceModelDialog = true
                        },
                        enabled = inputText.isNotBlank() && !viewModel.isEnhancing && !viewModel.isLoading
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Nội suy Prompt")
                    }
                    IconButton(
                        onClick = {
                            viewModel.generateImage(inputText)
                            inputText = ""
                        },
                        enabled = inputText.isNotBlank() && !viewModel.isLoading && !viewModel.isEnhancing
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Tạo")
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleMessageBubble(
    message: MessageEntity,
    fontSize: Float,
    onImageClick: ((String) -> Unit)? = null
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = bgColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (message.type == 0) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val isLongText = message.content.length > 60
                    val textToShow = if (expanded || !isLongText) {
                        message.content
                    } else {
                        message.content.take(60) + "..."
                    }
                    
                    Text(
                        text = textToShow,
                        color = textColor,
                        fontSize = fontSize.sp
                    )
                    
                    if (isLongText) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { expanded = !expanded },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (expanded) "Thu gọn" else "Mở rộng",
                                    tint = textColor
                                )
                            }
                        }
                    }
                }
            } else if (message.type == 1) {
                AsyncImage(
                    model = message.content,
                    contentDescription = "Generated Image",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .then(if (onImageClick != null) Modifier.clickable { onImageClick(message.content) } else Modifier)
                )
            }
        }
    }
}

@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                }
                IconButton(onClick = {
                    downloadImageSafe(context, scope, imageUrl)
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Tải xuống", tint = Color.White)
                }
            }
        }
    }
}

fun downloadImageSafe(context: Context, scope: CoroutineScope, urlStr: String) {
    Toast.makeText(context, "Đang khởi tạo tệp tải xuống...", Toast.LENGTH_SHORT).show()
    scope.launch(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val connection = url.openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            val filename = "Image_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
            }
            
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Đã lưu ảnh vào Thư viện!", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Không thể lưu ảnh (vấn đề phân quyền bộ nhớ)", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Lỗi tải ảnh: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

