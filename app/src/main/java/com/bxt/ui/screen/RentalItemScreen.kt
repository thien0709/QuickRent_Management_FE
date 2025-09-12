package com.bxt.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.ui.components.MapboxSearchBar
import com.bxt.ui.components.PickDateTime
import com.bxt.ui.state.ItemDataState
import com.bxt.ui.state.RentalState
import com.bxt.ui.theme.LocalDimens
import com.bxt.util.MapboxMarkerUtils
import com.bxt.viewmodel.RentalItemViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
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
    val itemDataState by viewModel.itemDataState.collectAsStateWithLifecycle()

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = itemDataState) {
                is ItemDataState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ItemDataState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ItemDataState.Success -> {
                    // Khi có dữ liệu, hiển thị nội dung chính
                    RentalItemContent(
                        item = state.item,
                        viewModel = viewModel,
                        onRentalSuccess = onRentalSuccess
                    )
                }
            }
        }
    }
}

@OptIn(MapboxExperimental::class, ExperimentalMaterial3Api::class)
@Composable
private fun RentalItemContent(
    item: ItemResponse,
    viewModel: RentalItemViewModel,
    onRentalSuccess: () -> Unit
) {
    val d = LocalDimens.current
    val context = LocalContext.current
    val rentalState by viewModel.rentalState.collectAsStateWithLifecycle()
    val initialAddress by viewModel.initialAddress.collectAsStateWithLifecycle()
    val resolvedAddress by viewModel.resolvedAddress.collectAsStateWithLifecycle()
    val initialMapLocation by viewModel.initialMapLocation.collectAsStateWithLifecycle()

    var startAt by remember { mutableStateOf<OffsetDateTime?>(null) }
    var endAt by remember { mutableStateOf<OffsetDateTime?>(null) }
    var address by rememberSaveable { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("CASH") }

    var selectedPoint by remember { mutableStateOf(initialMapLocation) }
    val mapState = rememberMapState()
    val mapViewportState = rememberMapViewportState {
        setCameraOptions { zoom(12.0); center(initialMapLocation) }
    }

    LaunchedEffect(initialMapLocation) {
        selectedPoint = initialMapLocation
        mapViewportState.setCameraOptions {
            zoom(12.0)
            center(initialMapLocation)
        }
    }

    LaunchedEffect(initialAddress) {
        if (initialAddress != null && address.isBlank()) {
            address = initialAddress!!
        }
    }

    LaunchedEffect(resolvedAddress) {
        resolvedAddress?.let {
            address = it
            viewModel.clearResolvedAddress()
        }
    }

    LaunchedEffect(rentalState) {
        if (rentalState is RentalState.Success) {
            val rentalId = (rentalState as RentalState.Success).id
            val message = "Yêu cầu thuê (ID: $rentalId) đã được gửi."
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.resetRentalState()
            onRentalSuccess()
        }
    }

    val pricePerHour = item.rentalPricePerHour ?: BigDecimal.ZERO
    val depositAmount = item.depositAmount ?: BigDecimal.ZERO

    val canCalculate = startAt != null && endAt != null && endAt!!.isAfter(startAt)
    val hours = if (canCalculate) max(1L, ceil(Duration.between(startAt!!, endAt!!).toMinutes() / 60.0).toLong()) else 0L
    val rentalTotal = if (canCalculate) pricePerHour.multiply(BigDecimal.valueOf(hours)) else BigDecimal.ZERO
    val finalTotal = rentalTotal.add(depositAmount)
    val canSubmit = canCalculate && address.isNotBlank() && rentalState !is RentalState.Submitting

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                viewModel.getAddressFromPoint(point)
                mapViewportState.setCameraOptions {
                    zoom(15.0)
                    center(point)
                }
            }
        )

        HorizontalDivider()
        PaymentMethodSelector(selectedPaymentMethod) { selectedPaymentMethod = it }
        HorizontalDivider()

        RentalSummary(
            pricePerHour = pricePerHour,
            depositAmount = depositAmount,
            chargeableHours = hours,
            rentalTotal = rentalTotal,
            finalTotal = finalTotal,
            canCalculate = canCalculate
        )

        Button(
            onClick = {
                viewModel.createRentalRequest(
                    item = item,
                    startAt = startAt!!.toInstant(),
                    endAt = endAt!!.toInstant(),
                    totalPrice = finalTotal,
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

        MapboxSearchBar(
            value = address,
            onValueChange = onAddressChange,
            proximity = selectedPoint,
            onPlacePicked = { point, fullAddress ->
                onPointSelected(point)
                onAddressChange(fullAddress)
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
                MapEffect(Unit) { mapView ->
                    mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
                    mapView.gestures.addOnMapClickListener(OnMapClickListener { point ->
                        onPointSelected(point)
                        true
                    })
                }

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
    depositAmount: BigDecimal,
    chargeableHours: Long,
    rentalTotal: BigDecimal,
    finalTotal: BigDecimal,
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
            Text("Tiền thuê dự kiến", style = MaterialTheme.typography.bodySmall)
            Text(money(rentalTotal), style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tiền đặt cọc", style = MaterialTheme.typography.bodySmall)
            Text(money(depositAmount), style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tổng tiền thanh toán", style = MaterialTheme.typography.titleSmall)
            Text(money(finalTotal), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}