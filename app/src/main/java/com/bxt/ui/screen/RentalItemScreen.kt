package com.bxt.ui.screen

import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bxt.ui.components.PickDateTime
import com.bxt.ui.state.RentalState
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
    val context = LocalContext.current
    val rentalState by viewModel.rentalState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val geocoder = remember(context) { Geocoder(context) }

    // Screen State
    var startAt by remember { mutableStateOf<OffsetDateTime?>(null) }
    var endAt by remember { mutableStateOf<OffsetDateTime?>(null) }
    var address by remember { mutableStateOf("") }

    // Map State
    val hcmCity = LatLng(10.762622, 106.660172)
    // Sửa lỗi: Dùng rememberMarkerState để quản lý trạng thái của Marker
    val markerState = rememberMarkerState(position = hcmCity)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(hcmCity, 12f)
    }

    // Hàm tìm địa chỉ từ LatLng
    fun getAddressFromLatLng(latLng: LatLng) {
        scope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                        address = addresses.firstOrNull()?.getAddressLine(0) ?: "Không tìm thấy địa chỉ"
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Không tìm thấy địa chỉ"
                }
            } catch (e: IOException) {
                address = "Lỗi khi lấy địa chỉ"
            }
        }
    }

    // Sửa lỗi: Dùng LaunchedEffect để theo dõi trạng thái kéo-thả
    LaunchedEffect(markerState.dragState) {
        if (markerState.dragState == DragState.END) {
            getAddressFromLatLng(markerState.position)
        }
    }

    // Cập nhật vị trí camera khi marker di chuyển
    LaunchedEffect(markerState.position) {
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(markerState.position, 15f))
    }

    // Formatters
    val dtFmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }
    val moneyFmt = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")).apply { maximumFractionDigits = 0 } }
    fun money(v: BigDecimal?): String = if (v == null) "—" else moneyFmt.format(v)

    // Calculation logic
    fun chargeableHours(s: OffsetDateTime, e: OffsetDateTime): Long {
        val mins = Duration.between(s, e).toMinutes().coerceAtLeast(0)
        return max(1L, ceil(mins / 60.0).toLong())
    }

    val canCalculate = startAt != null && endAt != null && endAt!!.isAfter(startAt)
    val hours = if (canCalculate) chargeableHours(startAt!!, endAt!!) else 0L
    val total = if (canCalculate) viewModel.pricePerHour.multiply(BigDecimal.valueOf(hours)) else BigDecimal.ZERO

    // Handle rental state changes
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
                title = { Text("Tạo yêu cầu thuê") },
                navigationIcon = { IconButton(onClick = onClickBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Chọn thời gian thuê", style = MaterialTheme.typography.titleLarge)

            OutlinedButton(
                onClick = {
                    PickDateTime(context, startAt) { picked ->
                        startAt = picked
                        if (endAt == null || !endAt!!.isAfter(picked)) {
                            endAt = picked.plusHours(1)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Bắt đầu: " + (startAt?.format(dtFmt) ?: "Chọn thời gian")) }

            OutlinedButton(
                onClick = { PickDateTime(context, endAt) { picked -> endAt = picked } },
                modifier = Modifier.fillMaxWidth(),
                enabled = startAt != null
            ) { Text("Kết thúc: " + (endAt?.format(dtFmt) ?: "Chọn thời gian")) }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Chọn địa chỉ giao hàng", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Địa chỉ nhận hàng (ví dụ: 123 Nguyễn Văn Cừ, P4, Q5)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Hoặc chọn trên bản đồ:", style = MaterialTheme.typography.bodySmall)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        markerState.position = latLng
                        getAddressFromLatLng(latLng)
                    }
                ) {
                    // Sửa lỗi: Xóa tham số onDragEnd và chỉ cần truyền state
                    Marker(
                        state = markerState,
                        title = "Vị trí giao hàng",
                        snippet = "Kéo để chọn vị trí",
                        draggable = true
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Summary
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Giá mỗi giờ")
                Text(money(viewModel.pricePerHour), fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Số giờ tính phí")
                Text(if (canCalculate) "$hours giờ" else "—", fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tổng tiền", style = MaterialTheme.typography.titleMedium)
                Text(money(total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) { Text("Xác nhận thuê") }

            if (rentalState is RentalState.Submitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}