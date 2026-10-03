package com.evgenykon.travelguide

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.evgenykon.travelguide.ui.AppNav
import com.evgenykon.travelguide.ui.theme.TravelGuideTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TravelGuideApp).container
        handleSharedIntent(intent, container)
        setContent {
            TravelGuideTheme {
                AppNav(container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedIntent(intent, (application as TravelGuideApp).container)
    }

    private fun handleSharedIntent(intent: Intent?, container: AppContainer) {
        if (intent?.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                container.pendingSharedKey.value = text
            }
        }
    }
}
