package com.evgenykon.travelguide.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.BuildConfig
import com.evgenykon.travelguide.data.backup.ImportResult
import com.evgenykon.travelguide.data.prefs.AppSettings
import com.evgenykon.travelguide.location.TrackingService
import com.evgenykon.travelguide.location.hasLocationPermission
import com.evgenykon.travelguide.location.trackingPermissions
import com.evgenykon.travelguide.network.ModelDto
import com.evgenykon.travelguide.network.VoiceDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings = container.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val cacheSizeState = MutableStateFlow(0L)
    val cacheSize = cacheSizeState.asStateFlow()

    private val voicesState = MutableStateFlow<List<VoiceDto>>(emptyList())
    val voices = voicesState.asStateFlow()

    private val modelsState = MutableStateFlow<List<ModelDto>>(emptyList())
    val models = modelsState.asStateFlow()

    private val loadingVoicesState = MutableStateFlow(false)
    val loadingVoices = loadingVoicesState.asStateFlow()

    private val loadingModelsState = MutableStateFlow(false)
    val loadingModels = loadingModelsState.asStateFlow()

    private val yandexConnectedState = MutableStateFlow(container.yandexAuth.isConnected)
    val yandexConnected = yandexConnectedState.asStateFlow()

    private val orConnectedState = MutableStateFlow(container.aiRepository.isConnected)
    val orConnected = orConnectedState.asStateFlow()

    private val messageState = MutableStateFlow<String?>(null)
    val message = messageState.asStateFlow()

    init {
        refreshCacheSize()
        loadVoices()
        loadModels()
    }

    fun clearMessage() {
        messageState.value = null
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            cacheSizeState.value = container.ttsRepository.cacheSizeBytes()
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = container.ttsRepository.clearCache()
            cacheSizeState.value = container.ttsRepository.cacheSizeBytes()
            messageState.value = "Удалено файлов: $deleted"
        }
    }

    fun loadVoices() {
        viewModelScope.launch {
            loadingVoicesState.value = true
            voicesState.value = container.ttsRepository.listVoices()
            loadingVoicesState.value = false
        }
    }

    fun loadModels() {
        if (!container.aiRepository.isConnected) return
        viewModelScope.launch {
            loadingModelsState.value = true
            runCatching { container.aiRepository.listModels() }
                .onSuccess { modelsState.value = it }
                .onFailure { messageState.value = it.message ?: "Не удалось загрузить модели" }
            loadingModelsState.value = false
        }
    }

    fun setVoice(voice: String) {
        viewModelScope.launch { container.settingsStore.setVoice(voice) }
    }

    fun setModel(model: String) {
        viewModelScope.launch { container.settingsStore.setModel(model) }
    }

    fun setSpeed(speed: Float) {
        viewModelScope.launch { container.settingsStore.setSpeed(speed) }
    }

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch { container.settingsStore.setAutoPlay(enabled) }
    }

    fun setStyleUrl(url: String) {
        viewModelScope.launch { container.settingsStore.setStyleUrl(url) }
    }

    fun setTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsStore.setTrackingEnabled(enabled) }
    }

    fun testVoice() {
        val current = settings.value
        viewModelScope.launch {
            container.ttsRepository
                .speak("Привет! Это тест озвучки Eff Travel Guide.", current.voice, current.speed.toDouble())
                .onFailure { messageState.value = it.message ?: "Не удалось озвучить" }
        }
    }

    fun showMessage(text: String) {
        messageState.value = text
    }

    suspend fun exportJson(): String = container.backupRepository.exportJson()

    suspend fun importJson(text: String): ImportResult = container.backupRepository.importJson(text)

    fun refreshConnections() {
        yandexConnectedState.value = container.yandexAuth.isConnected
        orConnectedState.value = container.aiRepository.isConnected
        if (container.aiRepository.isConnected) loadModels()
    }

    fun disconnectYandex() {
        container.yandexAuth.disconnect()
        yandexConnectedState.value = false
    }

    fun disconnectOpenRouter() {
        container.aiRepository.disconnect()
        orConnectedState.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, navController: NavController) {
    val vm: SettingsViewModel = viewModel(initializer = { SettingsViewModel(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val yandexConnected by vm.yandexConnected.collectAsStateWithLifecycle()
    val orConnected by vm.orConnected.collectAsStateWithLifecycle()
    val voices by vm.voices.collectAsStateWithLifecycle()
    val models by vm.models.collectAsStateWithLifecycle()
    val loadingVoices by vm.loadingVoices.collectAsStateWithLifecycle()
    val loadingModels by vm.loadingModels.collectAsStateWithLifecycle()
    val cacheSize by vm.cacheSize.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val tracking by TrackingService.isRunning.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showVoiceDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refreshConnections() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (context.hasLocationPermission()) {
            TrackingService.start(context)
            vm.setTrackingEnabled(true)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Нужно разрешение на геолокацию")
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = vm.exportJson()
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Не удалось открыть файл")
                }.onSuccess {
                    snackbarHostState.showSnackbar("Экспорт завершён")
                }.onFailure {
                    snackbarHostState.showSnackbar(it.message ?: "Ошибка экспорта")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: error("Не удалось прочитать файл")
                    vm.importJson(text)
                }.onSuccess { result ->
                    snackbarHostState.showSnackbar(
                        "Импорт: маршрутов +${result.routesAdded}, " +
                            "точек +${result.pointsAdded}, пропущено ${result.pointsSkipped}"
                    )
                }.onFailure {
                    snackbarHostState.showSnackbar("Ошибка импорта: ${it.message ?: "неизвестная"}")
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройки") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsSection("Yandex SpeechKit") {
                ConnectionStatus(connected = yandexConnected)
                Button(
                    onClick = { navController.navigate("auth/yandex") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (yandexConnected) "Управлять подключением" else "Подключить key.json")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showVoiceDialog = true },
                        enabled = yandexConnected
                    ) {
                        Text("Голос: ${settings.voice}")
                    }
                    Spacer(Modifier.width(8.dp))
                    if (loadingVoices) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                SpeedSlider(
                    speed = settings.speed,
                    onSpeedChange = { vm.setSpeed(it) }
                )
                OutlinedButton(
                    onClick = { vm.testVoice() },
                    enabled = yandexConnected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Проверить озвучку")
                }
            }

            SettingsSection("OpenRouter") {
                ConnectionStatus(connected = orConnected)
                Button(
                    onClick = { navController.navigate("auth/openrouter") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (orConnected) "Управлять подключением" else "Войти через OpenRouter")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            vm.loadModels()
                            showModelDialog = true
                        },
                        enabled = orConnected
                    ) {
                        Text("Модель: ${settings.model}")
                    }
                    Spacer(Modifier.width(8.dp))
                    if (loadingModels) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }

            SettingsSection("Озвучка") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Автовоспроизведение", Modifier.weight(1f))
                    Switch(
                        checked = settings.autoPlay,
                        onCheckedChange = { vm.setAutoPlay(it) }
                    )
                }
                Text(
                    "Кэш озвучки: ${formatSize(cacheSize)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = { vm.clearCache() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Очистить кэш озвучки")
                }
            }

            SettingsSection("Слежение") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Слежение за геопозицией", Modifier.weight(1f))
                    Switch(
                        checked = tracking,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (context.hasLocationPermission()) {
                                    TrackingService.start(context)
                                    vm.setTrackingEnabled(true)
                                } else {
                                    permissionLauncher.launch(trackingPermissions())
                                }
                            } else {
                                TrackingService.stop(context)
                                vm.setTrackingEnabled(false)
                            }
                        }
                    )
                }
                Text(
                    "При входе в 10-метровую зону точки приложение озвучит её описание один раз. " +
                        "Повторная озвучка — после удаления записи в «Истории». " +
                        "Работает при выключенном экране (постоянное уведомление).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection("Карта") {
                OutlinedTextField(
                    value = settings.styleUrl,
                    onValueChange = { vm.setStyleUrl(it) },
                    label = { Text("URL стиля MapLibre (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    "Пусто — встроенный стиль OpenStreetMap",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection("Данные: импорт и экспорт") {
                Text(
                    "Сохраняет маршруты и точки (включая описания) в JSON-файл. " +
                        "Импорт добавляет данные и пропускает уже существующие точки.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        exportLauncher.launch(
                            "eff-travel-guide-${System.currentTimeMillis()}.json"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Экспорт маршрутов и точек")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Импорт маршрутов и точек")
                }
            }

            SettingsSection("О приложении") {
                Text("Eff Travel Guide ${BuildConfig.VERSION_NAME}")
                Text(
                    "MapLibre · Yandex SpeechKit · OpenRouter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = { Text("Голос озвучки") },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(voices, key = { it.name }) { voice ->
                        TextButton(
                            onClick = {
                                vm.setVoice(voice.name)
                                showVoiceDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(voice.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) { Text("Закрыть") }
            }
        )
    }

    if (showModelDialog) {
        var query by remember { mutableStateOf("") }
        val filtered = models.filter {
            query.isBlank() ||
                it.id.contains(query, ignoreCase = true) ||
                it.title.contains(query, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            title = { Text("Модель OpenRouter") },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Поиск") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (loadingModels) {
                        CircularProgressIndicator(
                            Modifier
                                .padding(top = 16.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    LazyColumn(
                        Modifier
                            .padding(top = 8.dp)
                            .heightIn(max = 360.dp)
                    ) {
                        items(filtered, key = { it.id }) { model ->
                            TextButton(
                                onClick = {
                                    vm.setModel(model.id)
                                    showModelDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(model.id, style = MaterialTheme.typography.bodyMedium)
                                    model.name?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelDialog = false }) { Text("Закрыть") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ConnectionStatus(connected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (connected) "● Подключено" else "● Не подключено",
            color = if (connected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SpeedSlider(speed: Float, onSpeedChange: (Float) -> Unit) {
    var local by remember { mutableFloatStateOf(speed) }
    LaunchedEffect(speed) { local = speed }
    Column {
        Text(
            "Скорость речи: ${String.format(Locale.US, "%.2f", local)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onSpeedChange(local) },
            valueRange = 0.5f..1.5f
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "0 Б"
    bytes < 1024L -> "$bytes Б"
    bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f КБ", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f МБ", bytes / 1024.0 / 1024.0)
}
