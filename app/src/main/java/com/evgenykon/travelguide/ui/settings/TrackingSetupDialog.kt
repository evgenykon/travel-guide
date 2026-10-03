package com.evgenykon.travelguide.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.data.prefs.AppSettings
import com.evgenykon.travelguide.location.ReadinessAction
import com.evgenykon.travelguide.location.ReadinessItem
import com.evgenykon.travelguide.location.TrackingReadiness
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackingSetupViewModel(private val container: AppContainer) : ViewModel() {

    val settings = container.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setAutostartConfirmed(value: Boolean) {
        viewModelScope.launch { container.settingsStore.setXiaomiAutostartConfirmed(value) }
    }

    fun setBatteryConfirmed(value: Boolean) {
        viewModelScope.launch { container.settingsStore.setXiaomiBatteryConfirmed(value) }
    }

    fun setTrackingEnabled(value: Boolean) {
        viewModelScope.launch { container.settingsStore.setTrackingEnabled(value) }
    }
}

@Composable
fun TrackingSetupDialog(
    container: AppContainer,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    val vm: TrackingSetupViewModel =
        viewModel(initializer = { TrackingSetupViewModel(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var items by remember { mutableStateOf<List<ReadinessItem>>(emptyList()) }

    fun refresh() {
        val current = vm.settings.value
        items = TrackingReadiness.evaluate(
            context,
            current.xiaomiAutostartConfirmed,
            current.xiaomiBatteryConfirmed
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh() }

    LaunchedEffect(settings.xiaomiAutostartConfirmed, settings.xiaomiBatteryConfirmed) {
        refresh()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun handleAction(item: ReadinessItem) {
        when {
            item.id == TrackingReadiness.ID_LOCATION ->
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            item.id == TrackingReadiness.ID_NOTIFICATIONS ->
                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            item.id == TrackingReadiness.ID_BACKGROUND_LOCATION &&
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ->
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            else -> TrackingReadiness.openAction(context, item)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройка слежения") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Чтобы озвучка работала при выключенном экране, включите всё отмеченное ниже.",
                    style = MaterialTheme.typography.bodySmall
                )
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.satisfied) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Cancel
                            },
                            contentDescription = null,
                            tint = if (item.satisfied) {
                                Color(0xFF2E7D32)
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.manual) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { handleAction(item) }) {
                                    Text("Открыть")
                                }
                                Checkbox(
                                    checked = item.satisfied,
                                    onCheckedChange = { checked ->
                                        if (item.id == TrackingReadiness.ID_XIAOMI_AUTOSTART) {
                                            vm.setAutostartConfirmed(checked)
                                        } else {
                                            vm.setBatteryConfirmed(checked)
                                        }
                                    }
                                )
                            }
                        } else if (!item.satisfied) {
                            TextButton(onClick = { handleAction(item) }) {
                                Text("Открыть")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.setTrackingEnabled(true)
                    onStart()
                    onDismiss()
                },
                enabled = TrackingReadiness.canStart(items)
            ) {
                Text("Включить слежение")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
