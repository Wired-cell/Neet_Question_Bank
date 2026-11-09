package com.example.questionbank.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.questionbank.QuestionBankApp
import com.example.questionbank.ui.theme.QuestionBankTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as QuestionBankApp
        val vaultDao = app.database.vaultDao()
        val questionDao = app.database.questionDao()
        
        setContent {
            QuestionBankTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(vaultDao, questionDao)
                }
            }
        }
    }
}
