package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.di.ErrorResponse
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.components.RentalRequestCard
import com.bxt.ui.state.RentalRequestsState
import com.bxt.viewmodel.RentalServiceViewModel

@Composable
fun RentalServiceScreen(
    onBackClick: () -> Unit,
    onRentalClick: (Long?) -> Unit,
    viewModel: RentalServiceViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // THAY ĐỔI: Lắng nghe cả 3 State
    val state by viewModel.state.collectAsState()
    val thumbs by viewModel.thumbs.collectAsState()
    val addresses by viewModel.addresses.collectAsState()

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) viewModel.loadByOwner() else viewModel.loadByRenter()
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Owner") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Renter") })
        }

        when (val s = state) {
            is RentalRequestsState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            }

            is RentalRequestsState.Error -> {
                ErrorPane(
                    error = s.message,
                    onRetry = {
                        if (selectedTab == 0) viewModel.loadByOwner() else viewModel.loadByRenter()
                    },
                    onBack = onBackClick
                )
            }

            is RentalRequestsState.Success -> {
                val data = s.data
                if (data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có yêu cầu")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = data,
                            key = { it.id ?: it.hashCode().toLong() }
                        ) { req ->
                            // THAY ĐỔI: Lấy thumbnail và địa chỉ từ các map đã collect
                            val thumbnailUrl = thumbs[req.itemId]
                            val address = addresses[req.id]

                            RentalRequestCard(
                                data = req,
                                isOwnerMode = selectedTab == 0,
                                thumbnailUrl = thumbnailUrl,
                                address = address,
                                onView = { onRentalClick(req.id) },
                                onChangeStatus = { newStatus ->
                                    viewModel.updateStatus(
                                        requestId = req.id,
                                        newStatus = newStatus
                                    ) {
                                        if (selectedTab == 0) viewModel.loadByOwner() else viewModel.loadByRenter()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorPane(
    error: ErrorResponse,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(error.message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        if (error.canRetry) {
            Button(onClick = onRetry) { Text("Thử lại") }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onBack) { Text("Quay lại") }
    }
}