package com.bxt.ui.screen

import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val scope = rememberCoroutineScope()
    val geocoder = remember(context) { Geocoder(context) }

    // Form states
    var startAt by remember { mutableStateOf<OffsetDateTime?>(null) }
    var endAt by remember { mutableStateOf<OffsetDateTime?>(null) }
    var address by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("CASH") }

    // Map state
    val hcmCity = LatLng(10.762622, 106.660172)
    val markerState = rememberMarkerState(position = hcmCity)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(hcmCity, 12f)
    }

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

    LaunchedEffect(markerState.dragState) {
        if (markerState.dragState == DragState.END) getAddressFromLatLng(markerState.position)
    }
    LaunchedEffect(markerState.position) {
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(markerState.position, 15f))
    }

    LaunchedEffect(rentalState) {
        when (val state = rentalState) {
            is RentalState.Success -> {
                val message = "Yêu cầu thuê (ID: ${state.id}) đã được gửi."
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                viewModel.resetRentalState()
                onRentalSuccess()
            }
            else -> Unit
        }
    }

    val canCalculate = startAt != null && endAt != null && endAt!!.isAfter(startAt)
    val hours = if (canCalculate) {
        val mins = Duration.between(startAt!!, endAt!!).toMinutes().coerceAtLeast(0)
        max(1L, ceil(mins / 60.0).toLong())
    } else 0L
    val total = if (canCalculate) viewModel.pricePerHour.multiply(BigDecimal.valueOf(hours)) else BigDecimal.ZERO
    val canSubmit = canCalculate && address.isNotBlank() && rentalState !is RentalState.Submitting

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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = d.pagePadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(d.sectionGap)
        ) {
            TimeSelection(startAt, endAt, onStartAtChange = {
                startAt = it
                if (endAt == null || !endAt!!.isAfter(it)) endAt = it.plusHours(1)
            }, onEndAtChange = { endAt = it })
            Divider()
            AddressSelection(address, { address = it }, cameraPositionState, markerState) {
                markerState.position = it
                getAddressFromLatLng(it)
            }
            Divider()
            PaymentMethodSelector(selectedPaymentMethod) { selectedPaymentMethod = it }
            Divider()
            RentalSummary(viewModel.pricePerHour, hours, total, canCalculate)
            Button(
                onClick = {
                    viewModel.createRentalRequest(
                        startAt = startAt!!.toInstant(),
                        endAt = endAt!!.toInstant(),
                        totalPrice = total,
                        address = address,
                        latTo = BigDecimal.valueOf(markerState.position.latitude),
                        lngTo = BigDecimal.valueOf(markerState.position.longitude),
                        paymentMethod = selectedPaymentMethod
                    )
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(d.buttonHeight),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Xác nhận thuê", style = MaterialTheme.typography.bodySmall)
            }
            if (rentalState is RentalState.Submitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(d.pagePadding))
        }
    }
}

// Các Composable phụ không thay đổi
@Composable
private fun TimeSelection(
    startAt: OffsetDateTime?,
    endAt: OffsetDateTime?,
    onStartAtChange: (OffsetDateTime) -> Unit,
    onEndAtChange: (OffsetDateTime) -> Unit
) {
    val d = LocalDimens.current
    val context = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }

    Column(verticalArrangement = Arrangement.spacedBy(d.rowGap)) {
        Text("Chọn thời gian thuê", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { PickDateTime(context, startAt, onStartAtChange) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Bắt đầu: " + (startAt?.format(dtFmt) ?: "Chọn thời gian"), style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(
            onClick = { PickDateTime(context, endAt, onEndAtChange) },
            modifier = Modifier.fillMaxWidth(),
            enabled = startAt != null,
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Kết thúc: " + (endAt?.format(dtFmt) ?: "Chọn thời gian"), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AddressSelection(
    address: String,
    onAddressChange: (String) -> Unit,
    mapCameraPositionState: CameraPositionState,
    mapMarkerState: MarkerState,
    onMapClick: (LatLng) -> Unit
) {
    val d = LocalDimens.current
    Column(verticalArrangement = Arrangement.spacedBy(d.rowGap)) {
        Text("Chọn địa chỉ giao hàng", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Địa chỉ nhận hàng...", style = MaterialTheme.typography.labelSmall) },
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().heightIn(min = d.fieldMinHeight),
            shape = MaterialTheme.shapes.medium
        )
        Text("Hoặc chọn trên bản đồ:", style = MaterialTheme.typography.bodySmall)
        Box(modifier = Modifier.fillMaxWidth().height(d.imageSize * 3.2f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = mapCameraPositionState,
                onMapClick = onMapClick
            ) {
                Marker(state = mapMarkerState, title = "Vị trí giao hàng", snippet = "Kéo để chọn", draggable = true)
            }
        }
    }
}

@Composable
private fun PaymentMethodSelector(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit
) {
    val d = LocalDimens.current
    val paymentOptions = remember { mapOf("CASH" to "Thanh toán khi nhận hàng (COD)", "VNPAY" to "Thanh toán qua VNPay") }

    Column(verticalArrangement = Arrangement.spacedBy(d.rowGap / 2)) {
        Text("Phương thức thanh toán", style = MaterialTheme.typography.titleSmall)
        paymentOptions.forEach { (key, displayText) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = (selectedMethod == key), onClick = { onMethodSelected(key) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (selectedMethod == key), onClick = { onMethodSelected(key) })
                Spacer(Modifier.width(d.rowGap))
                Text(text = displayText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RentalSummary(
    pricePerHour: BigDecimal,
    chargeableHours: Long,
    totalPrice: BigDecimal,
    canCalculate: Boolean
) {
    val moneyFmt = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")).apply { maximumFractionDigits = 0 } }
    fun money(v: BigDecimal?): String = if (v == null) "—" else moneyFmt.format(v)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Giá mỗi giờ", style = MaterialTheme.typography.bodySmall)
            Text(money(pricePerHour), style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Số giờ tính phí", style = MaterialTheme.typography.bodySmall)
            Text(if (canCalculate) "$chargeableHours giờ" else "—", style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tổng tiền", style = MaterialTheme.typography.titleSmall)
            Text(money(totalPrice), style = MaterialTheme.typography.bodySmall)
        }
    }
}