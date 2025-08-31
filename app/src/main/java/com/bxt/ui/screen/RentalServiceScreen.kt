package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.di.ErrorResponse
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.components.RentalRequestCard
import com.bxt.ui.state.RentalRequestsState
import com.bxt.ui.theme.LocalDimens
import com.bxt.viewmodel.RentalServiceViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
// Thêm import cho pull to refresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
// Thêm import cho nested scroll connection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.CircularProgressIndicator


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun RentalServiceScreen(
    onBackClick: () -> Unit,
    onRentalClick: (Long?) -> Unit,
    viewModel: RentalServiceViewModel = hiltViewModel()
) {
    val d = LocalDimens.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Lấy trạng thái từ ViewModel
    val state by viewModel.state.collectAsState()
    val thumbs by viewModel.thumbs.collectAsState()
    val addresses by viewModel.addresses.collectAsState()
    val updatingRequestId by viewModel.isUpdatingStatus.collectAsState()
    val errorEvent by viewModel.errorEvent.collectAsState()

    // Trạng thái cho Pull-to-Refresh
    val isRefreshing by remember(state) {
        derivedStateOf { state is RentalRequestsState.Loading }
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    val listState = rememberLazyListState()

    // Kết nối cuộn để load thêm
    val isLoadingMore by remember(state) {
        derivedStateOf { (state as? RentalRequestsState.Success)?.isLoadingMore ?: false }
    }
    val pullUpConnection = rememberPullUpToLoadMore(
        listState = listState,
        isLoadingMore = isLoadingMore,
        trigger = d.buttonHeight,
        onLoadMore = { viewModel.loadNextPage() }
    )

    LaunchedEffect(errorEvent) {
        errorEvent?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.errorEventConsumed()
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) viewModel.loadByOwner() else viewModel.loadByRenter()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopAppBar(
                title = { Text("Quản lý thuê") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { positions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Đơn của tôi", style = MaterialTheme.typography.bodySmall) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Đơn tôi thuê", style = MaterialTheme.typography.bodySmall) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState) // Thêm pull-to-refresh
                    .nestedScroll(pullUpConnection) // Thêm pull-up to load more
            ) {
                when (val s = state) {
                    is RentalRequestsState.Loading -> {
                        // Chỉ hiển thị LoadingIndicator nếu không phải đang refresh
                        if (!isRefreshing) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                        }
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
                        if (data.isEmpty() && !s.canLoadMore) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Chưa có yêu cầu", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(d.pagePadding),
                                verticalArrangement = Arrangement.spacedBy(d.rowGap)
                            ) {
                                items(items = data, key = { it.id ?: it.hashCode().toLong() }) { req ->
                                    val thumbnailUrl = thumbs[req.itemId]
                                    val address = addresses[req.id]

                                    RentalRequestCard(
                                        data = req,
                                        isOwnerMode = selectedTab == 0,
                                        thumbnailUrl = thumbnailUrl,
                                        address = address,
                                        isUpdating = updatingRequestId == req.id,
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
                                if (s.isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = d.rowGap),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Hiển thị indicator kéo để tải lại
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
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
    val d = LocalDimens.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(d.pagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.rowGap, Alignment.CenterVertically),
    ) {
        Text(
            error.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        if (error.canRetry) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(d.buttonHeight),
                shape = MaterialTheme.shapes.medium
            ) { Text("Thử lại", style = MaterialTheme.typography.bodySmall) }
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(d.buttonHeight),
            shape = MaterialTheme.shapes.medium
        ) { Text("Quay lại", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun rememberPullUpToLoadMore(
    listState: LazyListState,
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
                    if (dy < 0f) {
                        accumulated += -dy
                        if (accumulated >= triggerPx && !isLoadingMore) {
                            accumulated = 0f
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