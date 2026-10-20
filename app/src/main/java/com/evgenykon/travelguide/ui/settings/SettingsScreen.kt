package com.evgenykon.travelguide.ui.settings

import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.R
import com.evgenykon.travelguide.BuildConfig
import com.evgenykon.travelguide.data.backup.ImportResult
import com.evgenykon.travelguide.data.prefs.AppSettings
import com.evgenykon.travelguide.location.TrackingService
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
            messageState.value = container.context.getString(R.string.settings_deleted_files, deleted)
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
                .onFailure {
                    messageState.value = container.context.getString(R.string.settings_models_error)
                }
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

    fun setPromptTemplate(template: String) {
        viewModelScope.launch { container.settingsStore.setPromptTemplate(template) }
    }

    fun resetPromptTemplate() {
        viewModelScope.launch { container.settingsStore.resetPromptTemplate() }
    }

    fun setTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsStore.setTrackingEnabled(enabled) }
    }

    fun testVoice() {
        val current = settings.value
        viewModelScope.launch {
            container.ttsRepository
                .speak(container.context.getString(R.string.settings_test_phrase), current.voice, current.speed.toDouble())
                .onFailure {
                    messageState.value = container.context.getString(R.string.settings_test_error)
                }
        }
    }

    fun showMessage(text: String) {
        messageState.value = text
    }

    suspend fun exportJson(): String = container.backupRepository.exportJson()

    suspend fun importJson(text: String): ImportResult = container.backupRepository.importJson(text)

    suspend fun exportSettingsJson(): String = container.settingsBackupRepository.exportJson()

    suspend fun importSettingsJson(text: String) = container.settingsBackupRepository.importJson(text)

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
    val defaultPromptTemplate = stringResource(R.string.prompt_default)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showVoiceDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showTrackingSetup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refreshConnections() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    val settingsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = vm.exportSettingsJson()
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error(context.getString(R.string.settings_file_open_error))
                }.onSuccess {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_settings_done))
                }.onFailure {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_settings_error))
                }
            }
        }
    }

    val settingsImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: error(context.getString(R.string.settings_file_read_error))
                    vm.importSettingsJson(text)
                }.onSuccess {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_import_settings_done))
                }.onFailure {
                    snackbarHostState.showSnackbar(
                        context.getString(
                            R.string.settings_import_settings_error,
                            it.message ?: context.getString(R.string.common_unknown_error)
                        )
                    )
                }
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
                    } ?: error(context.getString(R.string.settings_file_open_error))
                }.onSuccess {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_done))
                }.onFailure {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_error))
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
                        ?: error(context.getString(R.string.settings_file_read_error))
                    vm.importJson(text)
                }.onSuccess { result ->
                    snackbarHostState.showSnackbar(
                        context.getString(
                            R.string.settings_import_done,
                            result.routesAdded,
                            result.pointsAdded,
                            result.pointsSkipped
                        )
                    )
                }.onFailure {
                    snackbarHostState.showSnackbar(
                        context.getString(
                            R.string.settings_import_error,
                            it.message ?: context.getString(R.string.common_unknown_error)
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
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
                    Text(
                        if (yandexConnected) stringResource(R.string.common_manage_connection)
                        else stringResource(R.string.settings_connect_key)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showVoiceDialog = true },
                        enabled = yandexConnected
                    ) {
                        Text(stringResource(R.string.settings_voice, settings.voice))
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
                    Text(stringResource(R.string.settings_test_voice))
                }
            }

            SettingsSection("OpenRouter") {
                ConnectionStatus(connected = orConnected)
                Button(
                    onClick = { navController.navigate("auth/openrouter") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (orConnected) stringResource(R.string.common_manage_connection)
                        else stringResource(R.string.settings_enter_or)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            vm.loadModels()
                            showModelDialog = true
                        },
                        enabled = orConnected
                    ) {
                        Text(stringResource(R.string.settings_model, settings.model))
                    }
                    Spacer(Modifier.width(8.dp))
                    if (loadingModels) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_playback)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_autoplay), Modifier.weight(1f))
                    Switch(
                        checked = settings.autoPlay,
                        onCheckedChange = { vm.setAutoPlay(it) }
                    )
                }
                Text(
                    stringResource(R.string.settings_cache, formatSize(context, cacheSize)),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = { vm.clearCache() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_clear_cache))
                }
            }

            SettingsSection(stringResource(R.string.settings_tracking)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_tracking_switch), Modifier.weight(1f))
                    Switch(
                        checked = tracking,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showTrackingSetup = true
                            } else {
                                TrackingService.stop(context)
                                vm.setTrackingEnabled(false)
                            }
                        }
                    )
                }
                Text(
                    stringResource(R.string.settings_tracking_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(stringResource(R.string.settings_map)) {
                OutlinedTextField(
                    value = settings.styleUrl,
                    onValueChange = { vm.setStyleUrl(it) },
                    label = { Text(stringResource(R.string.settings_style_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    stringResource(R.string.settings_style_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(stringResource(R.string.settings_prompt)) {
                var promptDraft by remember(settings.promptTemplate) {
                    mutableStateOf(settings.promptTemplate)
                }
                Text(
                    stringResource(R.string.settings_prompt_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = promptDraft,
                    onValueChange = { promptDraft = it },
                    label = { Text(stringResource(R.string.settings_prompt_template)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.setPromptTemplate(promptDraft) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.common_save))
                    }
                    OutlinedButton(
                        onClick = {
                            promptDraft = defaultPromptTemplate
                            vm.resetPromptTemplate()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.settings_reset))
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_data)) {
                Text(
                    stringResource(R.string.settings_data_note),
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
                    Text(stringResource(R.string.settings_export_routes))
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_import_routes))
                }
                Text(
                    stringResource(R.string.settings_backup_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        settingsExportLauncher.launch(
                            "eff-travel-guide-settings-${System.currentTimeMillis()}.json"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_export_settings))
                }
                OutlinedButton(
                    onClick = {
                        settingsImportLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_import_settings))
                }
            }

            SettingsSection(stringResource(R.string.settings_about)) {
                Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME))
                Text(
                    "MapLibre · Yandex SpeechKit · OpenRouter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showTrackingSetup) {
        TrackingSetupDialog(
            container = container,
            onDismiss = { showTrackingSetup = false },
            onStart = { TrackingService.start(context) }
        )
    }

    if (showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = { Text(stringResource(R.string.settings_voice_dialog)) },
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
                            val genderLabel = when (voice.gender?.uppercase()) {
                                "FEMALE" -> stringResource(R.string.voice_female)
                                "MALE" -> stringResource(R.string.voice_male)
                                else -> null
                            }
                            Text(
                                if (genderLabel != null) "${voice.name} ($genderLabel)"
                                else voice.name
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) { Text(stringResource(R.string.common_close)) }
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
            title = { Text(stringResource(R.string.settings_model_dialog)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.common_search)) },
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
                TextButton(onClick = { showModelDialog = false }) { Text(stringResource(R.string.common_close)) }
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
            text = if (connected) stringResource(R.string.common_connected)
            else stringResource(R.string.common_not_connected),
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
            stringResource(R.string.settings_speed, String.format(Locale.US, "%.2f", local)),
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

private fun formatSize(context: Context, bytes: Long): String = when {
    bytes <= 0L -> context.getString(R.string.settings_size_zero)
    bytes < 1024L -> context.getString(R.string.settings_size_bytes, bytes)
    bytes < 1024L * 1024L -> context.getString(
        R.string.settings_size_kb,
        String.format(Locale.US, "%.1f", bytes / 1024.0)
    )
    else -> context.getString(
        R.string.settings_size_mb,
        String.format(Locale.US, "%.1f", bytes / 1024.0 / 1024.0)
    )
}
