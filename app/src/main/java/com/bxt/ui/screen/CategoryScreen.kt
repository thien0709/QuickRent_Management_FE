// File: ui/screen/CategoryScreen.kt
package com.bxt.ui.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bxt.R
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.ui.components.CategoryCard
import com.bxt.ui.components.ExpandableFab
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.components.PopularItemCard
import com.bxt.ui.state.CategoryState
import com.bxt.ui.theme.LocalDimens
import com.bxt.util.extractDistrictOrWard
import com.bxt.util.haversineKm
import com.bxt.viewmodel.CategoryViewModel
import com.bxt.viewmodel.LocationViewModel
import com.bxt.ui.state.LocationState

@Composable
fun CategoryScreen(
    categoryId: Long? = null,
    navController: NavController,
    onProductClick: (Long) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
    locationViewModel: LocationViewModel = hiltViewModel()
) {
    LaunchedEffect(categoryId) {
        if (categoryId != null) {
            viewModel.loadCategoryData(categoryId)
        } else {
            viewModel.loadInitialData()
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val itemAddresses by viewModel.itemAddresses.collectAsStateWithLifecycle()

    val locationState by locationViewModel.locationState.collectAsStateWithLifecycle()
    val userLatLng = (locationState as? LocationState.Success)?.location

    when (val s = state) {
        is CategoryState.Loading -> LoadingIndicator()
        is CategoryState.Error   -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(LocalDimens.current.pagePadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lỗi: ${s.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        is CategoryState.Success -> CategoryContent(
            categories = s.categories,
            products = s.products,
            selectedCategory = s.selectedCategory,
            isLoadingProducts = s.isLoadingProducts,
            itemAddresses = itemAddresses,
            userLatLng = userLatLng,
            onCategoryClick = { category -> viewModel.onCategorySelected(category) },
            onProductClick = onProductClick
        )
    }

    ExpandableFab(actions = com.bxt.util.FabActions.rental(navController))
}

@Composable
private fun CategoryContent(
    categories: List<CategoryResponse>,
    products: List<ItemResponse>,
    selectedCategory: CategoryResponse?,
    isLoadingProducts: Boolean,
    itemAddresses: Map<Long, String>,
    userLatLng: Pair<Double, Double>?,
    onCategoryClick: (CategoryResponse) -> Unit,
    onProductClick: (Long) -> Unit
) {
    val d = LocalDimens.current

    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(d.rowGap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.rowGap)
        ) {
            items(items = categories, key = { it.id ?: it.hashCode() }) { category ->
                CategoryCard(
                    category = category,
                    onClick = { onCategoryClick(category) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(2f)
                .padding(vertical = d.rowGap)
                .padding(end = d.rowGap)
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(d.rowGap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(
                targetState = Triple(selectedCategory, isLoadingProducts, products.isEmpty()),
                label = "product_list_state"
            ) { (category, loading, empty) ->
                when {
                    loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    category == null -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(d.pagePadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Vui lòng chọn một danh mục bên trái",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    empty -> {
                        val emptyComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty))
                        val progress by animateLottieCompositionAsState(
                            composition = emptyComposition,
                            iterations = LottieConstants.IterateForever
                        )
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LottieAnimation(
                                    composition = emptyComposition,
                                    progress = { progress },
                                    modifier = Modifier.size(120.dp)
                                )
                                Text(
                                    text = "Chưa có sản phẩm nào",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = d.rowGap),
                            verticalArrangement = Arrangement.spacedBy(d.sectionGap)
                        ) {
                            items(items = products, key = { it.id ?: it.hashCode() }) { product ->
                                val distanceKm: Double? = userLatLng?.let { (uLat, uLng) ->
                                    val iLat = product.lat?.toDouble()
                                    val iLng = product.lng?.toDouble()
                                    if (iLat != null && iLng != null) haversineKm(uLat, uLng, iLat, iLng) else null
                                }

                                val addressShort = extractDistrictOrWard(itemAddresses[product.id])

                                PopularItemCard(
                                    item = product,
                                    locationText = addressShort,
                                    distanceKm = distanceKm,
                                    onClick = { product.id?.let(onProductClick) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
