package com.evgenykon.travelguide.ui.routes

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.data.db.PointEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RouteDetailViewModel(
    private val container: AppContainer,
    private val routeId: Long
) : ViewModel() {

    val route = container.routeRepository.observeById(routeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val points = container.pointRepository.observeByRoute(routeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(point: PointEntity, enabled: Boolean) {
        viewModelScope.launch { container.pointRepository.setEnabled(point.id, enabled) }
    }

    fun removeFromRoute(point: PointEntity) {
        viewModelScope.launch { container.pointRepository.setRoute(point.id, null) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    container: AppContainer,
    navController: NavController,
    routeId: Long
) {
    val vm: RouteDetailViewModel = viewModel(
        key = "route-$routeId",
        initializer = { RouteDetailViewModel(container, routeId) }
    )
    val route by vm.route.collectAsStateWithLifecycle()
    val points by vm.points.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route?.name ?: "Маршрут") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    container.pendingRouteId.value = routeId
                    navController.navigate("map") {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Добавить точку") }
            )
        }
    ) { padding ->
        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("В маршруте пока нет точек", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(points, key = { it.id }) { point ->
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(point.name, style = MaterialTheme.typography.titleMedium)
                                if (point.description.isNotBlank()) {
                                    Text(
                                        point.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Switch(
                                checked = point.enabled,
                                onCheckedChange = { vm.setEnabled(point, it) }
                            )
                            IconButton(onClick = { vm.removeFromRoute(point) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Убрать из маршрута"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
