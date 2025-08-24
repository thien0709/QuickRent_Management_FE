package com.bxt.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var searchText by remember { mutableStateOf("") }

    val homeState by viewModel.homeState.collectAsState()
    val isDarkModeEnabled by viewModel.isDarkModeEnabled.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    // Lottie (giữ nguyên hiệu ứng trống)
    val empty by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty))
    val progress by animateLottieCompositionAsState(empty, iterations = LottieConstants.IterateForever)

    // Quyền vị trí (giữ logic cũ)
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

    // --- Pull-down refresh (AndroidX) ---
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    // --- Pull-up to load-more (chỉ khi chạm đáy rồi kéo thêm) ---
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ====== GIỮ NGUYÊN LAYOUT/GIAO DIỆN BÊN DƯỚI ======

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Welcome back!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
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
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = { locationViewModel.fetchCurrentLocation() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Change Location", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                        placeholder = { Text("Search", color = Color(0xFF999999)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )

                    Spacer(Modifier.width(12.dp))

                    Card(
                        modifier = Modifier.size(56.dp).clickable { onFilterClick() },
                        shape = RoundedCornerShape(16.dp),
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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATEGORIES",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "View all category",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onAllCategoriesClick() }
                    )
                }
            }

            item {
                if (success.categories.isEmpty()) {
                    EmptyLottie(empty, progress)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(success.categories) { category ->
                            CategoryCard(category = category, onClick = { onCategoryClick(category) })
                        }
                    }
                }
            }

            item {
                Text(
                    text = "POPULAR TODAY",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 1.2.sp
                )
            }

            if (success.popularItems.isEmpty()) {
                item { EmptyLottie(empty, progress) }
            } else {
                items(success.popularItems /* , key = { it.id } nếu có id */) { it ->
                    PopularItemCard(item = it, onClick = { onItemClick(it) })
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Footer spinner khi đang tải thêm
            item {
                if (isLoadingMore) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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

/* ---------- Helpers (tối giản & tách bạch) ---------- */

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
