package com.evgenykon.travelguide.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.evgenykon.travelguide.data.db.PointEntity
import com.evgenykon.travelguide.data.db.RouteEntity
import com.evgenykon.travelguide.util.MapPoi
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointSheet(
    point: PointEntity?,
    createRequest: CreateRequest?,
    routes: List<RouteEntity>,
    defaultRadius: Float,
    pendingRouteId: Long?,
    isVisited: Boolean,
    suggestedPoi: MapPoi?,
    onRadiusPreview: (Double) -> Unit,
    onDismiss: () -> Unit,
    onSave: (PointEntity) -> Unit,
    onDelete: (PointEntity) -> Unit,
    onGenerate: suspend (name: String, lat: Double, lng: Double, hint: String, poi: MapPoi?) -> Result<String>,
    onSpeak: suspend (String) -> Result<Unit>,
    onMessage: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val lat = point?.lat ?: createRequest?.lat ?: 0.0
    val lng = point?.lng ?: createRequest?.lng ?: 0.0
    val initialRadius = point?.radiusMeters ?: defaultRadius.toDouble()

    var name by remember(point?.id, createRequest) {
        mutableStateOf(point?.name ?: suggestedPoi?.name.orEmpty())
    }
    var description by remember(point?.id, createRequest) { mutableStateOf(point?.description ?: "") }
    var enabled by remember(point?.id, createRequest) { mutableStateOf(point?.enabled ?: true) }
    var radius by remember(point?.id, createRequest) { mutableStateOf(initialRadius.toString()) }
    var routeId by remember(point?.id, createRequest) {
        mutableStateOf(point?.routeId ?: pendingRouteId)
    }
    var poi by remember(point?.id, createRequest) { mutableStateOf(suggestedPoi) }
    var routeMenuExpanded by remember { mutableStateOf(false) }
    var generating by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (point == null) "Новая точка" else "Точка",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = String.format(Locale.US, "%.5f, %.5f", lat, lng),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isVisited) {
                Text(
                    "Точка уже озвучена. Чтобы озвучить снова, удалите запись в «Истории».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            poi?.let { objectPoi ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Объект OSM: ${objectPoi.name}" +
                            (objectPoi.category?.let { " — $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { poi = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Убрать объект")
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        generating = true
                        scope.launch {
                            onGenerate(name, lat, lng, description, poi)
                                .onSuccess { description = it }
                                .onFailure { onMessage(it.message ?: "Не удалось сгенерировать описание") }
                            generating = false
                        }
                    },
                    enabled = !generating
                ) {
                    if (generating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Сгенерировать")
                }
                OutlinedButton(
                    onClick = {
                        speaking = true
                        scope.launch {
                            onSpeak(description)
                                .onFailure { onMessage(it.message ?: "Не удалось озвучить") }
                            speaking = false
                        }
                    },
                    enabled = description.isNotBlank() && !speaking
                ) {
                    if (speaking) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Прослушать")
                }
            }

            OutlinedTextField(
                value = radius,
                onValueChange = { input ->
                    radius = input.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                    radius.replace(',', '.').toDoubleOrNull()?.let { onRadiusPreview(it) }
                },
                label = { Text("Радиус озвучки, м") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Активна для слежения")
                Spacer(Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Box {
                OutlinedButton(
                    onClick = { routeMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val routeName = routes.firstOrNull { it.id == routeId }?.name
                    Text(routeName ?: "Без маршрута")
                }
                DropdownMenu(
                    expanded = routeMenuExpanded,
                    onDismissRequest = { routeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Без маршрута") },
                        onClick = {
                            routeId = null
                            routeMenuExpanded = false
                        }
                    )
                    routes.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.name) },
                            onClick = {
                                routeId = route.id
                                routeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (point != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        val parsedRadius = radius.replace(',', '.').toDoubleOrNull()
                        onSave(
                            PointEntity(
                                id = point?.id ?: 0L,
                                routeId = routeId,
                                name = name.ifBlank { "Точка" },
                                lat = lat,
                                lng = lng,
                                description = description.trim(),
                                enabled = enabled,
                                radiusMeters = (parsedRadius ?: initialRadius).coerceIn(5.0, 100.0),
                                createdAt = point?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                ) {
                    Text("Сохранить")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmDelete && point != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить точку?") },
            text = { Text("Точка «${point.name}» будет удалена.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(point)
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
