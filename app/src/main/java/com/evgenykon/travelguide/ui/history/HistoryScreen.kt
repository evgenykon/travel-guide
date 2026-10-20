package com.evgenykon.travelguide.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.R
import com.evgenykon.travelguide.data.db.HistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class HistoryViewModel(private val container: AppContainer) : ViewModel() {

    val entries = container.historyRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(entry: HistoryEntity) {
        viewModelScope.launch { container.historyRepository.delete(entry) }
    }

    fun clear() {
        viewModelScope.launch { container.historyRepository.clear() }
    }

    suspend fun speak(entry: HistoryEntity): Result<Unit> {
        val point = entry.pointId?.let { container.pointRepository.get(it) }
        val text = point?.description?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException(container.context.getString(R.string.history_unavailable)))
        val settings = container.settingsStore.settings.first()
        return container.ttsRepository.speak(text, settings.voice, settings.speed.toDouble())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(container: AppContainer) {
    val vm: HistoryViewModel = viewModel(initializer = { HistoryViewModel(container) })
    val entries by vm.entries.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("dd.MM.yyyy HH:mm", locale) }
    val playErrorMessage = stringResource(R.string.history_play_error)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.common_history)) },
                actions = {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { vm.clear() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.history_clear_cd))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.pointName, style = MaterialTheme.typography.titleMedium)
                                val kindLabel = if (entry.kind == "ENTER") stringResource(R.string.history_kind_enter) else entry.kind
                                val routeSuffix = entry.routeName?.let { " · $it" }.orEmpty()
                                Text(
                                    "$kindLabel$routeSuffix",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    dateFormat.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    vm.speak(entry).onFailure {
                                        snackbarHostState.showSnackbar(
                                            it.message ?: playErrorMessage
                                        )
                                    }
                                }
                            }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.history_listen))
                            }
                            IconButton(onClick = { vm.delete(entry) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.history_delete))
                            }
                        }
                    }
                }
            }
        }
    }
}
