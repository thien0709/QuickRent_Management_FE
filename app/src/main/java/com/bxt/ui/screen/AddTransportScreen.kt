package com.bxt.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.ui.components.MapboxSearchBar
import com.bxt.viewmodel.AddTransportViewModel
import com.bxt.viewmodel.SelectTarget
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransportScreen(
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddTransportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.creationSuccess) { if (uiState.creationSuccess) onSubmit() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }

    // xin quyền & lấy vị trí hiện tại
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            fused.lastLocation.addOnSuccessListener { loc ->
                loc?.let { viewModel.setCurrentLocation(Point.fromLngLat(it.longitude, it.latitude)) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Tạo Chuyến Đi Mới") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            MapSection(
                uiState = uiState,
                onMapClick = viewModel::onMapClicked,
                onSelectFrom = { viewModel.setSelecting(SelectTarget.FROM) },
                onSelectTo = { viewModel.setSelecting(SelectTarget.TO) },
                onUseMyLocation = {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }
            )

            // FROM - dùng MapboxSearchBar
            MapboxSearchBar(
                value = uiState.fromAddress,
                onValueChange = { /* chỉ hiển thị; kết quả chọn sẽ set qua onPlacePicked */ },
                proximity = uiState.currentPoint ?: uiState.fromPoint,
                onPlacePicked = { p, addr -> viewModel.setFromBySearch(p, addr) }
            )

            // TO - dùng MapboxSearchBar
            MapboxSearchBar(
                value = uiState.toAddress,
                onValueChange = { /* như trên */ },
                proximity = uiState.fromPoint ?: uiState.currentPoint,
                onPlacePicked = { p, addr -> viewModel.setToBySearch(p, addr) }
            )

            OutlinedTextField(
                value = uiState.deliveryFee,
                onValueChange = viewModel::onFeeChanged,
                label = { Text("Phí chia sẻ (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.availableSeat,
                onValueChange = viewModel::onSeatsChanged,
                label = { Text("Số ghế trống") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            TimeSelectionSection(
                selectedTime = uiState.departTime,
                onTimeSelected = viewModel::onTimeChanged
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text("Mô tả (tùy chọn)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = viewModel::createTransport,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else Text("Tạo Chuyến Đi")
            }
        }
    }
}

@Composable
private fun MapSection(
    uiState: com.bxt.viewmodel.AddTransportUiState,
    onMapClick: (Point) -> Unit,
    onSelectFrom: () -> Unit,
    onSelectTo: () -> Unit,
    onUseMyLocation: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onSelectFrom, modifier = Modifier.weight(1f)) { Text("Chọn điểm đi trên map") }
            OutlinedButton(onClick = onSelectTo, modifier = Modifier.weight(1f)) { Text("Chọn điểm đến trên map") }
        }
        TextButton(onClick = onUseMyLocation) { Text("Dùng vị trí hiện tại làm điểm đi") }

        val mapView = rememberMapViewWithLifecycle()
        // Tái sử dụng managers
        val pointManager = remember(mapView) { mapView.annotations.createPointAnnotationManager() }
        val lineManager = remember(mapView) { mapView.annotations.createPolylineAnnotationManager() }

        AndroidView(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            factory = {
                mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
                // Đăng ký click đúng API gestures
                mapView.gestures.addOnMapClickListener { p ->
                    onMapClick(p)
                    true
                }
                mapView
            },
            update = { mv ->
                val map = mv.getMapboxMap()

                // Camera
                val center = uiState.fromPoint ?: uiState.currentPoint ?: Point.fromLngLat(106.700, 10.776)
                map.setCamera(CameraOptions.Builder().center(center).zoom(12.0).build())

                // Markers
                pointManager.deleteAll()
                uiState.fromPoint?.let {
                    pointManager.create(
                        PointAnnotationOptions()
                            .withPoint(it)
                            .withTextField("Điểm đi")
                    )
                }
                uiState.toPoint?.let {
                    pointManager.create(
                        PointAnnotationOptions()
                            .withPoint(it)
                            .withTextField("Điểm đến")
                    )
                }

                // Polyline
                lineManager.deleteAll()
                if (uiState.routePoints.isNotEmpty()) {
                    lineManager.create(
                        PolylineAnnotationOptions()
                            .withPoints(uiState.routePoints)
                            .withLineColor("#3bb2d0")
                            .withLineWidth(5.0)
                    )
                }
            }
        )

        if (uiState.isRouting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e ->
            when (e) {
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

@Composable
fun TimeSelectionSection(
    selectedTime: Instant,
    onTimeSelected: (Instant) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val localDateTime = LocalDateTime.ofInstant(selectedTime, ZoneId.systemDefault())

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            val timePickerDialog = TimePickerDialog(
                context,
                { _, hourOfDay: Int, minute: Int ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    onTimeSelected(calendar.time.toInstant())
                },
                localDateTime.hour,
                localDateTime.minute,
                true
            )
            timePickerDialog.show()
        },
        localDateTime.year, localDateTime.monthValue - 1, localDateTime.dayOfMonth
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Thời gian: ${localDateTime.format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy"))}")
        Row {
            TextButton(onClick = { onTimeSelected(Instant.now()) }) { Text("Bây giờ") }
            TextButton(onClick = { datePickerDialog.show() }) { Text("Chọn giờ") }
        }
    }
}
