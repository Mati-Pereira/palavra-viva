package com.palavraviva.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.palavraviva.app.data.prefs.AppearanceMode
import com.palavraviva.app.data.prefs.AppearancePrefs
import com.palavraviva.app.ui.PalavraVivaApp
import com.palavraviva.app.ui.theme.PalavraVivaTheme

class MainActivity : ComponentActivity() {

    // Resultado ignorado: negar apenas silencia o lembrete diário.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) requestNotificationPermissionIfNeeded()
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

    /** API 33+: pede POST_NOTIFICATIONS uma vez por abertura (o sistema limita repetições). */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
