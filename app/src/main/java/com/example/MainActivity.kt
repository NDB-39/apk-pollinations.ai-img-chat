package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.data.AppRepository
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsRepository
import com.example.data.local.dataStore
import com.example.ui.GlobalSettings
import com.example.ui.NovaNavHost
import com.example.ui.theme.MyApplicationTheme

val LocalGlobalSettings = compositionLocalOf<GlobalSettings> { error("No settings provided") }

class MainActivity : ComponentActivity() {
    private val database by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "nova_db")
            .fallbackToDestructiveMigration()
            .build()
    }
    private val appRepository by lazy {
        AppRepository(database.chatDao())
    }
    private val settingsRepository by lazy {
        SettingsRepository(applicationContext)
    }
    private val globalSettings by lazy {
        GlobalSettings(settingsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalGlobalSettings provides globalSettings) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NovaNavHost(
                            appRepository = appRepository,
                            settingsRepository = settingsRepository
                        )
                    }
                }
            }
        }
    }
}
