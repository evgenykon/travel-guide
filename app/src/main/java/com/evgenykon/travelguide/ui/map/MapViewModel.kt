package com.evgenykon.travelguide.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.data.db.PointEntity
import com.evgenykon.travelguide.data.db.RouteEntity
import com.evgenykon.travelguide.data.prefs.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(private val container: AppContainer) : ViewModel() {

    val points: StateFlow<List<PointEntity>> = container.pointRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routes: StateFlow<List<RouteEntity>> = container.routeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = container.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val visiblePoints: StateFlow<List<PointEntity>> =
        combine(points, settings) { list, current ->
            val filter = current.routeFilterId
            if (filter == null) list else list.filter { it.routeId == filter }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visitedPointIds: StateFlow<Set<Long>> = container.historyRepository
        .observeVisitedPointIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val selectedPointId = MutableStateFlow<Long?>(null)

    val selectedPoint: StateFlow<PointEntity?> =
        combine(points, selectedPointId) { list, id -> list.firstOrNull { it.id == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val createAtState = MutableStateFlow<Pair<Double, Double>?>(null)
    val createAt: StateFlow<Pair<Double, Double>?> = createAtState.asStateFlow()

    private val previewRadiusState = MutableStateFlow<Double?>(null)
    val previewRadius: StateFlow<Double?> = previewRadiusState.asStateFlow()

    val userLocation = MutableStateFlow<Pair<Double, Double>?>(null)

    fun selectPoint(id: Long?) {
        selectedPointId.value = id
    }

    fun startCreate(lat: Double, lng: Double) {
        selectedPointId.value = null
        createAtState.value = lat to lng
    }

    fun cancelCreate() {
        createAtState.value = null
        previewRadiusState.value = null
    }

    fun setPreviewRadius(radius: Double?) {
        previewRadiusState.value = radius
    }

    fun setRouteFilter(id: Long?) {
        viewModelScope.launch {
            container.settingsStore.setRouteFilterId(id)
        }
    }

    fun save(point: PointEntity) {
        viewModelScope.launch {
            container.pointRepository.save(point)
        }
    }

    fun delete(point: PointEntity) {
        viewModelScope.launch {
            container.pointRepository.delete(point)
        }
    }

    fun setEnabled(point: PointEntity, enabled: Boolean) {
        viewModelScope.launch {
            container.pointRepository.setEnabled(point.id, enabled)
        }
    }

    fun onUserLocation(lat: Double, lng: Double) {
        userLocation.value = lat to lng
    }

    suspend fun generateDescription(
        name: String,
        lat: Double,
        lng: Double,
        hint: String
    ): Result<String> = runCatching {
        container.aiRepository.generateDescription(settings.value.model, name, lat, lng, hint)
    }

    suspend fun speak(text: String): Result<Unit> {
        val current = settings.value
        return container.ttsRepository.speak(text, current.voice, current.speed.toDouble())
    }
}
