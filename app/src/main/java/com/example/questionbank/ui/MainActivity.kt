package com.example.questionbank.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.questionbank.QuestionBankApp
import com.example.questionbank.ui.theme.QuestionBankTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as QuestionBankApp
        val vaultDao = app.database.vaultDao()
        val questionDao = app.database.questionDao()
        
        val preferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        setContent {
            var darkTheme by remember { mutableStateOf(preferences.getBoolean("dark_theme", true)) }
            QuestionBankTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(vaultDao, questionDao, darkTheme) { enabled ->
                        darkTheme = enabled
                        preferences.edit().putBoolean("dark_theme", enabled).apply()
                    }
                }
            }
        }
    }
}
