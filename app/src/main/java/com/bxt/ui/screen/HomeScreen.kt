package com.bxt.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
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
    val isDarkModeEnabledState = viewModel.isDarkModeEnabled.collectAsState()
    val isDarkModeEnabled = isDarkModeEnabledState.value


    val empty by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.empty)
    )
    val progress by animateLottieCompositionAsState(
        empty,
        iterations = LottieConstants.IterateForever
    )

    LocationPermissionHandler(
        onPermissionGranted = {
            locationViewModel.fetchCurrentLocation()
        },
        onPermissionDenied = {}
    )

    val locationState by locationViewModel.locationState.collectAsState()

    val deliveryText = when (val state = locationState) {
        is LocationState.Loading -> "Đang lấy địa chỉ..."
        is LocationState.Success -> state.address ?: "Vị trí hiện tại"
        is LocationState.Error -> "Lỗi: ${state.message}"
        is LocationState.PermissionRequired -> "Cần cấp quyền vị trí"
        is LocationState.GpsDisabled -> "GPS đang tắt"
    }

    val hasAllData = homeState is HomeState.Success &&
            locationState !is LocationState.Loading

    if (!hasAllData) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
        return
    }

    val successHomeState = homeState as HomeState.Success

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    onCheckedChange = { newValue ->
                        viewModel.setDarkModeEnabled(newValue)
                    }
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
                onClick = {
                    locationViewModel.fetchCurrentLocation()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = "Change Location",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
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
                    placeholder = { Text("Search", color = Color(0xFF999999)) },
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
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Card(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { onFilterClick() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
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
                    modifier = Modifier.clickable {
                        onAllCategoriesClick()
                    }
                )

            }
        }



        item {
            if (successHomeState.categories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = empty,
                        progress = { progress },
                        modifier = Modifier.size(90.dp)
                    )
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(successHomeState.categories) { category ->
                        CategoryCard(
                            category = category,
                            onClick = { onCategoryClick(category) }
                        )
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

        if( successHomeState.popularItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = empty,
                        progress = { progress },
                        modifier = Modifier.size(90.dp)
                    )
                }
            }
        }
        else {
            items(successHomeState.popularItems) { item ->
                PopularItemCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
