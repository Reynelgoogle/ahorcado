package com.aistudio.ahorcado

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aistudio.ahorcado.data.GameRepository
import com.aistudio.ahorcado.ui.theme.AhorcadoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameRepository.attachAppContext(this)
        enableEdgeToEdge()
        setContent {
            AhorcadoTheme {
                GameApp()
            }
        }
    }
}
