// bxt/ui/screen/TransportDetailScreen.kt
package com.bxt.ui.screen

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bxt.ui.components.EditLocationPopup
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.state.TransportDetailState
import com.bxt.util.MapboxMarkerUtils
import com.bxt.viewmodel.DeliverableRequest
import com.bxt.viewmodel.FullTransportDetails
import com.bxt.viewmodel.TransportDetailViewModel
import com.bxt.viewmodel.TransportMode
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportDetailScreen(
    navController: NavController,
    viewModel: TransportDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current // Lấy context cho Toast

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.loadAllDetails()
    }
    LaunchedEffect(Unit) { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    LaunchedEffect(Unit) {
        viewModel.actionResult.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateBackEvent.collect {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Details") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            when (val state = uiState) {
                is TransportDetailState.Loading -> LoadingIndicator()
                is TransportDetailState.Error -> ErrorContent(state.message) { viewModel.loadAllDetails() }
                is TransportDetailState.Success -> TransportDetailContent(
                    details = state.details,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Thử lại")
        }
    }
}

@Composable
private fun TransportDetailContent(
    details: FullTransportDetails,
    viewModel: TransportDetailViewModel
) {
    var selectedMode by remember { mutableStateOf(TransportMode.RIDE) }
    var selectedRequest by remember { mutableStateOf<DeliverableRequest?>(null) }
    val mapView = rememberMapViewWithLifecycle()
    val deliverableAddresses by viewModel.deliverableAddresses.collectAsStateWithLifecycle()
    val serviceAddresses by viewModel.serviceAddresses.collectAsStateWithLifecycle()
    val pickupAddress by viewModel.userLocationAddress.collectAsStateWithLifecycle()
    val dropOffAddress by viewModel.dropOffAddress.collectAsStateWithLifecycle()
    var showEditPickupDialog by remember { mutableStateOf(false) }
    var showEditDropOffDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MapSection(
                mapView = mapView,
                details = details,
                mode = selectedMode,
                selectedRequest = selectedRequest,
                onMapClick = { point ->
                    if (details.userLocation != null && details.dropOffPoint == null) {
                        viewModel.updateDropOffLocationManually(point, null)
                    } else {
                        viewModel.updateUserLocationManually(point, null)
                    }
                }
            )
            ControlsOverlay(
                mode = selectedMode,
                userLocation = details.userLocation,
                selectedRequest = selectedRequest,
                onUpdateUserLocation = viewModel::updateUserLocation,
                onResetRoute = viewModel::resetToOriginalRoute
            )
        }

        TabRow(selectedTabIndex = selectedMode.ordinal) {
            Tab(selected = selectedMode == TransportMode.RIDE, onClick = { selectedMode = TransportMode.RIDE; selectedRequest = null; viewModel.resetToOriginalRoute() }, text = { Text("Chở người") })
            Tab(selected = selectedMode == TransportMode.PACKAGE, onClick = { selectedMode = TransportMode.PACKAGE; viewModel.resetToOriginalRoute() }, text = { Text("Gửi hàng") })
        }

        Crossfade(targetState = selectedMode, label = "mode-details") { mode ->
            when (mode) {
                TransportMode.RIDE -> RideDetails(
                    details = details,
                    serviceAddresses = serviceAddresses,
                    pickupAddress = pickupAddress,
                    dropOffAddress = dropOffAddress,
                    onEditPickupClick = { showEditPickupDialog = true },
                    onEditDropOffClick = { showEditDropOffDialog = true },
                    viewModel = viewModel
                )
                TransportMode.PACKAGE -> PackageDetails(
                    details = details,
                    selectedRequest = selectedRequest,
                    deliverableAddresses = deliverableAddresses,
                    onItemSelected = { request ->
                        selectedRequest = request
                        viewModel.calculateDeliveryRoute(request)
                    },
                    viewModel = viewModel
                )
            }
        }
    }

    if (showEditPickupDialog) {
        EditLocationPopup(
            currentLocation = pickupAddress ?: "",
            proximity = details.userLocation,
            onDismiss = { showEditPickupDialog = false },
            onSave = { point, address ->
                viewModel.updateUserLocationManually(point, address)
                showEditPickupDialog = false
            },
            onGetCurrentLocation = {
                viewModel.updateUserLocation()
                showEditPickupDialog = false
            }
        )
    }

    if (showEditDropOffDialog) {
        EditLocationPopup(
            currentLocation = dropOffAddress ?: "",
            proximity = details.dropOffPoint ?: details.userLocation,
            onDismiss = { showEditDropOffDialog = false },
            onSave = { point, address ->
                viewModel.updateDropOffLocationManually(point, address)
                showEditDropOffDialog = false
            },
            onGetCurrentLocation = {
                showEditDropOffDialog = false
            }
        )
    }
}

@Composable
private fun ControlsOverlay(
    mode: TransportMode,
    userLocation: Point?,
    selectedRequest: DeliverableRequest?,
    onUpdateUserLocation: () -> Unit,
    onResetRoute: () -> Unit
) {
    if (mode == TransportMode.RIDE && userLocation == null) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text("💡Tap the map to choose your pickup point", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mode == TransportMode.RIDE) {
            FloatingActionButton(onClick = onUpdateUserLocation, modifier = Modifier.size(48.dp), containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.MyLocation, "Refresh GPS location", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        if (mode == TransportMode.PACKAGE && selectedRequest != null) {
            FloatingActionButton(onClick = onResetRoute, modifier = Modifier.size(48.dp), containerColor = MaterialTheme.colorScheme.secondary) {
                Icon(Icons.Default.Refresh, "View original route", tint = MaterialTheme.colorScheme.onSecondary)
            }
        }
    }
}

@Composable
private fun RideDetails(
    details: FullTransportDetails,
    serviceAddresses: Pair<String, String>?,
    pickupAddress: String?,
    dropOffAddress: String?,
    onEditPickupClick: () -> Unit,
    onEditDropOffClick: () -> Unit,
    viewModel: TransportDetailViewModel
) {
    val service = details.service
    val availableSeats = (service.availableSeat ?: 0) - details.passengers.size
    val fromAddress = serviceAddresses?.first ?: "Loading..."
    val toAddress = serviceAddresses?.second ?: "Loading..."

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Trip details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                InfoRow("Driver’s route:", "${fromAddress} → ${toAddress}")
                InfoRow("Available seats:", "$availableSeats")
                InfoRow("Ticket price / person:", "${service.deliveryFee} VND")
                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                EditableLocationRow(
                    label = "Your pickup location",
                    address = pickupAddress,
                    onEditClick = onEditPickupClick
                )
                EditableLocationRow(
                    label = "Your travel destination",
                    address = dropOffAddress,
                    onEditClick = onEditDropOffClick
                )

                details.isRouteValid?.let { isValid ->
                    val color = if (isValid) Color(0xFF388E3C) else MaterialTheme.colorScheme.error
                    val text = if (isValid) "Your route is valid." else "Your route is invalid (too far from the main route)."
                    Text(text, color = color, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { viewModel.bookRide() },
            modifier = Modifier.fillMaxWidth(),
            enabled = details.isRouteValid == true && availableSeats > 0
        ) {
            Text(
                when {
                    details.userLocation == null -> "Select pickup location"
                    details.dropOffPoint == null -> "Select destination"
                    details.isRouteValid != true -> "Invalid route"
                    else -> "Book now"
                }
            )
        }
    }
}

@Composable
private fun EditableLocationRow(
    label: String,
    address: String?,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEditClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.weight(1f)) {
            Text(
                text = address ?: "Not selected",
                style = MaterialTheme.typography.bodyMedium,
                color = if (address != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 4.dp)
            )
            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MapSection(
    mapView: MapView,
    details: FullTransportDetails,
    mode: TransportMode,
    selectedRequest: DeliverableRequest?,
    onMapClick: ((Point) -> Unit)?
) {
    val map = remember(mapView) { mapView.getMapboxMap() }
    var styleLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(mapView) {
        styleLoaded = false
        map.loadStyleUri(Style.MAPBOX_STREETS) { styleLoaded = true }
    }

    val pointManager = remember(mapView) { mapView.annotations.createPointAnnotationManager() }
    val lineManager = remember(mapView) { mapView.annotations.createPolylineAnnotationManager() }

    DisposableEffect(onMapClick, mapView) {
        val listener = OnMapClickListener { point -> onMapClick?.invoke(point); true }
        if (onMapClick != null) mapView.gestures.addOnMapClickListener(listener)
        onDispose { mapView.gestures.removeOnMapClickListener(listener) }
    }

    LaunchedEffect(styleLoaded, details, mode, selectedRequest) {
        if (!styleLoaded) return@LaunchedEffect

        pointManager.deleteAll()
        lineManager.deleteAll()

        if (details.driverRoutePoints.isNotEmpty()) {
            lineManager.create(
                PolylineAnnotationOptions()
                    .withPoints(details.driverRoutePoints)
                    .withLineWidth(6.0)
                    .withLineColor("#3887be")
            )
        }

        if (mode == TransportMode.RIDE && details.passengerRoutePoints.isNotEmpty()) {
            lineManager.create(
                PolylineAnnotationOptions()
                    .withPoints(details.passengerRoutePoints)
                    .withLineWidth(4.0)
                    .withLineColor("#FFA000")
                    .withLinePattern("dash")
            )
        }

        val pointsForCamera = mutableListOf<Point>()
        details.service.fromLatitude?.let { lat -> details.service.fromLongitude?.let { lng ->
            val p = Point.fromLngLat(lng.toDouble(), lat.toDouble())
            pointsForCamera.add(p)
            pointManager.create(MapboxMarkerUtils.createStartMarker(p))
        }}
        details.service.toLatitude?.let { lat -> details.service.toLongitude?.let { lng ->
            val p = Point.fromLngLat(lng.toDouble(), lat.toDouble())
            pointsForCamera.add(p)
            pointManager.create(MapboxMarkerUtils.createDestinationMarker(p))
        }}

        when (mode) {
            TransportMode.RIDE -> {
                details.userLocation?.let { p ->
                    pointsForCamera.add(p)
                    pointManager.create(MapboxMarkerUtils.createUserLocationMarker(p).withTextField("Điểm đón"))
                }
                details.dropOffPoint?.let { p ->
                    pointsForCamera.add(p)
                    pointManager.create(MapboxMarkerUtils.createDeliveryMarker(p, "Điểm đến"))
                }
            }
            TransportMode.PACKAGE -> {
                selectedRequest?.let { req ->
                    req.request.latFrom?.let { lat -> req.request.lngFrom?.let { lng ->
                        val p = Point.fromLngLat(lng.toDouble(), lat.toDouble())
                        pointsForCamera.add(p)
                        pointManager.create(MapboxMarkerUtils.createPickupMarker(p, req.item.title ?: "..."))
                    }}
                    req.request.latTo?.let { lat -> req.request.lngTo?.let { lng ->
                        val p = Point.fromLngLat(lng.toDouble(), lat.toDouble())
                        pointsForCamera.add(p)
                        pointManager.create(MapboxMarkerUtils.createDeliveryMarker(p, req.item.title ?: "..."))
                    }}
                }
            }
        }

        if (pointsForCamera.size > 1) {
            map.flyTo(map.cameraForCoordinates(pointsForCamera, EdgeInsets(120.0, 80.0, 120.0, 80.0)), null)
        } else if (pointsForCamera.isNotEmpty()) {
            map.flyTo(CameraOptions.Builder().center(pointsForCamera.first()).zoom(14.0).build(), null)
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxWidth().height(300.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageDetails(
    details: FullTransportDetails,
    selectedRequest: DeliverableRequest?,
    deliverableAddresses: Map<Long, Pair<String, String>>,
    onItemSelected: (DeliverableRequest) -> Unit,
    viewModel: TransportDetailViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select goods for delivery",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedRequest?.item?.title ?: "Choose the item you want to send",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Item") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (details.deliverableRequests.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No delivery requests yet") },
                                onClick = { expanded = false }
                            )
                        } else {
                            details.deliverableRequests.forEach { req ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(req.item.title ?: "N/A")
                                            Text(
                                                text = "From (${req.request.latFrom?.let { "%.4f".format(it) }}, ${req.request.lngFrom?.let { "%.4f".format(it) }}) " +
                                                        "to (${req.request.latTo?.let { "%.4f".format(it) }}, ${req.request.lngTo?.let { "%.4f".format(it) }})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = { onItemSelected(req); expanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedRequest?.let { request ->
            val addresses = request.request.id?.let { deliverableAddresses[it] }
            val fromAddress = addresses?.first ?: "Loading address..."
            val toAddress = addresses?.second ?: "Loading address..."
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Delivery details",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow("Item", request.item.title ?: "N/A")
                    InfoRow("Delivery fee", "${details.service.deliveryFee} VND")
                    InfoRow("Pickup location", fromAddress)
                    InfoRow("Drop-off location", toAddress)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { selectedRequest?.let { viewModel.acceptPackageDelivery(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedRequest != null
        ) {
            Text("Accept this delivery")
        }
    }

}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return mapView
}