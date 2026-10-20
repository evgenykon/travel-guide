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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.R
import com.evgenykon.travelguide.data.db.RouteWithCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutesViewModel(private val container: AppContainer) : ViewModel() {

    val routes = container.routeRepository.observeWithCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String) {
        viewModelScope.launch { container.routeRepository.create(name) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { container.routeRepository.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            container.routeRepository.get(id)?.let { container.routeRepository.delete(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(container: AppContainer, navController: NavController) {
    val vm: RoutesViewModel = viewModel(initializer = { RoutesViewModel(container) })
    val routes by vm.routes.collectAsStateWithLifecycle()

    var createDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<RouteWithCount?>(null) }
    var deleteTarget by remember { mutableStateOf<RouteWithCount?>(null) }
    var nameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.common_routes)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                nameInput = ""
                createDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.routes_create_cd))
            }
        }
    ) { padding ->
        if (routes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.routes_empty),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routes, key = { it.id }) { route ->
                    Card(onClick = { navController.navigate("routes/${route.id}") }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(route.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.routes_points_count, route.pointCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                nameInput = route.name
                                renameTarget = route
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_rename))
                            }
                            IconButton(onClick = { deleteTarget = route }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (createDialog) {
        AlertDialog(
            onDismissRequest = { createDialog = false },
            title = { Text(stringResource(R.string.routes_new)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.point_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            vm.create(nameInput.trim())
                        }
                        createDialog = false
                    }
                ) {
                    Text(stringResource(R.string.routes_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { createDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.routes_rename_title)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.point_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            vm.rename(target.id, nameInput.trim())
                        }
                        renameTarget = null
                    }
                ) {
                    Text(stringResource(R.string.routes_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.routes_delete_title)) },
            text = { Text(stringResource(R.string.routes_delete_message, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(target.id)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}
