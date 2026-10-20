package com.evgenykon.travelguide.ui.auth

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.R
import com.evgenykon.travelguide.auth.OpenRouterOAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class OpenRouterAuthUiState(
    val connected: Boolean = false,
    val inProgress: Boolean = false,
    val error: String? = null
)

class OpenRouterAuthViewModel(private val container: AppContainer) : ViewModel() {

    private val state = MutableStateFlow(
        OpenRouterAuthUiState(connected = container.aiRepository.isConnected)
    )
    val uiState = state

    fun start(context: Context) {
        if (state.value.inProgress) return
        state.value = OpenRouterAuthUiState(inProgress = true)
        viewModelScope.launch {
            when (val result = container.openRouterOAuth.authorize(context)) {
                is OpenRouterOAuth.AuthResult.Success -> {
                    container.aiRepository.saveKey(result.key)
                    state.value = OpenRouterAuthUiState(connected = true)
                }
                is OpenRouterOAuth.AuthResult.Failure -> {
                    state.value = OpenRouterAuthUiState(error = result.message)
                }
            }
        }
    }

    fun disconnect() {
        container.aiRepository.disconnect()
        state.value = OpenRouterAuthUiState(connected = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenRouterAuthScreen(container: AppContainer) {
    val vm: OpenRouterAuthViewModel = viewModel(initializer = { OpenRouterAuthViewModel(container) })
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("OpenRouter") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.connected -> {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(stringResource(R.string.common_connected), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.or_connected_note),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Button(
                        onClick = { vm.disconnect() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.common_disconnect))
                    }
                }

                state.inProgress -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(Modifier.size(36.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.or_waiting),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    Text(
                        stringResource(R.string.or_login_note),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { vm.start(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.or_login))
                    }
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Button(
                    onClick = { vm.start(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.common_retry))
                }
            }
        }
    }
}
