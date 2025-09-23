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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bxt.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportRequestScreen(
    navController: NavController,
    viewModel: TransportRequestViewModel = hiltViewModel()
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 3 && info.totalItemsCount > 0
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadNextPage() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý chuyến đi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Tabs: Tôi lái / Tôi tham gia
            TabRow(
                selectedTabIndex = if (tab == RequestTab.OWNER) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = tab == RequestTab.OWNER,
                    onClick = { viewModel.switchMode(RequestTab.OWNER) },
                    text = { Text("Tôi lái") }
                )
                Tab(
                    selected = tab == RequestTab.RENTER,
                    onClick = { viewModel.switchMode(RequestTab.RENTER) },
                    text = { Text("Tôi tham gia") }
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is com.bxt.ui.state.TransportRequestState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is com.bxt.ui.state.TransportRequestState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lỗi: ${state.message}")
                        }
                    }
                    is com.bxt.ui.state.TransportRequestState.Success -> {
                        val rows = state.items
                        if (rows.isEmpty()) {
                            EmptyState(tab)
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    rows,
                                    // KEY PHẢI DUY NHẤT — dùng prefix + id con
                                    key = { row ->
                                        when (row.kind) {
                                            ParticipationKind.OWNER ->
                                                "S:${row.service.id ?: "unknown"}"
                                            ParticipationKind.RIDE ->
                                                "R:${row.passenger?.id ?: "p-${row.service.id}-${row.hashCode()}"}"
                                            ParticipationKind.PKG_SEND,
                                            ParticipationKind.PKG_RECV ->
                                                "K:${row.pkg?.id ?: "k-${row.service.id}-${row.hashCode()}"}"
                                        }
                                    }
                                ) { row ->

                                    // Lấy địa chỉ (nếu chưa resolve sẽ hiện placeholder)
                                    val addrKey = when (row.kind) {
                                        ParticipationKind.OWNER    -> "S:${row.service.id}"
                                        ParticipationKind.RIDE     -> "R:${row.passenger?.id}"
                                        ParticipationKind.PKG_SEND -> "K:${row.pkg?.id}"
                                        ParticipationKind.PKG_RECV -> "K:${row.pkg?.id}"
                                    }
                                    val addr = addresses[addrKey]
                                    val fromText = addr?.first ?: "Đang lấy địa chỉ…"
                                    val toText   = addr?.second ?: "Đang lấy địa chỉ…"

                                    TripCard(
                                        row = row,
                                        fromText = fromText,
                                        toText = toText
                                    ) {
                                        val sid = row.service.id ?: return@TripCard
                                        // Điều hướng theo serviceId + entity cho VM biết bảng nào + id nào
                                        when (row.kind) {
                                            ParticipationKind.OWNER -> {
                                                navController.navigate(
                                                    "transport_service_detail/$sid"
                                                )
                                            }
                                            ParticipationKind.RIDE -> {
                                                val pid = row.passenger?.id ?: return@TripCard
                                                navController.navigate(
                                                    "transport_service_detail/$sid?entity=passenger&entityId=$pid"
                                                )
                                            }
                                            ParticipationKind.PKG_SEND,
                                            ParticipationKind.PKG_RECV -> {
                                                val pkgId = row.pkg?.id ?: return@TripCard
                                                navController.navigate(
                                                    "transport_service_detail/$sid?entity=package&entityId=$pkgId"
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isLoadingMore) item(key = "loading_more") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) { CircularProgressIndicator() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------- UI nhỏ gọn ---------- */

@Composable
private fun TripCard(
    row: UnifiedTrip,
    fromText: String,
    toText: String,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Chuyến #${row.service.id ?: "-"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindBadge(row.kind)
                    AssistChip(onClick = {}, label = { Text(row.service.status ?: "-") })
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(fromText, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text("→ $toText", style = MaterialTheme.typography.bodyMedium, maxLines = 1)

            row.pkg?.packageDescription?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, maxLines = 2, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetaPill("Phí dự kiến", row.service.deliveryFee?.toPlainString() ?: "-")
                MetaPill("Chỗ còn", row.service.availableSeat?.toString() ?: "-")
            }
        }
    }
}

@Composable
private fun EmptyState(tab: RequestTab) {
    val msg = if (tab == RequestTab.OWNER)
        "Bạn chưa có chuyến nào là chủ xe."
    else
        "Bạn chưa tham gia chuyến nào."
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun KindBadge(kind: ParticipationKind) {
    val text = when (kind) {
        ParticipationKind.OWNER    -> "Owner"
        ParticipationKind.RIDE     -> "Ride"
        ParticipationKind.PKG_SEND -> "Pkg Send"
        ParticipationKind.PKG_RECV -> "Pkg Recv"
    }
    SuggestionChip(onClick = {}, label = { Text(text) })
}

@Composable
private fun MetaPill(title: String, value: String) {
    AssistChip(onClick = {}, label = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    })
}
