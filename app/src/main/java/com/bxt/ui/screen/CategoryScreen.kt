package com.bxt.ui.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.bxt.util.FabActions
import com.bxt.viewmodel.CategoryViewModel
import com.bxt.ui.theme.LocalDimens

@Composable
fun CategoryScreen(
    categoryId: Long? = null,
    navController: NavController,
    onBackClick: () -> Unit,
    onProductClick: (Long) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    LaunchedEffect(categoryId) {
        if (categoryId != null) {
            viewModel.loadCategoryData(categoryId)
        } else {
            viewModel.loadInitialData()
        }
    }

    val state = viewModel.state.collectAsState().value

    when (val s = state) {
        is CategoryState.Loading -> LoadingIndicator()
        is CategoryState.Error   -> Text(text = "Lỗi: ${s.message}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(LocalDimens.current.pagePadding))
        is CategoryState.Success -> CategoryContent(
            categories = s.categories,
            products = s.products,
            selectedCategory = s.selectedCategory,
            isLoadingProducts = s.isLoadingProducts,
            onCategoryClick = { viewModel.onCategorySelected(it) },
            onProductClick = onProductClick
        )
    }

    ExpandableFab(actions = FabActions.rental(navController))
}

@Composable
fun CategoryContent(
    categories: List<CategoryResponse>,
    products: List<ItemResponse>,
    selectedCategory: CategoryResponse?,
    isLoadingProducts: Boolean,
    onCategoryClick: (CategoryResponse) -> Unit,
    onProductClick: (Long) -> Unit
) {
    val d = LocalDimens.current

    val empty by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty))
    val progress by animateLottieCompositionAsState(empty, iterations = LottieConstants.IterateForever)

    Row(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(d.rowGap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(categories) { category ->
                CategoryCard(
                    category = category,
                    onClick = { onCategoryClick(category) }
                )
                Spacer(modifier = Modifier.height(d.rowGap))
            }
        }

        // Right: products of selected category
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(2f)
                .padding(d.sectionGap)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(d.rowGap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(targetState = selectedCategory) { category ->
                if (category == null) {
                    Text(
                        text = "Vui lòng chọn một danh mục bên trái",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    when {
                        isLoadingProducts -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LoadingIndicator()
                            }
                        }
                        products.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LottieAnimation(composition = empty, progress = { progress })
                            }
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = d.rowGap),
                                verticalArrangement = Arrangement.spacedBy(d.sectionGap)
                            ) {
                                items(items = products, key = { it.id ?: 0 }) { product ->
                                    PopularItemCard(
                                        item = product,
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
}
