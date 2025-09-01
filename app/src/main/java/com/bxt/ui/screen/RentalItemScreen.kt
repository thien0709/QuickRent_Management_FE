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
import com.bxt.ui.components.MapboxSearchBar
import com.bxt.ui.components.PickDateTime
import com.bxt.ui.state.RentalState
import com.bxt.ui.theme.LocalDimens
import com.bxt.util.MapboxMarkerUtils
import com.bxt.viewmodel.RentalItemViewModel
// Mapbox imports
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
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

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
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

    // Mapbox state
    val hcmCity = Point.fromLngLat(106.660172, 10.762622) // Lưu ý: lng, lat
    var selectedPoint by remember { mutableStateOf(hcmCity) }
    val mapState = rememberMapState()
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(12.0)
            center(hcmCity)
        }
    }

    fun getAddressFromPoint(point: Point) {
        scope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(point.latitude(), point.longitude(), 1) { addresses ->
                        address = addresses.firstOrNull()?.getAddressLine(0) ?: "Không tìm thấy địa chỉ"
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addrs = geocoder.getFromLocation(point.latitude(), point.longitude(), 1)
                    address = addrs?.firstOrNull()?.getAddressLine(0) ?: "Không tìm thấy địa chỉ"
                }
            } catch (_: IOException) {
                address = "Lỗi khi lấy địa chỉ"
            }
        }
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

            HorizontalDivider()

            MapboxAddressSelection(
                address = address,
                onAddressChange = { address = it },
                selectedPoint = selectedPoint,
                mapState = mapState,
                mapViewportState = mapViewportState,
                onPointSelected = { point ->
                    selectedPoint = point
                    getAddressFromPoint(point)
                    // Di chuyển camera đến vị trí mới
                    mapViewportState.setCameraOptions {
                        zoom(15.0)
                        center(point)
                    }
                }
            )

            HorizontalDivider()
            PaymentMethodSelector(selectedPaymentMethod) { selectedPaymentMethod = it }
            HorizontalDivider()
            RentalSummary(viewModel.pricePerHour, hours, total, canCalculate)

            Button(
                onClick = {
                    viewModel.createRentalRequest(
                        startAt = startAt!!.toInstant(),
                        endAt = endAt!!.toInstant(),
                        totalPrice = total,
                        address = address,
                        latTo = BigDecimal.valueOf(selectedPoint.latitude()),
                        lngTo = BigDecimal.valueOf(selectedPoint.longitude()),
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

@OptIn(MapboxExperimental::class)
@Composable
private fun MapboxAddressSelection(
    address: String,
    onAddressChange: (String) -> Unit,
    selectedPoint: Point,
    mapState: com.mapbox.maps.extension.compose.MapState,
    mapViewportState: com.mapbox.maps.extension.compose.animation.viewport.MapViewportState,
    onPointSelected: (Point) -> Unit
) {
    val d = LocalDimens.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(d.rowGap)) {
        Text("Chọn địa chỉ giao hàng", style = MaterialTheme.typography.titleSmall)

        // Replace the text field with MapboxSearchBar
        MapboxSearchBar(
            value = address,
            onValueChange = onAddressChange,
            proximity = selectedPoint,
            onPlacePicked = { point, fullAddress ->
                onPointSelected(point)
                onAddressChange(fullAddress)
                // Move camera to the new location
                mapViewportState.setCameraOptions {
                    zoom(15.0)
                    center(point)
                }
            }
        )

        Text("Hoặc chọn trên bản đồ:", style = MaterialTheme.typography.bodySmall)

        Box(modifier = Modifier.fillMaxWidth().height(d.imageSize * 3.2f)) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapState = mapState,
                mapViewportState = mapViewportState
            ) {
                // Set map style
                MapEffect(Unit) { mapView ->
                    mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
                }
                // Map click listener
                MapEffect(Unit) { mapView ->
                    mapView.gestures.addOnMapClickListener(OnMapClickListener { point ->
                        onPointSelected(point)
                        true // consume the event
                    })
                }

                // Annotation (marker) - Sử dụng utility function
                PointAnnotationGroup(
                    annotations = listOf(
                        MapboxMarkerUtils.createSimpleMarker(
                            point = selectedPoint,
                            title = "Vị trí giao hàng",
                            isDraggable = true,
                            context = context
                        )
                    )
                )
            }
        }
    }
}

// Các Composable khác giữ nguyên (TimeSelection, PaymentMethodSelector, RentalSummary)
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