package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.ui.components.PopularItemCard
import com.bxt.ui.state.LocationState
import com.bxt.util.haversineKm
import com.bxt.viewmodel.LocationViewModel
import com.bxt.viewmodel.SearchItemViewModel
import kotlinx.coroutines.flow.*
import java.lang.Double.parseDouble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchItemScreen(
    onNavigateBack: () -> Unit,
    onItemClick: (ItemResponse) -> Unit,
    vm: SearchItemViewModel = hiltViewModel(),
    locationVM: LocationViewModel = hiltViewModel()
) {
    val q by vm.query.collectAsState()
    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()
    val end by vm.endReached.collectAsState()
    val addresses by vm.addresses.collectAsState()

    val loc by locationVM.locationState.collectAsState()
    val my = (loc as? LocationState.Success)?.location

    val listState = rememberLazyListState()
    var showFilter by remember { mutableStateOf(false) }

    // Infinite scroll
    LaunchedEffect(listState, loadingMore, end, loading) {
        snapshotFlow { listState.layoutInfo }
            .map { info ->
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = info.totalItemsCount
                total > 0 && last >= total - 3
            }
            .distinctUntilChanged()
            .filter { it && !loadingMore && !end && !loading }
            .collectLatest { vm.loadMore() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = q,
                        onValueChange = vm::setQuery,
                        placeholder = { Text("Nhập tên sản phẩm để tìm…") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showFilter = true }) {
                        Icon(Icons.Default.FilterList, null)
                    }
                    if (q.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                }
            )
        }
    ) { pad ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize().padding(pad)
        ) {
            if (loading && items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id ?: it.hashCode() }) { item ->
                        val dKm: Double? = my?.let { (uLat, uLng) ->
                            val iLat = item.lat?.toDouble(); val iLng = item.lng?.toDouble()
                            if (iLat != null && iLng != null) haversineKm(uLat, uLng, iLat, iLng) else null
                        }
                        PopularItemCard(
                            item = item,
                            locationText = addresses[item.id], // đã prefetch -> không còn hiện lat/lng
                            distanceKm = dKm,
                            onClick = { onItemClick(item) }
                        )
                    }
                    if (loadingMore) item {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) { CircularProgressIndicator() }
                    }
                    if (end && items.isNotEmpty()) item {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) { Text("Đã hiển thị hết kết quả") }
                    }
                }
            }
        }
    }

    if (showFilter) {
        PriceLocationFilterSheet(
            onApply = { min, max, near, radius ->
                vm.setPrice(min, max)
                if (near && my == null) locationVM.fetchCurrentLocation()
                vm.setNearMe(near, my, radius)
                showFilter = false
            },
            onClear = {
                vm.setPrice(null, null)
                vm.setNearMe(false, null, 5.0)
            },
            onDismiss = { showFilter = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceLocationFilterSheet(
    onApply: (minPrice: Double?, maxPrice: Double?, nearMe: Boolean, radiusKm: Double) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var min by remember { mutableStateOf("") }
    var max by remember { mutableStateOf("") }
    var near by remember { mutableStateOf(false) }
    var radius by remember { mutableStateOf("5") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Bộ lọc", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = min, onValueChange = { min = it },
                label = { Text("Giá tối thiểu") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = max, onValueChange = { max = it },
                label = { Text("Giá tối đa") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gần tôi")
                Switch(checked = near, onCheckedChange = { near = it })
            }
            if (near) {
                OutlinedTextField(
                    value = radius, onValueChange = { radius = it },
                    label = { Text("Bán kính (km)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { min = ""; max = ""; near = false; radius = "5"; onClear() },
                    modifier = Modifier.weight(1f)
                ) { Text("Xoá") }
                Button(
                    onClick = {
                        onApply(
                            min.toDoubleOrNull(),
                            max.toDoubleOrNull(),
                            near,
                            radius.toDoubleOrNull() ?: 5.0
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Áp dụng") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
