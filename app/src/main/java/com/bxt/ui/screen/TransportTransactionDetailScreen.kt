package com.bxt.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.state.TransportTransactionState
import com.bxt.viewmodel.TransportTransactionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportManagementScreen(
    navController: NavController,
    viewModel: TransportTransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Lắng nghe các sự kiện từ ViewModel
    LaunchedEffect(Unit) {
        viewModel.actionResult.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateBackEvent.collect {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý chuyến đi #${viewModel.serviceId}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is TransportTransactionState.Loading -> LoadingIndicator()
                is TransportTransactionState.Error -> Text("Lỗi: ${state.message}")
                is TransportTransactionState.Success -> {
                    val details = state.details
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Phần 1: Trạng thái và hành động
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Trạng thái hiện tại: ${details.service.status}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Hành động:", style = MaterialTheme.typography.titleSmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Hiển thị các nút tùy theo trạng thái hiện tại
                                        if (details.service.status == "PENDING") {
                                            Button(onClick = { viewModel.updateStatus("CONFIRMED") }, modifier = Modifier.weight(1f)) {
                                                Text("Xác nhận chuyến")
                                            }
                                        }
                                        if (details.service.status == "CONFIRMED") {
                                            Button(onClick = { viewModel.updateStatus("IN_PROGRESS") }, modifier = Modifier.weight(1f)) {
                                                Text("Bắt đầu chuyến")
                                            }
                                        }
                                        if (details.service.status == "IN_PROGRESS") {
                                            Button(onClick = { viewModel.updateStatus("COMPLETED") }, modifier = Modifier.weight(1f)) {
                                                Text("Hoàn thành")
                                            }
                                        }
                                        if (details.service.status != "COMPLETED" && details.service.status != "CANCELLED") {
                                            OutlinedButton(onClick = { viewModel.cancelTrip() }, modifier = Modifier.weight(1f)) {
                                                Text("Hủy chuyến")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Phần 2: Danh sách hành khách
                        item {
                            Text("Danh sách hành khách (${details.passengers.size})", style = MaterialTheme.typography.titleLarge)
                        }
                        if (details.passengers.isEmpty()) {
                            item { Text("Chưa có hành khách nào đặt chỗ.") }
                        } else {
                            items(details.passengers) { passenger ->
                                ListItem(
                                    headlineContent = { Text("Hành khách ID: ${passenger.userId}") },
                                    supportingContent = { Text("Điểm đón: ${passenger.pickupLatitude}, ${passenger.pickupLongitude}") }
                                )
                            }
                        }

                        // Phần 3: Danh sách gói hàng
                        item {
                            Text("Danh sách gói hàng (${details.packages.size})", style = MaterialTheme.typography.titleLarge)
                        }
                        if (details.packages.isEmpty()) {
                            item { Text("Chưa có gói hàng nào được yêu cầu.") }
                        } else {
                            items(details.packages) { pkg ->
                                ListItem(
                                    headlineContent = { Text("Gói hàng ID: ${pkg.id}") },
                                    supportingContent = { Text("Từ: ${pkg.senderId} -> Đến: ${pkg.receiptId}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}