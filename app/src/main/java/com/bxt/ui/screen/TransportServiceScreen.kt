// file: com/bxt/ui/screen/TransportServiceScreen.kt

package com.bxt.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bxt.ui.components.ErrorPopupManager
import com.bxt.ui.components.TransportServiceCard
import com.bxt.ui.state.TransportServiceListState
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.viewmodel.TransportServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportServiceScreen(
    navController: NavController,
    viewModel: TransportServiceViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Filter states
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var addressQuery by remember { mutableStateOf("") }

    // Xử lý lỗi
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is TransportServiceListState.Error) {
            if (state.message.contains("forbidden", ignoreCase = true) ||
                state.message.contains("unauthorized", ignoreCase = true)
            ) {
                Toast.makeText(context, "Phiên đăng nhập đã hết hạn.", Toast.LENGTH_LONG).show()
                navController.navigate("login_route") { popUpTo(0) { inclusive = true } }
            } else {
                ErrorPopupManager.showError(
                    message = state.message,
                    canRetry = true,
                    onRetry = { viewModel.loadTransportServices() }
                )
            }
        }
    }

    fun getFilteredServices(services: List<TransportServiceResponse>): List<TransportServiceResponse> {
        return services.filter { service ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val searchText = "Chuyến đi #${service.id} ${service.description ?: ""}"
                searchText.contains(searchQuery, ignoreCase = true)
            }

            val matchesAddress = if (addressQuery.isBlank()) true else {
                val description = service.description ?: ""
                description.contains(addressQuery, ignoreCase = true)
            }

            val matchesPrice = try {
                val min = minPrice.toDoubleOrNull()
                val max = maxPrice.toDoubleOrNull()
                val servicePrice = service.deliveryFee?.toDouble() ?: 0.0

                when {
                    min != null && max != null -> servicePrice in min..max
                    min != null -> servicePrice >= min
                    max != null -> servicePrice <= max
                    else -> true
                }
            } catch (e: Exception) {
                true
            }

            matchesSearch && matchesAddress && matchesPrice
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search và Filter bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Tìm kiếm dịch vụ...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Xóa")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bộ lọc: ${if (minPrice.isNotEmpty() || maxPrice.isNotEmpty() || addressQuery.isNotEmpty()) "Đã áp dụng" else "Chưa áp dụng"}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    TextButton(
                        onClick = { showFilterSheet = true }
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bộ lọc")
                    }
                }
            }
        }

        // Nội dung chính
        Box(modifier = Modifier.fillMaxSize()) {
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
                        val filteredServices = getFilteredServices(state.services)

                        if (filteredServices.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (searchQuery.isNotBlank() || minPrice.isNotEmpty() ||
                                            maxPrice.isNotEmpty() || addressQuery.isNotEmpty())
                                            "Không tìm thấy kết quả phù hợp với bộ lọc."
                                        else "Không có dịch vụ nào.",
                                        modifier = Modifier.padding(16.dp)
                                    )

                                    if (searchQuery.isNotBlank() || minPrice.isNotEmpty() ||
                                        maxPrice.isNotEmpty() || addressQuery.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                searchQuery = ""
                                                minPrice = ""
                                                maxPrice = ""
                                                addressQuery = ""
                                            }
                                        ) {
                                            Text("Xóa bộ lọc")
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredServices, key = { it.id!! }) { service ->
                                    TransportServiceCard(
                                        service = service,
                                        onDelete = { viewModel.deleteTransportService(it) },
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                    is TransportServiceListState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Đã xảy ra lỗi khi tải dữ liệu")
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadTransportServices() }) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Bộ lọc dịch vụ",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Lọc theo địa chỉ
                OutlinedTextField(
                    value = addressQuery,
                    onValueChange = { addressQuery = it },
                    label = { Text("Mô tả/Địa chỉ") },
                    placeholder = { Text("Nhập mô tả hoặc địa chỉ cần tìm...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lọc theo giá
                Text(
                    text = "Khoảng giá",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minPrice,
                        onValueChange = { minPrice = it },
                        label = { Text("Giá tối thiểu") },
                        placeholder = { Text("0") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maxPrice,
                        onValueChange = { maxPrice = it },
                        label = { Text("Giá tối đa") },
                        placeholder = { Text("∞") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            searchQuery = ""
                            minPrice = ""
                            maxPrice = ""
                            addressQuery = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Xóa tất cả")
                    }

                    Button(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Áp dụng")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}