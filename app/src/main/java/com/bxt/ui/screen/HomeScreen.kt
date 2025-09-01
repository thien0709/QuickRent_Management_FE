package com.bxt.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.*
import com.bxt.R
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.ui.components.CategoryCard
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.components.LocationPermissionHandler
import com.bxt.ui.components.PopularItemCard
import com.bxt.ui.state.HomeState
import com.bxt.ui.state.LocationState
import com.bxt.ui.theme.LocalDimens
import com.bxt.viewmodel.HomeViewModel
import com.bxt.viewmodel.LocationViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onCategoryClick: (CategoryResponse) -> Unit,
    onItemClick: (ItemResponse) -> Unit,
    onAllCategoriesClick: () -> Unit,
    onFilterClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    locationViewModel: LocationViewModel = hiltViewModel()
) {
    val d = LocalDimens.current
    var searchText by remember { mutableStateOf("") }
    val itemAddresses by viewModel.itemAddresses.collectAsState()
    val homeState by viewModel.homeState.collectAsState()
    val isDarkModeEnabled by viewModel.isDarkModeEnabled.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    // Lottie (giữ nguyên hiệu ứng trống)
    val empty by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty))
    val progress by animateLottieCompositionAsState(empty, iterations = LottieConstants.IterateForever)

    // Quyền vị trí
    LocationPermissionHandler(
        onPermissionGranted = { locationViewModel.fetchCurrentLocation() },
        onPermissionDenied = {}
    )
    val locationState by locationViewModel.locationState.collectAsState()

    val deliveryText = when (val s = locationState) {
        is LocationState.Loading -> "Đang lấy địa chỉ..."
        is LocationState.Success -> s.address ?: "Vị trí hiện tại"
        is LocationState.Error -> "Lỗi: ${s.message}"
        is LocationState.PermissionRequired -> "Cần cấp quyền vị trí"
        is LocationState.GpsDisabled -> "GPS đang tắt"
    }

    val hasAllData = homeState is HomeState.Success && locationState !is LocationState.Loading
    if (!hasAllData) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) { LoadingIndicator() }
        return
    }
    val success = homeState as HomeState.Success

    // Pull-down refresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    // Chạm đáy rồi kéo thêm để load-more
    val listState = rememberLazyListState()
    val pullUpConnection = rememberPullUpToLoadMore(
        listState = listState,
        isLoadingMore = isLoadingMore,
        trigger = 96.dp, // kéo thêm ~96dp ở đáy sẽ gọi loadNextPage()
        onLoadMore = { viewModel.loadNextPage() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pullRefresh(pullRefreshState)
            .nestedScroll(pullUpConnection)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(d.pagePadding),
            verticalArrangement = Arrangement.spacedBy(d.sectionGap)
        ) {
            // ====== GIỮ NGUYÊN LAYOUT GẦN NHƯ CŨ ======

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Welcome back!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Switch(
                        checked = isDarkModeEnabled,
                        onCheckedChange = { viewModel.setDarkModeEnabled(it) }
                    )
                }
            }

            item {
                Text(
                    text = "Delivery to: $deliveryText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = { locationViewModel.fetchCurrentLocation() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "Change Location",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search", style = MaterialTheme.typography.bodySmall, color = Color(0xFF999999)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.width(d.rowGap))

                    Card(
                        modifier = Modifier
                            .size(56.dp) // giữ 56dp như cũ
                            .clickable { onFilterClick() },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Filter",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = d.rowGap, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATEGORIES",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "View all category",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onAllCategoriesClick() }
                    )
                }
            }

            item {
                if (success.categories.isEmpty()) {
                    EmptyLottie(empty, progress)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(d.sectionGap)) {
                        items(success.categories) { category ->
                            CategoryCard(category = category, onClick = { onCategoryClick(category) })
                        }
                    }
                }
            }

            item {
                Text(
                    text = "POPULAR TODAY",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (success.popularItems.isEmpty()) {
                item { EmptyLottie(empty, progress) }
            } else {
                items(success.popularItems, key = { it.id ?: it.hashCode() }) { item ->
                    val address = itemAddresses[item.id]
                    PopularItemCard(
                        item = item,
                        address = address,
                        onClick = { onItemClick(item) }
                    )

                    Spacer(Modifier.height(d.rowGap))
                }
            }

            item {
                if (isLoadingMore) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = d.rowGap),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/* ---------- Helpers ---------- */

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
                    if (dy < 0f) { // kéo thêm ở đáy
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

@Composable
private fun EmptyLottie(composition: LottieComposition?, progress: Float) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.size(90.dp))
    }
}
