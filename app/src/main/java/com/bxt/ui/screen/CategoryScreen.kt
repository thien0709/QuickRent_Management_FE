package com.bxt.ui.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberImagePainter
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.ui.components.CategoryCard
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.components.PopularItemCard
import com.bxt.ui.state.CategoryState
import com.bxt.viewmodel.CategoryViewModel

@Composable
fun CategoryScreen(
    onBackClick : () -> Unit,
    onProductClick: (Long) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    val state = viewModel.state.collectAsState().value

    when (val categoryState = state) {
        is CategoryState.Loading -> {
            LoadingIndicator()
        }
        is CategoryState.Error -> {
            Text(text = "Lỗi: ${categoryState.message}", modifier = Modifier.padding(16.dp))
        }
        is CategoryState.Success -> {
            CategoryContent(
                categories = categoryState.categories,
                products = categoryState.products,
                selectedCategory = categoryState.selectedCategory,
                isLoadingProducts = categoryState.isLoadingProducts,
                onCategoryClick = { category -> viewModel.onCategorySelected(category) },
                onProductClick = onProductClick
            )
        }
    }
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
    Row(modifier = Modifier.fillMaxSize()) {
        // LazyColumn cho danh mục
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(categories) { category ->
                CategoryCard(
                    category = category,
                    onClick = { onCategoryClick(category) }
                )
            }
        }

        // Cột chứa sản phẩm
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(2f)
                .padding(16.dp)
                .background(Color.White)
                .clip(MaterialTheme.shapes.medium)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(targetState = selectedCategory) { category ->
                if (category == null) {
                    Text(
                        text = "Vui lòng chọn một danh mục bên trái",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                } else {
                    Text(
                        text = "Sản phẩm của: ${category.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (isLoadingProducts) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    } else if (products.isEmpty()) {
                        Text(
                            text = "Không có sản phẩm nào trong danh mục này.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = products, key = { it.id ?: 0 }) { product ->
                                PopularItemCard(
                                    item = product,
                                    onClick = {
                                        product.id?.let { id ->
                                            onProductClick(id)
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
