package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.components.TransportServiceCard
import com.bxt.viewmodel.TransportRequestState
import com.bxt.viewmodel.TransportRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportRequestScreen(
    navController: NavController,
    viewModel: TransportRequestViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                if (lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 3) {
                    viewModel.loadNextPage()
                }
            }
    }

    LaunchedEffect(selectedTab) {
        val mode = when(selectedTab) {
            0 -> TransportRequestViewModel.LoadMode.DRIVER
            1 -> TransportRequestViewModel.LoadMode.PARTICIPANT
            2 -> TransportRequestViewModel.LoadMode.SENDER
            3 -> TransportRequestViewModel.LoadMode.RECEIVER
            else -> TransportRequestViewModel.LoadMode.DRIVER
        }
        viewModel.switchMode(mode)
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopAppBar(
                title = { Text("Quản lý Chuyến đi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Chuyến đi của tôi") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Chuyến đi tham gia") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Gói hàng đã gửi") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Gói hàng đã nhận") })
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is TransportRequestState.Loading -> LoadingIndicator()
                    is TransportRequestState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Lỗi: ${state.message}")
                    }
                    is TransportRequestState.Success -> {
                        if (state.services.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Chưa có chuyến đi nào")
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items = state.services, key = { it.id!! }) { service ->
                                    val addressPair = addresses[service.id]
                                    TransportServiceCard(
                                        service = service,
                                        fromAddress = addressPair?.first ?: "Đang tải...",
                                        toAddress = addressPair?.second ?: "Đang tải...",
                                        onClick = {
                                            navController.navigate("transport_detail/${service.id}")
                                        }
                                    )
                                }

                                item {
                                    if (isLoadingMore) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}