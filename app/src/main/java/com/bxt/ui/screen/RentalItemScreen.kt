package com.bxt.ui.screen

import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bxt.ui.components.PickDateTime
import com.bxt.ui.state.RentalState
import com.bxt.ui.theme.LocalDimens
import com.bxt.viewmodel.RentalItemViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalItemScreen(
    onClickBack: () -> Unit,
    onRentalSuccess: () -> Unit,
    viewModel: RentalItemViewModel = hiltViewModel()
) {
    val d = LocalDimens.current
    val context = LocalContext.current
    val rentalState by viewModel.rentalState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val geocoder = remember(context) { Geocoder(context) }

    // Form state
    var startAt by remember { mutableStateOf<OffsetDateTime?>(null) }
    var endAt by remember { mutableStateOf<OffsetDateTime?>(null) }
    var address by remember { mutableStateOf("") }

    // Map state (HCM default)
    val hcmCity = LatLng(10.762622, 106.660172)
    val markerState = rememberMarkerState(position = hcmCity)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(hcmCity, 12f)
    }

    // Reverse geocoding
    fun getAddressFromLatLng(latLng: LatLng) {
        scope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                        address = addresses.firstOrNull()?.getAddressLine(0) ?: "Không tìm thấy địa chỉ"
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addrs = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    address = addrs?.firstOrNull()?.getAddressLine(0) ?: "Không tìm thấy địa chỉ"
                }
            } catch (_: IOException) {
                address = "Lỗi khi lấy địa chỉ"
            }
        }
    }

    // Lấy địa chỉ khi kết thúc kéo marker
    LaunchedEffect(markerState.dragState) {
        if (markerState.dragState == DragState.END) {
            getAddressFromLatLng(markerState.position)
        }
    }

    // Cập nhật camera khi marker thay đổi
    LaunchedEffect(markerState.position) {
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(markerState.position, 15f))
    }

    // Formatters
    val dtFmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }
    val moneyFmt = remember {
        NumberFormat.getCurrencyInstance(Locale("vi", "VN")).apply { maximumFractionDigits = 0 }
    }
    fun money(v: BigDecimal?): String = if (v == null) "—" else moneyFmt.format(v)

    // Tính tiền
    fun chargeableHours(s: OffsetDateTime, e: OffsetDateTime): Long {
        val mins = Duration.between(s, e).toMinutes().coerceAtLeast(0)
        return max(1L, ceil(mins / 60.0).toLong())
    }
    val canCalculate = startAt != null && endAt != null && endAt!!.isAfter(startAt)
    val hours = if (canCalculate) chargeableHours(startAt!!, endAt!!) else 0L
    val total = if (canCalculate) viewModel.pricePerHour.multiply(BigDecimal.valueOf(hours)) else BigDecimal.ZERO

    // Handle state
    LaunchedEffect(rentalState) {
        when (val state = rentalState) {
            is RentalState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetRentalState()
            }
            is RentalState.Success -> {
                snackbarHostState.showSnackbar("Gửi yêu cầu thuê thành công!")
                onRentalSuccess()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tạo yêu cầu thuê", style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = d.pagePadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(d.sectionGap)
        ) {
            // Thời gian
            Text("Chọn thời gian thuê", style = MaterialTheme.typography.titleSmall)

            OutlinedButton(
                onClick = {
                    PickDateTime(context, startAt) { picked ->
                        startAt = picked
                        if (endAt == null || !endAt!!.isAfter(picked)) {
                            endAt = picked.plusHours(1)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "Bắt đầu: " + (startAt?.format(dtFmt) ?: "Chọn thời gian"),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedButton(
                onClick = { PickDateTime(context, endAt) { picked -> endAt = picked } },
                modifier = Modifier.fillMaxWidth(),
                enabled = startAt != null,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "Kết thúc: " + (endAt?.format(dtFmt) ?: "Chọn thời gian"),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Divider()

            // Địa chỉ
            Text("Chọn địa chỉ giao hàng", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Địa chỉ nhận hàng (ví dụ: 123 Nguyễn Văn Cừ, P4, Q5)", style = MaterialTheme.typography.labelSmall) },
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = d.fieldMinHeight),
                shape = MaterialTheme.shapes.medium
            )

            Text("Hoặc chọn trên bản đồ:", style = MaterialTheme.typography.bodySmall)

            val mapHeight = d.imageSize * 3.2f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        markerState.position = latLng
                        getAddressFromLatLng(latLng)
                    }
                ) {
                    Marker(
                        state = markerState,
                        title = "Vị trí giao hàng",
                        snippet = "Kéo để chọn vị trí",
                        draggable = true
                    )
                }
            }

            Divider()

            // Tóm tắt
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Giá mỗi giờ", style = MaterialTheme.typography.bodySmall)
                Text(money(viewModel.pricePerHour), style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Số giờ tính phí", style = MaterialTheme.typography.bodySmall)
                Text(if (canCalculate) "$hours giờ" else "—", style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tổng tiền", style = MaterialTheme.typography.titleSmall)
                Text(money(total), style = MaterialTheme.typography.titleSmall)
            }

            val canSubmit = canCalculate && address.isNotBlank() && rentalState !is RentalState.Submitting

            Button(
                onClick = {
                    viewModel.createRentalRequest(
                        startAt = startAt!!.toInstant(),
                        endAt = endAt!!.toInstant(),
                        totalPrice = total,
                        address = address,
                        latTo = BigDecimal(markerState.position.latitude),
                        lngTo = BigDecimal(markerState.position.longitude),
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(d.buttonHeight),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Xác nhận thuê", style = MaterialTheme.typography.bodySmall)
            }

            if (rentalState is RentalState.Submitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
