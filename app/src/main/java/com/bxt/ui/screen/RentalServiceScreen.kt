package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.components.RentalRequestCard
import com.bxt.ui.state.RentalServiceState
import com.bxt.viewmodel.RentalServiceViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun RentalServiceScreen(
    onBackClick: () -> Unit,
    onRentalClick: (Long?) -> Unit,
    viewModel: RentalServiceViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()

    val thumbs by viewModel.thumbs.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val updatingRequestId by viewModel.isUpdatingStatus.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    val pullUpConnection = rememberPullUpToLoadMore(
        listState = listState,
        isLoadingMore = isLoadingMore,
        trigger = 96.dp,
        onLoadMore = { viewModel.loadNextPage() }
    )

    LaunchedEffect(selectedTab) {
        val mode = if (selectedTab == 0) RentalServiceViewModel.LoadMode.OWNER else RentalServiceViewModel.LoadMode.RENTER
        viewModel.switchMode(mode)
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopAppBar(
                title = { Text("Quản lý thuê") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Đơn của tôi") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Đơn tôi thuê") })
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullUpConnection)
            ) {
                when (val state = uiState) {
                    is RentalServiceState.Loading -> {
                        LoadingIndicator()
                    }
                    is RentalServiceState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lỗi: ${state.message}")
                        }
                    }
                    is RentalServiceState.Success -> {
                        if (state.requests.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Chưa có yêu cầu nào")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items = state.requests, key = { it.id!! }) { req ->
                                    RentalRequestCard(
                                        data = req,
                                        isOwnerMode = selectedTab == 0,
                                        thumbnailUrl = thumbs[req.itemId],
                                        address = addresses[req.id],
                                        isUpdating = updatingRequestId == req.id,
                                        onView = { onRentalClick(req.id) },
                                        onConfirm = { viewModel.confirmRequest(req.id!!) },
                                        onReject = { viewModel.rejectRequest(req.id!!) },
                                        onCancel = { viewModel.cancelRequest(req.id!!) }
                                    )
                                }

                                if (isLoadingMore) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
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

@Composable
private fun rememberPullUpToLoadMore(
    listState: androidx.compose.foundation.lazy.LazyListState,
    isLoadingMore: Boolean,
    trigger: Dp,
    onLoadMore: () -> Unit
): NestedScrollConnection {
    val density = LocalDensity.current
    val triggerPx = with(density) { trigger.toPx() }
    var accumulated by remember(isLoadingMore) { mutableStateOf(0f) }

    return remember(isLoadingMore, triggerPx, listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag && !listState.canScrollForward) {
                    val dy = available.y
                    if (dy < 0f) { // Scroll xuống
                        accumulated += -dy
                        if (accumulated >= triggerPx && !isLoadingMore) {
                            accumulated = 0f
                            println("🚀 PULL UP: Triggering load more")
                            onLoadMore()
                        }
                    } else {
                        accumulated = 0f
                    }
                } else if (source == NestedScrollSource.Drag && listState.canScrollForward) {
                    accumulated = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                accumulated = 0f
                return Velocity.Zero
            }
        }
    }
}