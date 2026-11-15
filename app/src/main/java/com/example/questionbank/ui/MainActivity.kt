package com.example.questionbank.ui

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.questionbank.QuestionBankApp
import com.example.questionbank.ui.theme.QuestionBankTheme

class MainActivity : ComponentActivity() {
    private var sharedImageUri by mutableStateOf<Uri?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedImageUri = sharedImageFrom(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedImageUri = sharedImageFrom(intent)
        
        val app = application as QuestionBankApp
        val vaultDao = app.database.vaultDao()
        val questionDao = app.database.questionDao()
        
        val preferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        setContent {
            var darkTheme by remember { mutableStateOf(preferences.getBoolean("dark_theme", true)) }
            var darcula by remember { mutableStateOf(preferences.getBoolean("darcula_theme", false)) }
            QuestionBankTheme(darkTheme = darkTheme, darcula = darcula) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(vaultDao, questionDao, darkTheme, darcula, { enabled ->
                        darkTheme = enabled
                        preferences.edit().putBoolean("dark_theme", enabled).apply()
                    }, onDarculaChange = { enabled ->
                        darcula = enabled
                        preferences.edit().putBoolean("darcula_theme", enabled).apply()
                    }, sharedImageUri = sharedImageUri, onSharedImageConsumed = { sharedImageUri = null })
                }
            }
        }
    }
}


private fun sharedImageFrom(intent: Intent?): Uri? {
    if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("image/") != true) return null
    return if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
    }
}
