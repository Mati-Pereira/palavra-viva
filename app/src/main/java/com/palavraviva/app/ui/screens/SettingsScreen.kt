package com.palavraviva.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.palavraviva.app.PalavraVivaApp
import com.palavraviva.app.data.prefs.AiPrefs
import com.palavraviva.app.data.prefs.AiProvider
import com.palavraviva.app.data.prefs.AppearanceMode
import com.palavraviva.app.data.prefs.AppearancePrefs
import com.palavraviva.app.data.prefs.ReminderPrefs
import com.palavraviva.app.notification.NotificationScheduler
import com.palavraviva.app.ui.theme.Gold
import com.palavraviva.app.ui.theme.Green
import com.palavraviva.app.ui.theme.NightInk
import com.palavraviva.app.ui.theme.Paper
import com.palavraviva.app.ui.theme.Sepia
import com.palavraviva.app.ui.theme.SepiaInk
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Aparência global: carregada do DataStore e aplicada ao tema do app.
    var appearance by remember { mutableStateOf(AppearanceMode.AUTO) }
    var fontSize by remember { mutableStateOf(17f) }
    var versesPerLine by remember { mutableStateOf(true) }

    LaunchedEffect(context) {
        AppearancePrefs.mode(context).collect { appearance = it }
    }

    // Lembrete diário (WorkManager), persistido no DataStore.
    val reminderFlow = remember(context) { ReminderPrefs.enabled(context) }
    val reminderEnabled by reminderFlow.collectAsState(initial = true)
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun setReminder(enabled: Boolean) {
        // NonCancellable: a escrita sobrevive se o usuário sair da tela no
        // mesmo instante — prefs e WorkManager ficam sempre em sync.
        scope.launch {
            withContext(NonCancellable) { ReminderPrefs.setEnabled(context, enabled) }
        }
        if (enabled) {
            NotificationScheduler.ensureScheduled(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            NotificationScheduler.cancel(context)
        }
    }

    fun selectAppearance(mode: AppearanceMode) {
        appearance = mode
        scope.launch { AppearancePrefs.setMode(context, mode) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            }
            Text("Aparência & leitura", style = MaterialTheme.typography.titleMedium)
        }

        // Aparência (Automático / Claro / Sépia / Escuro)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("APARÊNCIA", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppearanceMode.entries.forEach { mode ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                onClick = { selectAppearance(mode) },
                                shape = RoundedCornerShape(10.dp),
                                border = if (appearance == mode)
                                    BorderStroke(2.dp, Gold)
                                else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                AppearancePreview(mode)
                            }
                            Spacer(Modifier.height(8.dp))
                            FilterChip(
                                selected = appearance == mode,
                                onClick = { selectAppearance(mode) },
                                label = { Text(mode.label, fontSize = 11.sp) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Automático segue o modo escuro do sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Tamanho da fonte
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("TAMANHO DA FONTE", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 14f..24f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold),
                    )
                    Text("A", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                }
                Text(
                    "Prévia: Porque Deus amou o mundo…",
                    fontSize = fontSize.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Opções
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                SettingRow("Versículos em linhas", "Um versículo por linha", versesPerLine) { versesPerLine = it }
                Spacer(Modifier.height(12.dp))
                SettingRow("Lembrete diário", "Notificação às 9h com sua sequência", reminderEnabled) {
                    setReminder(it)
                }
            }
        }

        // Versão da Bíblia + atribuição obrigatória da licença (CC BY 3.0)
        val uriHandler = LocalUriHandler.current
        AiStudySettingsCard(uriHandler = uriHandler)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Versão da Bíblia", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Bíblia Livre",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Texto bíblico: Bíblia Livre (BLIVRE), licença CC BY 3.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "creativecommons.org/licenses/by/3.0/br",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            runCatching { uriHandler.openUri("https://creativecommons.org/licenses/by/3.0/br/") }
                        },
                    )
                }
                Text(
                    "BLIVRE",
                    color = Gold,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private val InkDark = Color(0xFF1B1B33)
private val InkLight = Color(0xFFF0EDF7)
private val fractions = listOf(0.7f, 0.55f, 0.6f)

/** Miniatura do tema: "linhas de texto" sobre a cor de fundo. */
@Composable
private fun AppearancePreview(mode: AppearanceMode) {
    Column(Modifier.fillMaxWidth().height(56.dp)) {
        when (mode) {
            AppearanceMode.AUTO -> {
                // Metade clara + metade escura: segue o sistema.
                PreviewHalf(Paper, InkDark)
                PreviewHalf(NightInk, InkLight)
            }
            AppearanceMode.LIGHT -> PreviewBody(Paper, InkDark)
            AppearanceMode.SEPIA -> PreviewBody(Sepia, SepiaInk)
            AppearanceMode.DARK -> PreviewBody(NightInk, InkLight)
        }
    }
}

@Composable
private fun PreviewBody(bg: Color, fg: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(bg)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        fractions.forEachIndexed { i, f ->
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .height(6.dp)
                    .background(fg.copy(alpha = if (i == 0) 1f else 0.3f), RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun PreviewHalf(bg: Color, fg: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .height(5.dp)
                .background(fg, RoundedCornerShape(3.dp)),
        )
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .height(5.dp)
                .background(fg.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun AiStudySettingsCard(uriHandler: androidx.compose.ui.platform.UriHandler) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val savedProvider by remember(context) { AiPrefs.provider(context) }
        .collectAsState(initial = AiProvider.GEMINI)
    val savedApiKey by remember(context, savedProvider) { AiPrefs.apiKey(context, savedProvider) }
        .collectAsState(initial = "")
    val savedModel by remember(context, savedProvider) { AiPrefs.model(context, savedProvider) }
        .collectAsState(initial = savedProvider.defaultModel)
    val availableModels by remember(context, savedProvider) { AiPrefs.availableModels(context, savedProvider) }
        .collectAsState(initial = savedProvider.fallbackModels)

    var apiKeyInput by remember(savedProvider, savedApiKey) { mutableStateOf(savedApiKey) }
    var showKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("ASSISTENTE DE ESTUDO (IA)", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Explicações histórico-culturais, termos da época e chat sobre passagens. " +
                    "Tudo que for gerado fica salvo offline no dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Text("PROVEDOR DE IA", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiProvider.entries.forEach { provider ->
                    FilterChip(
                        selected = savedProvider == provider,
                        onClick = {
                            statusMessage = null
                            scope.launch { AiPrefs.setProvider(context, provider) }
                        },
                        label = { Text(provider.label) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    statusMessage = null
                },
                label = { Text("Chave da API (${savedProvider.label})") },
                placeholder = { Text(savedProvider.keyHint) },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        if (apiKeyInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    apiKeyInput = ""
                                    statusMessage = null
                                    scope.launch { AiPrefs.setApiKey(context, savedProvider, "") }
                                },
                            ) {
                                Icon(Icons.Filled.Clear, contentDescription = "Limpar chave")
                            }
                        }
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showKey) "Ocultar chave" else "Mostrar chave",
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            AiPrefs.setApiKey(context, savedProvider, apiKeyInput)
                            statusIsError = false
                            statusMessage = if (apiKeyInput.isBlank()) {
                                "Chave removida."
                            } else {
                                "Chave salva localmente."
                            }
                        }
                    },
                    enabled = !testing && apiKeyInput.trim() != savedApiKey.trim(),
                ) {
                    Text("Salvar")
                }

                Button(
                    onClick = {
                        val candidate = apiKeyInput.trim()
                        if (candidate.isEmpty()) {
                            statusIsError = true
                            statusMessage = "Informe uma chave da API (${savedProvider.label}) primeiro."
                            return@Button
                        }
                        testing = true
                        statusMessage = null
                        scope.launch {
                            val res = PalavraVivaApp.studyRepository.testAndFetchModels(savedProvider, candidate)
                            testing = false
                            res.onSuccess { models ->
                                statusIsError = false
                                statusMessage = "${savedProvider.label} OK · ${models.size} modelos disponíveis."
                            }.onFailure { err ->
                                statusIsError = true
                                statusMessage = err.message ?: "Não foi possível validar a chave."
                            }
                        }
                    },
                    enabled = !testing,
                    modifier = Modifier.weight(1f),
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (testing) "Testando..." else "Testar conexão")
                }
            }

            statusMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (statusIsError) MaterialTheme.colorScheme.error else Green,
                )
            }

            Spacer(Modifier.height(14.dp))
            Text("MODELO (${savedProvider.label.uppercase()})", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            val modelChoices = remember(availableModels, savedModel) {
                if (savedModel in availableModels) availableModels else listOf(savedModel) + availableModels
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                modelChoices.forEach { modelId ->
                    FilterChip(
                        selected = savedModel == modelId,
                        onClick = { scope.launch { AiPrefs.setModel(context, savedProvider, modelId) } },
                        label = { Text(modelId, fontSize = 12.sp) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                savedProvider.keyUrlLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    runCatching { uriHandler.openUri(savedProvider.keyUrl) }
                },
            )
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
