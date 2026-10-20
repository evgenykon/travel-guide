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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.evgenykon.travelguide.R
import com.evgenykon.travelguide.data.db.PointEntity
import com.evgenykon.travelguide.data.db.RouteEntity
import com.evgenykon.travelguide.data.repo.PlaceInfo
import com.evgenykon.travelguide.util.MapPoi
import com.evgenykon.travelguide.util.localizedCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

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
    place: PlaceInfo?,
    onRadiusPreview: (Double) -> Unit,
    onDismiss: () -> Unit,
    onSave: (PointEntity) -> Unit,
    onDelete: (PointEntity) -> Unit,
    onGenerate: suspend (name: String, place: PlaceInfo?, lat: Double, lng: Double, hint: String, poi: MapPoi?) -> Result<String>,
    onSpeak: suspend (String) -> Result<Unit>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val generatedText = stringResource(R.string.point_generated)
    val defaultPointName = stringResource(R.string.point_default_name)
    val unknownError = stringResource(R.string.common_unknown_error)

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null && !statusIsError) {
            delay(4_000)
            statusMessage = null
        }
    }

    val lat = point?.lat ?: createRequest?.lat ?: 0.0
    val lng = point?.lng ?: createRequest?.lng ?: 0.0
    val initialRadius = point?.radiusMeters ?: defaultRadius.toDouble()

    var name by remember(point?.id, createRequest) {
        mutableStateOf(point?.name ?: suggestedPoi?.name.orEmpty())
    }
    var description by remember(point?.id, createRequest) { mutableStateOf(point?.description ?: "") }
    var enabled by remember(point?.id, createRequest) { mutableStateOf(point?.enabled ?: true) }
    var radiusMeters by remember(point?.id, createRequest) {
        mutableFloatStateOf(initialRadius.toFloat().coerceIn(5f, 100f))
    }
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
                text = if (point == null) stringResource(R.string.point_new) else stringResource(R.string.point_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = String.format(Locale.US, "%.5f, %.5f", lat, lng),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!place?.address.isNullOrBlank()) {
                Text(
                    stringResource(R.string.map_address, place?.address.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            place?.location?.let { locationText ->
                Text(
                    stringResource(R.string.map_city_country, locationText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isVisited) {
                Text(
                    stringResource(R.string.point_visited),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            poi?.let { objectPoi ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(
                            R.string.point_osm_object,
                            objectPoi.name + (objectPoi.localizedCategory(locale)?.let { " — $it" } ?: "")
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { poi = null }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.point_remove_object))
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.point_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.point_description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            )

            statusMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color(0xFF2E7D32)
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        generating = true
                        statusMessage = null
                        scope.launch {
                            onGenerate(name, place, lat, lng, description, poi)
                                .onSuccess {
                                    description = it
                                    statusIsError = false
                                    statusMessage = generatedText
                                }
                                .onFailure {
                                    statusIsError = true
                                    statusMessage = context.getString(
                                        R.string.point_generation_error,
                                        it.message ?: unknownError
                                    )
                                }
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
                    Text(stringResource(R.string.point_generate))
                }
                OutlinedButton(
                    onClick = {
                        speaking = true
                        statusMessage = null
                        scope.launch {
                            onSpeak(description)
                                .onFailure {
                                    statusIsError = true
                                    statusMessage =
                                        context.getString(
                                            R.string.point_speak_error,
                                            it.message ?: unknownError
                                        )
                                }
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
                    Text(stringResource(R.string.point_speak))
                }
            }

            Column {
                Text(stringResource(R.string.point_radius, radiusMeters.roundToInt()))
                Slider(
                    value = radiusMeters,
                    onValueChange = {
                        radiusMeters = it
                        onRadiusPreview(it.toDouble())
                    },
                    valueRange = 5f..100f,
                    steps = 18
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.point_active))
                Spacer(Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Box {
                OutlinedButton(
                    onClick = { routeMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val routeName = routes.firstOrNull { it.id == routeId }?.name
                    Text(routeName ?: stringResource(R.string.common_no_route))
                }
                DropdownMenu(
                    expanded = routeMenuExpanded,
                    onDismissRequest = { routeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_no_route)) },
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
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        onSave(
                            PointEntity(
                                id = point?.id ?: 0L,
                                routeId = routeId,
                                name = name.ifBlank { defaultPointName },
                                lat = lat,
                                lng = lng,
                                description = description.trim(),
                                enabled = enabled,
                                radiusMeters = radiusMeters.toDouble().coerceIn(5.0, 100.0),
                                createdAt = point?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmDelete && point != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.point_delete_confirm)) },
            text = { Text(stringResource(R.string.point_delete_message, point.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(point)
                }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
