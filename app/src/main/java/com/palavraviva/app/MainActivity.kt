package com.palavraviva.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.palavraviva.app.data.prefs.AppearanceMode
import com.palavraviva.app.data.prefs.AppearancePrefs
import com.palavraviva.app.ui.PalavraVivaApp
import com.palavraviva.app.ui.theme.PalavraVivaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Preferência de aparência persistida (Automático por padrão,
            // seguindo o modo escuro do sistema).
            val appearance by AppearancePrefs.mode(this)
                .collectAsState(initial = AppearanceMode.AUTO)
            PalavraVivaTheme(appearance) {
                PalavraVivaApp()
            }
        }
    }
}
