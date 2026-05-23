package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    var textModels by mutableStateOf("")
    var imageModels by mutableStateOf("")
    var textSystemPrompt by mutableStateOf("")
    var imageSystemPrompt by mutableStateOf("")
    var fontSize by mutableStateOf(16f)

    init {
        viewModelScope.launch {
            textModels = repository.textModelsFlow.first()
            imageModels = repository.imageModelsFlow.first()
            textSystemPrompt = repository.textSystemPromptFlow.first()
            imageSystemPrompt = repository.imageSystemPromptFlow.first()
            fontSize = repository.fontSizeFlow.first()
        }
    }

    fun save() {
        viewModelScope.launch {
            repository.saveValues(
                textModels = textModels,
                imageModels = imageModels,
                textSystemPrompt = textSystemPrompt,
                imageSystemPrompt = imageSystemPrompt,
                fontSize = fontSize
            )
        }
    }
}

// Simple Factory
@Composable
fun provideSettingsViewModel(repository: SettingsRepository): SettingsViewModel {
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(repository) as T
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsRepository: SettingsRepository) {
    val viewModel = provideSettingsViewModel(settingsRepository)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Cài đặt") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.textModels,
                onValueChange = { viewModel.textModels = it },
                label = { Text("Danh sách Text Models (cách bằng dấu phẩy)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.imageModels,
                onValueChange = { viewModel.imageModels = it },
                label = { Text("Danh sách Image Models (cách bằng dấu phẩy)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.textSystemPrompt,
                onValueChange = { viewModel.textSystemPrompt = it },
                label = { Text("System Prompt cho Chat") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = viewModel.imageSystemPrompt,
                onValueChange = { viewModel.imageSystemPrompt = it },
                label = { Text("System Prompt cho Nội suy Image") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text("Cỡ chữ: ${viewModel.fontSize.toInt()}sp")
            Slider(
                value = viewModel.fontSize,
                onValueChange = { viewModel.fontSize = it },
                valueRange = 12f..30f,
                steps = 18
            )

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu cấu hình")
            }
        }
    }
}
