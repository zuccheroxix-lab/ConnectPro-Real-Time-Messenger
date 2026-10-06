package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.FirebaseRepository
import com.example.ui.AppNavHost
import com.example.ui.Routes
import com.example.ui.theme.PulseChatTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository = FirebaseRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialConversationId = intent?.getStringExtra("conversationId")

        setContent {
            PulseChatTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        initialRoute = Routes.SPLASH,
                        initialConversationId = initialConversationId
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            repository.updatePresence(isOnline = true)
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            repository.updatePresence(isOnline = false)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
