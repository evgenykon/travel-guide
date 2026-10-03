package com.evgenykon.travelguide.ui.auth

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.evgenykon.travelguide.AppContainer

data class YandexAuthUiState(
    val connected: Boolean = false,
    val serviceAccountId: String? = null,
    val keyId: String? = null,
    val error: String? = null
)

class YandexAuthViewModel(private val container: AppContainer) : ViewModel() {

    private val state = kotlinx.coroutines.flow.MutableStateFlow(buildState())
    val uiState = state

    fun importKey(keyJson: String) {
        container.yandexAuth.importKeyJson(keyJson)
            .onSuccess { state.value = buildState() }
            .onFailure {
                state.value = buildState().copy(error = it.message ?: "Некорректный key.json")
            }
    }

    fun disconnect() {
        container.yandexAuth.disconnect()
        state.value = buildState()
    }

    private fun buildState(): YandexAuthUiState {
        val key = container.yandexAuth.currentKey()
        return YandexAuthUiState(
            connected = container.yandexAuth.isConnected,
            serviceAccountId = key?.serviceAccountId,
            keyId = key?.id
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YandexAuthScreen(container: AppContainer) {
    val vm: YandexAuthViewModel = viewModel(initializer = { YandexAuthViewModel(container) })
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val sharedKey by container.pendingSharedKey.collectAsStateWithLifecycle()
    LaunchedEffect(sharedKey) {
        sharedKey?.let {
            vm.importKey(it)
            container.pendingSharedKey.value = null
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { vm.importKey(readText(context, it)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Yandex SpeechKit") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.connected) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Подключено", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Сервисный аккаунт: ${state.serviceAccountId}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Key ID: ${state.keyId}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Button(
                    onClick = { vm.disconnect() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отключить")
                }
            } else {
                Text(
                    "Подключение выполняется авторизованным ключом сервисного аккаунта Yandex Cloud. " +
                        "Приложение обменяет ключ на IAM-токен и будет автоматически его обновлять.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        picker.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Выбрать key.json")
                }
                OutlinedButton(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Можно также «Поделиться» файлом key.json в приложение")
                }
                Text(
                    "Как получить ключ:\n" +
                        "1. Yandex Cloud → Сервисные аккаунты → создать аккаунт.\n" +
                        "2. Назначить роль ai.speechkit-tts.user.\n" +
                        "3. Создать авторизованный ключ и скачать key.json.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun readText(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull().orEmpty()
