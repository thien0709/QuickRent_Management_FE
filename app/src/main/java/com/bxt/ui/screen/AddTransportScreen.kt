//// file: com/bxt/ui/screen/AddTransportScreen.kt
//package com.bxt.ui.screen
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.DatePickerDialog
//import android.app.TimePickerDialog
//import android.widget.DatePicker
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.bxt.ui.components.AddressAutocompleteTextField
//import com.bxt.viewmodel.AddTransportViewModel
//import com.bxt.viewmodel.SelectTarget
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.maps.model.CameraPosition
//import com.google.android.gms.maps.model.LatLng
//import com.google.maps.android.compose.*
//
//import java.time.Instant
//import java.time.LocalDateTime
//import java.time.ZoneId
//import java.time.format.DateTimeFormatter
//import java.util.Calendar
//
//@SuppressLint("MissingPermission")
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AddTransportScreen(
//    onSubmit: () -> Unit,
//    onBack: () -> Unit,
//    viewModel: AddTransportViewModel = hiltViewModel()
//) {
//    val uiState by viewModel.uiState.collectAsState()
//    val context = LocalContext.current
//    val snackbarHostState = remember { SnackbarHostState() }
//
//    // điều hướng khi tạo thành công
//    LaunchedEffect(uiState.creationSuccess) {
//        if (uiState.creationSuccess) onSubmit()
//    }
//    // báo lỗi
//    LaunchedEffect(uiState.error) {
//        uiState.error?.let { snackbarHostState.showSnackbar(it) }
//    }
//
//    // xin quyền & lấy vị trí hiện tại
//    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
//    val permissionLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { granted ->
//        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
//                granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
//        if (ok) {
//            fusedClient.lastLocation.addOnSuccessListener { loc ->
//                loc?.let { viewModel.setCurrentLocation(LatLng(it.latitude, it.longitude)) }
//            }
//        }
//    }
//
//    Scaffold(
//        snackbarHost = { SnackbarHost(snackbarHostState) },
//        topBar = {
//            TopAppBar(
//                title = { Text("Tạo Chuyến Đi Mới") },
//                navigationIcon = {
//                    // IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
//                }
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .padding(paddingValues)
//                .padding(16.dp)
//                .verticalScroll(rememberScrollState()),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//
//            // --- Khu vực Map + các nút chọn ---
//            MapSection(
//                uiState = uiState,
//                onMapClick = viewModel::onMapClicked,
//                onSelectFrom = { viewModel.setSelecting(SelectTarget.FROM) },
//                onSelectTo = { viewModel.setSelecting(SelectTarget.TO) },
//                onUseMyLocation = {
//                    permissionLauncher.launch(
//                        arrayOf(
//                            Manifest.permission.ACCESS_FINE_LOCATION,
//                            Manifest.permission.ACCESS_COARSE_LOCATION
//                        )
//                    )
//                }
//            )
//
//            // --- Autocomplete FROM ---
//            AddressAutocompleteTextField (
//                label = "Điểm đi",
//                text = uiState.fromAddress,
//                isLoading = uiState.isSearchingFrom,
//                suggestions = uiState.fromPredictions,
//                onTextChange = { q -> viewModel.searchPlaces(q, SelectTarget.FROM) },
//                onSelect = { item -> viewModel.choosePrediction(item, SelectTarget.FROM) }
//            )
//
//            // --- Autocomplete TO ---
//            AddressAutocompleteTextField(
//                label = "Điểm đến",
//                text = uiState.toAddress,
//                isLoading = uiState.isSearchingTo,
//                suggestions = uiState.toPredictions,
//                onTextChange = { q -> viewModel.searchPlaces(q, SelectTarget.TO) },
//                onSelect = { item -> viewModel.choosePrediction(item, SelectTarget.TO) }
//            )
//
//            OutlinedTextField(
//                value = uiState.deliveryFee,
//                onValueChange = viewModel::onFeeChanged,
//                label = { Text("Phí chia sẻ (VND)") },
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            OutlinedTextField(
//                value = uiState.availableSeat,
//                onValueChange = viewModel::onSeatsChanged,
//                label = { Text("Số ghế trống") },
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            TimeSelectionSection(
//                selectedTime = uiState.departTime,
//                onTimeSelected = viewModel::onTimeChanged
//            )
//
//            OutlinedTextField(
//                value = uiState.description,
//                onValueChange = viewModel::onDescriptionChanged,
//                label = { Text("Mô tả (tùy chọn)") },
//                modifier = Modifier.fillMaxWidth(),
//                minLines = 3
//            )
//
//            Button(
//                onClick = viewModel::createTransport,
//                enabled = !uiState.isLoading,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(50.dp)
//            ) {
//                if (uiState.isLoading) {
//                    CircularProgressIndicator(
//                        modifier = Modifier.size(22.dp),
//                        color = MaterialTheme.colorScheme.onPrimary,
//                        strokeWidth = 2.dp
//                    )
//                } else {
//                    Text("Tạo Chuyến Đi")
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun MapSection(
//    uiState: com.bxt.viewmodel.AddTransportUiState,
//    onMapClick: (LatLng) -> Unit,
//    onSelectFrom: () -> Unit,
//    onSelectTo: () -> Unit,
//    onUseMyLocation: () -> Unit
//) {
//    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//            OutlinedButton(onClick = onSelectFrom, modifier = Modifier.weight(1f)) {
//                Text("Chọn điểm đi trên map")
//            }
//            OutlinedButton(onClick = onSelectTo, modifier = Modifier.weight(1f)) {
//                Text("Chọn điểm đến trên map")
//            }
//        }
//        TextButton(onClick = onUseMyLocation) {
//            Text("Dùng vị trí hiện tại làm điểm đi")
//        }
//
//        val defaultCenter = uiState.fromLatLng ?: uiState.currentLatLng ?: LatLng(10.776, 106.700) // HCM
//        val cameraPositionState = rememberCameraPositionState {
//            position = CameraPosition.fromLatLngZoom(defaultCenter, 12f)
//        }
//
//        GoogleMap(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(260.dp),
//            cameraPositionState = cameraPositionState,
//            onMapClick = onMapClick,
//            uiSettings = MapUiSettings(zoomControlsEnabled = true),
//            properties = MapProperties(isMyLocationEnabled = uiState.currentLatLng != null)
//        ) {
//            uiState.fromLatLng?.let {
//                Marker(
//                    state = MarkerState(it),
//                    title = "Điểm đi"
//                )
//            }
//            uiState.toLatLng?.let {
//                Marker(
//                    state = MarkerState(it),
//                    title = "Điểm đến"
//                )
//            }
//            if (uiState.routePoints.isNotEmpty()) {
//                Polyline(
//                    points = uiState.routePoints,
//                    width = 8f
//                )
//            }
//        }
//        if (uiState.isRouting) {
//            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
//        }
//    }
//}
//
//@Composable
//fun TimeSelectionSection(
//    selectedTime: Instant,
//    onTimeSelected: (Instant) -> Unit
//) {
//    val context = LocalContext.current
//    val calendar = Calendar.getInstance()
//    val localDateTime = LocalDateTime.ofInstant(selectedTime, ZoneId.systemDefault())
//
//    val datePickerDialog = DatePickerDialog(
//        context,
//        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
//            calendar.set(year, month, dayOfMonth)
//            val timePickerDialog = TimePickerDialog(
//                context,
//                { _, hourOfDay: Int, minute: Int ->
//                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
//                    calendar.set(Calendar.MINUTE, minute)
//                    onTimeSelected(calendar.toInstant())
//                },
//                localDateTime.hour,
//                localDateTime.minute,
//                true
//            )
//            timePickerDialog.show()
//        },
//        localDateTime.year,
//        localDateTime.monthValue - 1,
//        localDateTime.dayOfMonth
//    )
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(
//            text = "Thời gian: ${
//                localDateTime.format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy"))
//            }"
//        )
//        Row {
//            TextButton(onClick = { onTimeSelected(Instant.now()) }) { Text("Bây giờ") }
//            TextButton(onClick = { datePickerDialog.show() }) { Text("Chọn giờ") }
//        }
//    }
//}
//
//// small ext
//private fun Calendar.toInstant(): Instant =
//    this.time.toInstant()
