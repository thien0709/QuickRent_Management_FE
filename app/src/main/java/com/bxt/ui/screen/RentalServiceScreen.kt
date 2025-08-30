package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    // Lắng nghe tất cả state từ ViewModel
    val state by viewModel.state.collectAsState()
    val thumbs by viewModel.thumbs.collectAsState()
    val addresses by viewModel.addresses.collectAsState()
    val updatingRequestId by viewModel.isUpdatingStatus.collectAsState()
    val errorEvent by viewModel.errorEvent.collectAsState()

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
                            Text("Chưa có yêu cầu", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
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
    val d = LocalDimens.current
    Column(
        Modifier.fillMaxSize().padding(d.pagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.rowGap),
    ) {
        Text(
            error.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        if (error.canRetry) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(d.buttonHeight),
                shape = MaterialTheme.shapes.medium
            ) { Text("Thử lại", style = MaterialTheme.typography.bodySmall) }
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(d.buttonHeight),
            shape = MaterialTheme.shapes.medium
        ) { Text("Quay lại", style = MaterialTheme.typography.bodySmall) }
    }
}