package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.ui.components.ErrorPopupManager
import com.bxt.ui.components.ExpandableFab
import com.bxt.ui.components.TransportServiceCard
import com.bxt.ui.state.TransportServiceListState
import com.bxt.ui.theme.LocalDimens
import com.bxt.util.FabActions
import com.bxt.viewmodel.TransportServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportServiceScreen(
    navController: NavController,
    viewModel: TransportServiceViewModel
) {
    val d = LocalDimens.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val addressMap by viewModel.addressMap.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var addressQuery by remember { mutableStateOf("") }

    // Điều hướng về login nếu 401/403
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is TransportServiceListState.Error) {
            if (state.message.contains("forbidden", true) || state.message.contains("unauthorized", true)) {
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            } else {
                ErrorPopupManager.showError(
                    message = state.message,
                    canRetry = true,
                    onRetry = { viewModel.loadTransportServices() }
                )
            }
        }
    }

    // Filter helper — có xét cả địa chỉ đi/đến
    fun List<TransportServiceResponse>.filtered(
        q: String, minStr: String, maxStr: String, addrQ: String
    ): List<TransportServiceResponse> {
        val min = minStr.toDoubleOrNull()
        val max = maxStr.toDoubleOrNull()
        return filter { s ->
            val textForSearch = buildString {
                append("Chuyến đi #${s.id ?: ""} ")
                append(s.description.orEmpty())
            }
            val addresses = s.id?.let { id ->
                addressMap[id]
            }
            val fromAddr = addresses?.first.orEmpty()
            val toAddr = addresses?.second.orEmpty()

            val price = s.deliveryFee?.toDouble() ?: 0.0

            val okSearch = q.isBlank() || textForSearch.contains(q, ignoreCase = true)
            val okAddr = addrQ.isBlank() ||
                    fromAddr.contains(addrQ, ignoreCase = true) ||
                    toAddr.contains(addrQ, ignoreCase = true) ||
                    s.description.orEmpty().contains(addrQ, ignoreCase = true)
            val okPrice = when {
                min != null && max != null -> price in min..max
                min != null -> price >= min
                max != null -> price <= max
                else -> true
            }
            okSearch && okAddr && okPrice
        }
    }

    Column(Modifier.fillMaxSize()) {

        // Search & Filter bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.pagePadding, vertical = d.rowGap),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(d.pagePadding), verticalArrangement = Arrangement.spacedBy(d.rowGap)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Tìm kiếm dịch vụ...", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = d.fieldMinHeight),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    textStyle = MaterialTheme.typography.bodySmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val applied = remember(minPrice, maxPrice, addressQuery) {
                        minPrice.isNotEmpty() || maxPrice.isNotEmpty() || addressQuery.isNotEmpty()
                    }
                    Text(
                        text = "Bộ lọc: ${if (applied) "Đã áp dụng" else "Chưa áp dụng"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Bộ lọc", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Nội dung
        Box(Modifier.fillMaxSize()) {
            ErrorPopupManager.ErrorPopup()

            val isRefreshing = uiState is TransportServiceListState.Loading

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadTransportServices() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is TransportServiceListState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is TransportServiceListState.Success -> {
                        val filtered = remember(
                            state.services, searchQuery, minPrice, maxPrice, addressQuery, addressMap
                        ) {
                            state.services.filtered(searchQuery, minPrice, maxPrice, addressQuery)
                        }

                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val hasFilter = searchQuery.isNotBlank() ||
                                            minPrice.isNotEmpty() || maxPrice.isNotEmpty() || addressQuery.isNotEmpty()
                                    Text(
                                        text = if (hasFilter)
                                            "Không tìm thấy kết quả phù hợp với bộ lọc."
                                        else "Không có dịch vụ nào.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(d.pagePadding)
                                    )
                                    if (hasFilter) {
                                        TextButton(onClick = {
                                            searchQuery = ""; minPrice = ""; maxPrice = ""; addressQuery = ""
                                        }) { Text("Xóa bộ lọc", style = MaterialTheme.typography.bodySmall) }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = d.pagePadding, vertical = d.rowGap),
                                verticalArrangement = Arrangement.spacedBy(d.sectionGap)
                            ) {
                                items(
                                    items = filtered,
                                    key = { it.id ?: it.hashCode().toLong() }
                                ) { service ->
                                    val pair = service.id?.let { addressMap[it] }
                                    val fromAddr = pair?.first ?: "Đang lấy địa chỉ..."
                                    val toAddr = pair?.second ?: "Đang lấy địa chỉ..."

                                    // ⬇️ Dùng Card phiên bản nhận chuỗi địa chỉ (đã refactor)
                                    TransportServiceCard(
                                        service = service,
                                        fromAddress = fromAddr,
                                        toAddress = toAddr,
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
                            Text("Đã xảy ra lỗi khi tải dữ liệu", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(d.rowGap))
                            Button(
                                onClick = { viewModel.loadTransportServices() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(d.buttonHeight),
                                shape = MaterialTheme.shapes.medium
                            ) { Text("Thử lại", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            addressQuery = addressQuery,
            onAddressQuery = { addressQuery = it },
            minPrice = minPrice,
            onMinPrice = { minPrice = it },
            maxPrice = maxPrice,
            onMaxPrice = { maxPrice = it },
            onDismiss = { showFilterSheet = false }
        )
    }

    ExpandableFab(actions = com.bxt.util.FabActions.transport(navController))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    addressQuery: String,
    onAddressQuery: (String) -> Unit,
    minPrice: String,
    onMinPrice: (String) -> Unit,
    maxPrice: String,
    onMaxPrice: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val d = LocalDimens.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.pagePadding),
            verticalArrangement = Arrangement.spacedBy(d.rowGap)
        ) {
            Text(text = "Bộ lọc dịch vụ", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = addressQuery,
                onValueChange = onAddressQuery,
                label = { Text("Mô tả/Địa chỉ", style = MaterialTheme.typography.labelSmall) },
                placeholder = { Text("Nhập mô tả hoặc địa chỉ cần tìm...", style = MaterialTheme.typography.bodySmall) },
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().heightIn(min = d.fieldMinHeight),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Text("Khoảng giá", style = MaterialTheme.typography.bodySmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.rowGap)
            ) {
                OutlinedTextField(
                    value = minPrice,
                    onValueChange = onMinPrice,
                    label = { Text("Giá tối thiểu", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("0", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).heightIn(min = d.fieldMinHeight),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = onMaxPrice,
                    label = { Text("Giá tối đa", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("∞", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).heightIn(min = d.fieldMinHeight),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
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
                    },
                    modifier = Modifier.weight(1f).height(d.buttonHeight),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Xóa tất cả", style = MaterialTheme.typography.bodySmall) }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(d.buttonHeight),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Áp dụng", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
