package com.evgenykon.travelguide.ui.map

import android.Manifest
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.data.db.PointEntity
import com.evgenykon.travelguide.data.db.RouteEntity
import com.evgenykon.travelguide.location.hasLocationPermission
import com.evgenykon.travelguide.tts.PlaybackState
import com.evgenykon.travelguide.util.Geo
import com.evgenykon.travelguide.util.MapPoi
import com.evgenykon.travelguide.util.MapPoiExtractor
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import java.util.Locale
import kotlin.math.roundToInt

private const val SOURCE_POINTS_ENABLED = "points-enabled"
private const val SOURCE_POINTS_DISABLED = "points-disabled"
private const val SOURCE_RADIUS_ALL = "radius-all"
private const val SOURCE_RADIUS_SELECTED = "radius-selected"
private const val SOURCE_USER = "user"
private const val LAYER_RADIUS_ALL_FILL = "radius-all-fill"
private const val LAYER_RADIUS_ALL_LINE = "radius-all-line"
private const val LAYER_RADIUS_SELECTED_FILL = "radius-selected-fill"
private const val LAYER_RADIUS_SELECTED_LINE = "radius-selected-line"
private const val LAYER_POINTS_DISABLED = "points-disabled-layer"
private const val LAYER_POINTS_ENABLED = "points-enabled-layer"
private const val LAYER_USER = "user-layer"
private const val PROP_ID = "id"
private const val SELECTED_ZOOM = 18.0

private val DEFAULT_TARGET = LatLng(55.751244, 37.618423)

@Composable
fun MapScreen(container: AppContainer, navController: NavController) {
    val vm: MapViewModel = viewModel(initializer = { MapViewModel(container) })
    val context = LocalContext.current

    val routes by vm.routes.collectAsStateWithLifecycle()
    val visiblePoints by vm.visiblePoints.collectAsStateWithLifecycle()
    val visitedPointIds by vm.visitedPointIds.collectAsStateWithLifecycle()
    val selectedPoint by vm.selectedPoint.collectAsStateWithLifecycle()
    val selectedPoi by vm.selectedPoi.collectAsStateWithLifecycle()
    val selectedElevation by vm.selectedElevation.collectAsStateWithLifecycle()
    val elevationLoading by vm.elevationLoading.collectAsStateWithLifecycle()
    val address by vm.address.collectAsStateWithLifecycle()
    val createRequest by vm.createRequest.collectAsStateWithLifecycle()
    val previewRadius by vm.previewRadius.collectAsStateWithLifecycle()
    val sheetOpen by vm.sheetOpen.collectAsStateWithLifecycle()
    val userLocation by vm.userLocation.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pendingRouteId by container.pendingRouteId.collectAsStateWithLifecycle()
    val playbackState by container.audioPlayer.state.collectAsStateWithLifecycle()
    val playbackLabel by container.audioPlayer.label.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = context.hasLocationPermission()
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(hasPermission) {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { vm.onUserLocation(it.latitude, it.longitude) }
            }
        }
        if (hasPermission) {
            container.locationProvider.requestUpdates(2_000L, 1f, callback)
        }
        onDispose {
            container.locationProvider.removeUpdates(callback)
        }
    }

    val mapView = rememberMapViewWithLifecycle()
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleRef by remember { mutableStateOf<Style?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var centeredTarget by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val styleUrl = settings.styleUrl

    LaunchedEffect(mapView, styleUrl) {
        mapView.getMapAsync { map ->
            mapRef = map
            val styleBuilder = if (styleUrl.isBlank()) {
                val json = context.assets.open("map_style.json")
                    .bufferedReader()
                    .use { it.readText() }
                Style.Builder().fromJson(json)
            } else {
                Style.Builder().fromUri(styleUrl)
            }
            map.setStyle(styleBuilder) { style ->
                setupLayers(style)
                styleRef = style
                mapReady = true
            }
            val savedLat = vm.settings.value.lastLat
            val savedLng = vm.settings.value.lastLng
            val initialTarget = if (savedLat != null && savedLng != null) {
                LatLng(savedLat, savedLng)
            } else {
                DEFAULT_TARGET
            }
            map.cameraPosition = CameraPosition.Builder()
                .target(initialTarget)
                .zoom(if (savedLat != null) 15.0 else 10.0)
                .build()
            if (context.hasLocationPermission()) {
                container.locationProvider.lastLocation().addOnSuccessListener { location ->
                    if (location != null && centeredTarget == null) {
                        centeredTarget = location.latitude to location.longitude
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(15.0)
                            .build()
                    }
                }
            }
            map.uiSettings.isAttributionEnabled = true
            map.uiSettings.isCompassEnabled = true
            map.addOnMapClickListener { latLng ->
                val screenPoint = map.projection.toScreenLocation(latLng)
                val features = map.queryRenderedFeatures(
                    screenPoint,
                    LAYER_POINTS_ENABLED,
                    LAYER_POINTS_DISABLED
                )
                val id = features.firstOrNull()
                    ?.getStringProperty(PROP_ID)
                    ?.toLongOrNull()
                when {
                    id != null -> vm.selectPoint(id, queryPoi(map, latLng))
                    vm.createRequest.value != null ->
                        vm.startCreate(latLng.latitude, latLng.longitude, queryPoi(map, latLng))
                    else -> vm.selectPoint(null)
                }
                true
            }
            map.addOnMapLongClickListener { latLng ->
                vm.startCreate(latLng.latitude, latLng.longitude, queryPoi(map, latLng))
                true
            }
        }
    }

    LaunchedEffect(mapReady, visiblePoints) {
        if (!mapReady) return@LaunchedEffect
        val style = styleRef ?: return@LaunchedEffect
        style.getSourceAs<GeoJsonSource>(SOURCE_POINTS_ENABLED)
            ?.setGeoJson(pointsFeatureCollection(visiblePoints.filter { it.enabled }))
        style.getSourceAs<GeoJsonSource>(SOURCE_POINTS_DISABLED)
            ?.setGeoJson(pointsFeatureCollection(visiblePoints.filter { !it.enabled }))
    }

    LaunchedEffect(mapReady, visiblePoints) {
        if (!mapReady) return@LaunchedEffect
        val style = styleRef ?: return@LaunchedEffect
        style.getSourceAs<GeoJsonSource>(SOURCE_RADIUS_ALL)
            ?.setGeoJson(radiusFeatureCollection(visiblePoints))
    }

    LaunchedEffect(mapReady, selectedPoint, createRequest, previewRadius, settings.radiusMeters) {
        if (!mapReady) return@LaunchedEffect
        val style = styleRef ?: return@LaunchedEffect
        val point = selectedPoint
        val request = createRequest
        val collection = when {
            point != null -> radiusFeatureCollection(point.lat, point.lng, point.radiusMeters)
            request != null -> radiusFeatureCollection(
                request.lat,
                request.lng,
                previewRadius ?: settings.radiusMeters.toDouble()
            )
            else -> FeatureCollection.fromFeatures(emptyList())
        }
        style.getSourceAs<GeoJsonSource>(SOURCE_RADIUS_SELECTED)?.setGeoJson(collection)
    }

    LaunchedEffect(mapReady, userLocation) {
        if (!mapReady) return@LaunchedEffect
        val style = styleRef ?: return@LaunchedEffect
        val location = userLocation
        val collection = if (location == null) {
            FeatureCollection.fromFeatures(emptyList())
        } else {
            FeatureCollection.fromFeatures(
                listOf(Feature.fromGeometry(Point.fromLngLat(location.second, location.first)))
            )
        }
        style.getSourceAs<GeoJsonSource>(SOURCE_USER)?.setGeoJson(collection)

        if (location != null) {
            val centered = centeredTarget
            val farAway = centered == null ||
                Geo.distanceMeters(centered.first, centered.second, location.first, location.second) > 2_000.0
            if (farAway) {
                centeredTarget = location
                mapRef?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.first, location.second),
                        15.0
                    ),
                    600
                )
            }
        }
    }

    LaunchedEffect(mapReady, settings.lastLat, settings.lastLng) {
        if (!mapReady || centeredTarget != null) return@LaunchedEffect
        val lat = settings.lastLat ?: return@LaunchedEffect
        val lng = settings.lastLng ?: return@LaunchedEffect
        centeredTarget = lat to lng
        mapRef?.cameraPosition = CameraPosition.Builder()
            .target(LatLng(lat, lng))
            .zoom(15.0)
            .build()
    }

    LaunchedEffect(mapReady, selectedPoint?.id) {
        if (!mapReady) return@LaunchedEffect
        val point = selectedPoint ?: return@LaunchedEffect
        val map = mapRef ?: return@LaunchedEffect
        val zoom = maxOf(map.cameraPosition.zoom, SELECTED_ZOOM)
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(point.lat, point.lng), zoom),
            600
        )
    }

    LaunchedEffect(settings.routeFilterId) {
        val filter = settings.routeFilterId
        val selected = vm.selectedPoint.value
        if (filter != null && selected != null && selected.routeId != filter) {
            vm.selectPoint(null)
        }
    }

    LaunchedEffect(pendingRouteId) {
        pendingRouteId?.let { vm.setRouteFilter(it) }
    }

    LaunchedEffect(mapReady, settings.routeFilterId, visiblePoints.isEmpty()) {
        if (!mapReady) return@LaunchedEffect
        val filter = settings.routeFilterId ?: return@LaunchedEffect
        val map = mapRef ?: return@LaunchedEffect
        val points = vm.visiblePoints.value.filter { it.routeId == filter }
        if (points.isEmpty()) return@LaunchedEffect
        if (points.size == 1) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(points[0].lat, points[0].lng),
                    SELECTED_ZOOM
                ),
                600
            )
        } else {
            val builder = LatLngBounds.Builder()
            points.forEach { builder.include(LatLng(it.lat, it.lng)) }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120), 600)
        }
    }

    val calloutVisible = selectedPoint != null && createRequest == null && !sheetOpen

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!calloutVisible) {
                Column(horizontalAlignment = Alignment.End) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val target = vm.userLocation.value
                                ?.let { LatLng(it.first, it.second) }
                                ?: mapRef?.cameraPosition?.target
                            if (target != null) {
                                vm.startCreate(
                                    target.latitude,
                                    target.longitude,
                                    mapRef?.let { queryPoi(it, target) }
                                )
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Нажмите и удерживайте карту, чтобы поставить точку"
                                    )
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Точка") }
                    )
                    Spacer(Modifier.height(8.dp))
                    FloatingActionButton(
                        onClick = {
                            val location = vm.userLocation.value
                            if (location != null) {
                                mapRef?.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(location.first, location.second))
                                    .zoom(17.0)
                                    .build()
                            }
                        }
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Моё местоположение")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
            RouteFilterChip(
                routes = routes,
                selectedId = settings.routeFilterId,
                onSelect = { vm.setRouteFilter(it) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            val calloutPoint = selectedPoint
            if (calloutPoint != null && createRequest == null && !sheetOpen) {
                PointCallout(
                    point = calloutPoint,
                    poi = selectedPoi,
                    address = address,
                    elevation = selectedElevation,
                    elevationLoading = elevationLoading,
                    playbackState = playbackState,
                    onPlayPause = {
                        when (playbackState) {
                            PlaybackState.PLAYING -> container.audioPlayer.pause()
                            PlaybackState.PAUSED -> container.audioPlayer.resume()
                            PlaybackState.IDLE -> {
                                val text = calloutPoint.description
                                if (text.isBlank()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("У точки нет описания")
                                    }
                                } else {
                                    scope.launch {
                                        vm.speak(text).onFailure {
                                            snackbarHostState.showSnackbar(
                                                it.message ?: "Не удалось озвучить"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onStop = { container.audioPlayer.stop() },
                    onEdit = { vm.openSheet() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }

            if (playbackState != PlaybackState.IDLE && (calloutPoint == null || sheetOpen)) {
                PlaybackPill(
                    state = playbackState,
                    label = playbackLabel,
                    onPlayPause = {
                        if (playbackState == PlaybackState.PLAYING) {
                            container.audioPlayer.pause()
                        } else {
                            container.audioPlayer.resume()
                        }
                    },
                    onStop = { container.audioPlayer.stop() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 64.dp)
                )
            }
        }
    }

    val sheetPoint = selectedPoint
    if (createRequest != null || (sheetPoint != null && sheetOpen)) {
        PointSheet(
            point = sheetPoint,
            createRequest = createRequest,
            routes = routes,
            defaultRadius = settings.radiusMeters,
            pendingRouteId = pendingRouteId ?: settings.routeFilterId,
            isVisited = sheetPoint?.let { it.id in visitedPointIds } ?: false,
            suggestedPoi = createRequest?.poi ?: selectedPoi,
            address = address,
            onRadiusPreview = { vm.setPreviewRadius(it) },
            onDismiss = {
                vm.selectPoint(null)
                vm.cancelCreate()
                vm.closeSheet()
                container.pendingRouteId.value = null
            },
            onSave = {
                vm.save(it)
                vm.selectPoint(null)
                vm.cancelCreate()
                vm.closeSheet()
                container.pendingRouteId.value = null
            },
            onDelete = {
                vm.delete(it)
                vm.selectPoint(null)
                vm.cancelCreate()
                vm.closeSheet()
                container.pendingRouteId.value = null
            },
            onGenerate = { name, addressText, lat, lng, hint, poi ->
                vm.generateDescription(name, addressText, lat, lng, hint, poi)
            },
            onSpeak = { text -> vm.speak(text) },
            onMessage = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        )
    }
}

private fun queryPoi(map: MapLibreMap, latLng: LatLng): MapPoi? =
    runCatching {
        val screenPoint = map.projection.toScreenLocation(latLng)
        MapPoiExtractor.extract(map.queryRenderedFeatures(screenPoint))
    }.getOrNull()

@Composable
private fun PointCallout(
    point: PointEntity,
    poi: MapPoi?,
    address: String?,
    elevation: Double?,
    elevationLoading: Boolean,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                point.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "OSM: " + (poi
                    ?.let { listOfNotNull(it.name, it.category).joinToString(" — ") }
                    ?: "нет данных"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                String.format(Locale.US, "%.5f, %.5f", point.lat, point.lng),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!address.isNullOrBlank()) {
                Text(
                    "Адрес: $address",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                when {
                    elevationLoading -> "Высота: …"
                    elevation != null -> "Высота: ${elevation.roundToInt()} м"
                    else -> "Высота: нет данных"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (playbackState == PlaybackState.PLAYING) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = "Играть или пауза"
                    )
                }
                IconButton(
                    onClick = onStop,
                    enabled = playbackState != PlaybackState.IDLE
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Остановить")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEdit) {
                    Text("Изменить")
                }
            }
        }
    }
}

@Composable
private fun PlaybackPill(
    state: PlaybackState,
    label: String?,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label ?: "Озвучка",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp)
            )
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (state == PlaybackState.PLAYING) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = "Играть или пауза"
                )
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Stop, contentDescription = "Остановить")
            }
        }
    }
}

@Composable
private fun RouteFilterChip(
    routes: List<RouteEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(
            onClick = { expanded = true },
            label = {
                Text(routes.firstOrNull { it.id == selectedId }?.name ?: "Все точки")
            },
            leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Все точки") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            routes.forEach { route ->
                DropdownMenuItem(
                    text = { Text(route.name) },
                    onClick = {
                        onSelect(route.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onStart()
        mapView.onResume()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
        }
    }
    return mapView
}

private fun setupLayers(style: Style) {
    if (style.getSource(SOURCE_POINTS_ENABLED) == null) {
        style.addSource(
            GeoJsonSource(SOURCE_POINTS_ENABLED, FeatureCollection.fromFeatures(emptyList()))
        )
    }
    if (style.getSource(SOURCE_POINTS_DISABLED) == null) {
        style.addSource(
            GeoJsonSource(SOURCE_POINTS_DISABLED, FeatureCollection.fromFeatures(emptyList()))
        )
    }
    if (style.getSource(SOURCE_RADIUS_ALL) == null) {
        style.addSource(
            GeoJsonSource(SOURCE_RADIUS_ALL, FeatureCollection.fromFeatures(emptyList()))
        )
    }
    if (style.getSource(SOURCE_RADIUS_SELECTED) == null) {
        style.addSource(
            GeoJsonSource(SOURCE_RADIUS_SELECTED, FeatureCollection.fromFeatures(emptyList()))
        )
    }
    if (style.getSource(SOURCE_USER) == null) {
        style.addSource(
            GeoJsonSource(SOURCE_USER, FeatureCollection.fromFeatures(emptyList()))
        )
    }

    if (style.getLayer(LAYER_RADIUS_ALL_FILL) == null) {
        style.addLayer(
            FillLayer(LAYER_RADIUS_ALL_FILL, SOURCE_RADIUS_ALL).withProperties(
                PropertyFactory.fillColor(Color.parseColor("#2E7D32")),
                PropertyFactory.fillOpacity(0.12f)
            )
        )
    }
    if (style.getLayer(LAYER_RADIUS_ALL_LINE) == null) {
        style.addLayer(
            LineLayer(LAYER_RADIUS_ALL_LINE, SOURCE_RADIUS_ALL).withProperties(
                PropertyFactory.lineColor(Color.parseColor("#2E7D32")),
                PropertyFactory.lineWidth(1f),
                PropertyFactory.lineOpacity(0.6f)
            )
        )
    }
    if (style.getLayer(LAYER_RADIUS_SELECTED_FILL) == null) {
        style.addLayer(
            FillLayer(LAYER_RADIUS_SELECTED_FILL, SOURCE_RADIUS_SELECTED).withProperties(
                PropertyFactory.fillColor(Color.parseColor("#2E7D32")),
                PropertyFactory.fillOpacity(0.25f)
            )
        )
    }
    if (style.getLayer(LAYER_RADIUS_SELECTED_LINE) == null) {
        style.addLayer(
            LineLayer(LAYER_RADIUS_SELECTED_LINE, SOURCE_RADIUS_SELECTED).withProperties(
                PropertyFactory.lineColor(Color.parseColor("#2E7D32")),
                PropertyFactory.lineWidth(2f)
            )
        )
    }
    if (style.getLayer(LAYER_POINTS_DISABLED) == null) {
        style.addLayer(
            CircleLayer(LAYER_POINTS_DISABLED, SOURCE_POINTS_DISABLED).withProperties(
                PropertyFactory.circleColor(Color.parseColor("#9E9E9E")),
                PropertyFactory.circleRadius(6f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(2f)
            )
        )
    }
    if (style.getLayer(LAYER_POINTS_ENABLED) == null) {
        style.addLayer(
            CircleLayer(LAYER_POINTS_ENABLED, SOURCE_POINTS_ENABLED).withProperties(
                PropertyFactory.circleColor(Color.parseColor("#2E7D32")),
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(2f)
            )
        )
    }
    if (style.getLayer(LAYER_USER) == null) {
        style.addLayer(
            CircleLayer(LAYER_USER, SOURCE_USER).withProperties(
                PropertyFactory.circleColor(Color.parseColor("#1A73E8")),
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(2f)
            )
        )
    }
}

private fun pointsFeatureCollection(points: List<PointEntity>): FeatureCollection =
    FeatureCollection.fromFeatures(
        points.map { point ->
            Feature.fromGeometry(Point.fromLngLat(point.lng, point.lat)).apply {
                addStringProperty(PROP_ID, point.id.toString())
                addStringProperty("name", point.name)
            }
        }
    )

private fun radiusFeatureCollection(points: List<PointEntity>): FeatureCollection =
    FeatureCollection.fromFeatures(
        points.map { point ->
            Feature.fromGeometry(circlePolygon(point.lat, point.lng, point.radiusMeters))
        }
    )

private fun radiusFeatureCollection(lat: Double, lng: Double, radiusMeters: Double): FeatureCollection =
    FeatureCollection.fromFeatures(
        listOf(Feature.fromGeometry(circlePolygon(lat, lng, radiusMeters)))
    )

private fun circlePolygon(lat: Double, lng: Double, radiusMeters: Double): Polygon {
    val ring = Geo.circleRing(lat, lng, radiusMeters)
    val ringPoints = ring.map { Point.fromLngLat(it.second, it.first) }
    val closed = ringPoints + ringPoints.first()
    return Polygon.fromLngLats(listOf(closed))
}
