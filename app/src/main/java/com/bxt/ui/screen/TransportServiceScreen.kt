package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.ui.components.ErrorPopupManager
import com.bxt.ui.components.ExpandableFab
import com.bxt.ui.components.TransportServiceCard
import com.bxt.ui.state.LocationState
import com.bxt.ui.state.TransportServiceListState
import com.bxt.ui.theme.LocalDimens
import com.bxt.util.FabActions
import com.bxt.util.haversineKm
import com.bxt.viewmodel.LocationViewModel
import com.bxt.viewmodel.TransportServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportServiceScreen(
    navController: NavController,
    viewModel: TransportServiceViewModel,
    locationViewModel: LocationViewModel = hiltViewModel()
) {
    val d = LocalDimens.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val addressMap by viewModel.addressMap.collectAsStateWithLifecycle()

    val locState by locationViewModel.locationState.collectAsStateWithLifecycle()
    val userLatLng: Pair<Double, Double>? = (locState as? LocationState.Success)?.location

    var showFilter by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var addrQ by remember { mutableStateOf("") }
    var nearMe by remember { mutableStateOf(false) }
    var radiusKmStr by remember { mutableStateOf("15") }
    val radiusKm = radiusKmStr.toDoubleOrNull().takeIf { nearMe }

    LaunchedEffect(uiState) {
        val s = uiState
        if (s is TransportServiceListState.Error) {
            if (s.message.contains("forbidden", true) || s.message.contains("unauthorized", true)) {
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            } else {
                ErrorPopupManager.showError(
                    message = s.message,
                    canRetry = true,
                    onRetry = { viewModel.loadTransportServices() }
                )
            }
        }
    }

    fun List<TransportServiceResponse>.filtered(): List<TransportServiceResponse> {
        val min = minPrice.toDoubleOrNull()
        val max = maxPrice.toDoubleOrNull()
        val (ulat, ulng) = userLatLng ?: (null to null)

        return filter { s ->
            val searchText = buildString {
                append("Chuyến đi #${s.id ?: ""} ")
                append(s.description.orEmpty())
            }
            val price = s.deliveryFee?.toDouble() ?: 0.0
            val okSearch = q.isBlank() || searchText.contains(q, true)

            val pair = s.id?.let { addressMap[it] }
            val fromAddr = pair?.first.orEmpty()
            val toAddr = pair?.second.orEmpty()
            val okAddr = addrQ.isBlank() ||
                    fromAddr.contains(addrQ, true) ||
                    toAddr.contains(addrQ, true) ||
                    s.description.orEmpty().contains(addrQ, true)

            val okPrice = when {
                min != null && max != null -> price in min..max
                min != null -> price >= min
                max != null -> price <= max
                else -> true
            }

            val okDistance = if (ulat != null && ulng != null && radiusKm != null) {
                val dFrom = if (s.fromLatitude != null && s.fromLongitude != null)
                    haversineKm(ulat, ulng, s.fromLatitude.toDouble(), s.fromLongitude.toDouble())
                else Double.POSITIVE_INFINITY
                val dTo = if (s.toLatitude != null && s.toLongitude != null)
                    haversineKm(ulat, ulng, s.toLatitude.toDouble(), s.toLongitude.toDouble())
                else Double.POSITIVE_INFINITY
                minOf(dFrom, dTo) <= radiusKm
            } else true

            okSearch && okAddr && okPrice && okDistance
        }
    }

    Column(Modifier.fillMaxSize()) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.pagePadding, vertical = d.rowGap),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                Modifier.padding(d.pagePadding),
                verticalArrangement = Arrangement.spacedBy(d.rowGap)
            ) {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    label = { Text("Tìm kiếm dịch vụ…", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (q.isNotEmpty()) {
                            IconButton(onClick = { q = "" }) { Icon(Icons.Default.Clear, null) }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = d.fieldMinHeight),
                    textStyle = MaterialTheme.typography.bodySmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val applied = remember(minPrice, maxPrice, addrQ, nearMe, radiusKmStr) {
                        minPrice.isNotEmpty() || maxPrice.isNotEmpty() ||
                                addrQ.isNotEmpty() || (nearMe && radiusKmStr.isNotBlank())
                    }
                    Text(
                        "Bộ lọc: ${if (applied) "Đã áp dụng" else "Chưa áp dụng"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { showFilter = true }) {
                        Icon(Icons.Default.FilterList, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Bộ lọc", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            ErrorPopupManager.ErrorPopup()

            val refreshing = uiState is TransportServiceListState.Loading
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { viewModel.loadTransportServices() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val s = uiState) {
                    is TransportServiceListState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is TransportServiceListState.Success -> {
                        val data = remember(
                            s.services, q, minPrice, maxPrice, addrQ, addressMap, userLatLng, radiusKm
                        ) { s.services.filtered() }

                        if (data.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val hasFilter = q.isNotBlank() || minPrice.isNotBlank()
                                            || maxPrice.isNotBlank() || addrQ.isNotBlank()
                                            || (nearMe && radiusKmStr.isNotBlank())
                                    Text(
                                        text = if (hasFilter) "Không tìm thấy kết quả phù hợp."
                                        else "Chưa có dịch vụ.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (hasFilter) {
                                        TextButton(onClick = {
                                            q = ""; minPrice = ""; maxPrice = ""; addrQ = ""
                                            nearMe = false; radiusKmStr = "15"
                                        }) { Text("Xóa bộ lọc") }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = d.pagePadding, vertical = d.rowGap
                                ),
                                verticalArrangement = Arrangement.spacedBy(d.sectionGap)
                            ) {
                                items(data, key = { it.id ?: it.hashCode().toLong() }) { item ->
                                    val pair = item.id?.let { addressMap[it] }
                                    TransportServiceCard(
                                        service = item,
                                        fromAddress = pair?.first ?: "Đang lấy địa chỉ…",
                                        toAddress = pair?.second ?: "Đang lấy địa chỉ…",
                                        onClick = { /* mở chi tiết */ }
                                    )
                                }
                            }
                        }
                    }
                    is TransportServiceListState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(d.pagePadding),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Có lỗi khi tải dữ liệu", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(d.rowGap))
                            Button(
                                onClick = { viewModel.loadTransportServices() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(d.buttonHeight),
                                shape = MaterialTheme.shapes.medium
                            ) { Text("Thử lại") }
                        }
                    }
                }
            }
        }
    }

    if (showFilter) {
        FilterSheet(
            addressQuery = addrQ, onAddressQuery = { addrQ = it },
            minPrice = minPrice, onMinPrice = { minPrice = it },
            maxPrice = maxPrice, onMaxPrice = { maxPrice = it },
            nearMeEnabled = nearMe, onNearMeEnabled = { nearMe = it },
            radiusKmStr = radiusKmStr, onRadiusKmStr = { radiusKmStr = it },
            onRequestLocation = { locationViewModel.fetchCurrentLocation() },
            locationState = locState,
            onDismiss = { showFilter = false }
        )
    }

    ExpandableFab(actions = FabActions.transport(navController))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    addressQuery: String,
    onAddressQuery: (String) -> Unit,
    minPrice: String,
    onMinPrice: (String) -> Unit,
    maxPrice: String,
    onMaxPrice: (String) -> Unit,
    nearMeEnabled: Boolean,
    onNearMeEnabled: (Boolean) -> Unit,
    radiusKmStr: String,
    onRadiusKmStr: (String) -> Unit,
    onRequestLocation: () -> Unit,
    locationState: LocationState,
    onDismiss: () -> Unit
) {
    val d = LocalDimens.current
    val cfg = LocalConfiguration.current
    val maxHeight = (cfg.screenHeightDp * 0.9f).dp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(d.pagePadding),
            verticalArrangement = Arrangement.spacedBy(d.rowGap)
        ) {
            Text("Bộ lọc dịch vụ", style = MaterialTheme.typography.titleSmall)

            FilterTextField(
                value = addressQuery, onValueChange = onAddressQuery,
                label = "Mô tả/Địa chỉ"
            )

            Text("Khoảng giá", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.rowGap)
            ) {
                FilterTextField(
                    value = minPrice, onValueChange = onMinPrice,
                    label = "Giá tối thiểu", keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                FilterTextField(
                    value = maxPrice, onValueChange = onMaxPrice,
                    label = "Giá tối đa", keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Gần tôi", style = MaterialTheme.typography.titleSmall)
                    val locText = when (locationState) {
                        is LocationState.Success -> "Đã có vị trí hiện tại"
                        is LocationState.Loading -> "Đang lấy vị trí…"
                        is LocationState.PermissionRequired -> "Chưa có quyền vị trí"
                        is LocationState.GpsDisabled -> "GPS đang tắt"
                        is LocationState.Error -> "Lỗi vị trí"
                    }
                    Text(locText, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = nearMeEnabled, onCheckedChange = onNearMeEnabled)
            }

            if (nearMeEnabled) {
                FilterTextField(
                    value = radiusKmStr, onValueChange = onRadiusKmStr,
                    label = "Bán kính (km)", keyboardType = KeyboardType.Number
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onRequestLocation) { Text("Lấy vị trí hiện tại") }
                }
            }

            Spacer(Modifier.height(d.sectionGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.rowGap)
            ) {
                OutlinedButton(
                    onClick = {
                        onAddressQuery("")
                        onMinPrice("")
                        onMaxPrice("")
                        onNearMeEnabled(false)
                        onRadiusKmStr("15")
                    },
                    modifier = Modifier.weight(1f).height(d.buttonHeight),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Xóa tất cả") }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(d.buttonHeight),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Áp dụng") }
            }
        }
    }
}

@Composable
private fun FilterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    val d = LocalDimens.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodySmall,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = d.fieldMinHeight)
    )
}
