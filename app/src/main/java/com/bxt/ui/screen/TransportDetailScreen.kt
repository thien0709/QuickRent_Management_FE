// bxt/ui/screen/TransportDetailScreen.kt
package com.bxt.ui.screen

import android.Manifest
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
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.loadAllDetails()
    }
    LaunchedEffect(Unit) { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết chuyến đi") },
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
    val userLocationAddress by viewModel.userLocationAddress.collectAsStateWithLifecycle()
    var showEditLocationDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MapSection(
                mapView = mapView,
                details = details,
                mode = selectedMode,
                selectedRequest = selectedRequest,
                onUserLocationChanged = { point -> viewModel.updateUserLocationManually(point, null) }
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
                    userLocationAddress = userLocationAddress,
                    onEditLocationClick = { showEditLocationDialog = true }
                )
                TransportMode.PACKAGE -> PackageDetails(
                    details = details,
                    selectedRequest = selectedRequest,
                    deliverableAddresses = deliverableAddresses,
                    onItemSelected = { request ->
                        selectedRequest = request
                        viewModel.calculateDeliveryRoute(request)
                    }
                )
            }
        }
    }

    if (showEditLocationDialog) {
        EditLocationPopup(
            currentLocation = userLocationAddress ?: "",
            proximity = details.userLocation,
            onDismiss = { showEditLocationDialog = false },
            onSave = { point, address ->
                viewModel.updateUserLocationManually(point, address)
                showEditLocationDialog = false
            },
            onGetCurrentLocation = {
                viewModel.updateUserLocation()
                showEditLocationDialog = false
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
            Text("💡 Nhấn vào bản đồ để chọn vị trí của bạn", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mode == TransportMode.RIDE) {
            FloatingActionButton(onClick = onUpdateUserLocation, modifier = Modifier.size(48.dp), containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.MyLocation, "Cập nhật vị trí GPS", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        if (mode == TransportMode.PACKAGE && selectedRequest != null) {
            FloatingActionButton(onClick = onResetRoute, modifier = Modifier.size(48.dp), containerColor = MaterialTheme.colorScheme.secondary) {
                Icon(Icons.Default.Refresh, "Xem route gốc", tint = MaterialTheme.colorScheme.onSecondary)
            }
        }
    }
}

@Composable
private fun RideDetails(
    details: FullTransportDetails,
    serviceAddresses: Pair<String, String>?,
    userLocationAddress: String?,
    onEditLocationClick: () -> Unit
) {
    val service = details.service
    val availableSeats = (service.availableSeat ?: 0) - details.passengers.size
    val fromAddress = serviceAddresses?.first ?: "Đang tải..."
    val toAddress = serviceAddresses?.second ?: "Đang tải..."

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Thông tin chuyến đi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                InfoRow("Đi từ", fromAddress)
                InfoRow("Đi đến", toAddress)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                InfoRow("Số chỗ còn trống", "$availableSeats")
                InfoRow("Giá vé / người", "${service.deliveryFee} VND")
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onEditLocationClick),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vị trí của bạn", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (userLocationAddress != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(userLocationAddress, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f, fill = false))
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                        }
                    } else {
                        Text("Chưa có vị trí", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = { /* TODO: Implement booking logic */ }, modifier = Modifier.fillMaxWidth(), enabled = availableSeats > 0 && details.userLocation != null) {
            Text(if (details.userLocation != null) "Đặt ngay" else "Chọn vị trí để tiếp tục")
        }
    }
}

@Composable
private fun MapSection(
    mapView: MapView,
    details: FullTransportDetails,
    mode: TransportMode,
    selectedRequest: DeliverableRequest?,
    onUserLocationChanged: ((Point) -> Unit)?
) {
    val map = remember(mapView) { mapView.getMapboxMap() }
    var styleLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(mapView) {
        styleLoaded = false
        map.loadStyleUri(Style.MAPBOX_STREETS) { styleLoaded = true }
    }

    val pointManager = remember(mapView) { mapView.annotations.createPointAnnotationManager() }
    val lineManager  = remember(mapView) { mapView.annotations.createPolylineAnnotationManager() }

    DisposableEffect(onUserLocationChanged, mapView) {
        val listener = OnMapClickListener { point ->
            onUserLocationChanged?.invoke(point)
            true
        }
        if (onUserLocationChanged != null) mapView.gestures.addOnMapClickListener(listener)
        onDispose { mapView.gestures.removeOnMapClickListener(listener) }
    }

    LaunchedEffect(styleLoaded, details, mode, selectedRequest) {
        if (!styleLoaded) return@LaunchedEffect

        pointManager.deleteAll()
        lineManager.deleteAll()

        if (details.routePoints.isNotEmpty()) {
            lineManager.create(
                PolylineAnnotationOptions()
                    .withPoints(details.routePoints)
                    .withLineWidth(5.0)
                    .withLineColor("#3887be")
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
            TransportMode.RIDE -> details.userLocation?.let { p ->
                pointsForCamera.add(p)
                pointManager.create(MapboxMarkerUtils.createUserLocationMarker(p))
            }
            TransportMode.PACKAGE -> selectedRequest?.let { req ->
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

        when {
            pointsForCamera.size > 1 -> {
                map.flyTo(map.cameraForCoordinates(pointsForCamera, EdgeInsets(120.0, 80.0, 120.0, 80.0)), null)
            }
            pointsForCamera.size == 1 -> {
                map.flyTo(CameraOptions.Builder().center(pointsForCamera.first()).zoom(14.0).build(), null)
            }
            else -> {
                map.flyTo(CameraOptions.Builder().center(Point.fromLngLat(106.701755, 10.776789)).zoom(10.0).build(), null)
            }
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
    onItemSelected: (DeliverableRequest) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Chọn hàng hóa vận chuyển", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedRequest?.item?.title ?: "Chọn món đồ bạn muốn gửi",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Món đồ") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (details.deliverableRequests.isEmpty()) {
                            DropdownMenuItem(text = { Text("Chưa có yêu cầu vận chuyển nào") }, onClick = { expanded = false })
                        } else {
                            details.deliverableRequests.forEach { req ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(req.item.title ?: "N/A")
                                            Text(
                                                text = "Từ (${req.request.latFrom?.let { "%.4f".format(it) }}, ${req.request.lngFrom?.let { "%.4f".format(it) }}) " +
                                                        "đến (${req.request.latTo?.let { "%.4f".format(it) }}, ${req.request.lngTo?.let { "%.4f".format(it) }})",
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
            val fromAddress = addresses?.first ?: "Đang tải địa chỉ..."
            val toAddress = addresses?.second ?: "Đang tải địa chỉ..."
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Chi tiết vận chuyển", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    InfoRow("Món hàng", request.item.title ?: "N/A")
                    InfoRow("Phí vận chuyển", "${details.service.deliveryFee} VND")
                    InfoRow("Điểm lấy hàng", fromAddress)
                    InfoRow("Điểm giao hàng", toAddress)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { /* TODO: Implement package delivery logic */ }, modifier = Modifier.fillMaxWidth(), enabled = selectedRequest != null) {
            Text("Nhận vận chuyển hàng này")
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