package com.example.ui

import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.local.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GlobalSettings(private val settingsRepository: SettingsRepository) {
    private val _fontSize = MutableStateFlow(16f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.fontSizeFlow.collect { size ->
                _fontSize.value = size
            }
        }
    }
}
