package com.example.communityvoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.communityvoice.ui.CommunityVoiceApp
import com.example.communityvoice.ui.theme.CommunityVoiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Draws behind the status and navigation bars, which is what a current
        // Android app looks like. Scaffold's insets keep content clear of them.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CommunityVoiceTheme {
                CommunityVoiceApp()
            }
        }
    }
}
