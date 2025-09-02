// File: ui/screen/SearchScreen.kt
package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.ui.components.PopularItemCard
import com.bxt.viewmodel.SearchItemViewModel

// NEW: dùng LocationViewModel + state để tính khoảng cách
import com.bxt.viewmodel.LocationViewModel
import com.bxt.ui.state.LocationState
import com.bxt.util.haversineKm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchItemScreen(
    onNavigateBack: () -> Unit,
    onItemClick: (ItemResponse) -> Unit,
    viewModel: SearchItemViewModel = hiltViewModel(),
    locationViewModel: LocationViewModel = hiltViewModel() // NEW
) {
    val searchText by viewModel.searchText.collectAsState()
    val searchedItems by viewModel.searchedItems.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val addresses by viewModel.itemAddresses.collectAsState()

    // Lấy vị trí hiện tại (nếu đã có)
    val locationState by locationViewModel.locationState.collectAsState()
    val userLatLng = (locationState as? LocationState.Success)?.location // Pair<Double, Double> (lat, lng)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchText,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text("Tìm kiếm sản phẩm...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchedItems, key = { it.id ?: it.hashCode() }) { item ->
                        // Tính khoảng cách theo km nếu có đủ dữ liệu
                        val distanceKm: Double? = userLatLng?.let { (uLat, uLng) ->
                            val iLat = item.lat?.toDouble()
                            val iLng = item.lng?.toDouble()
                            if (iLat != null && iLng != null) haversineKm(uLat, uLng, iLat, iLng) else null
                        }

                        PopularItemCard(
                            item = item,
                            locationText = addresses[item.id], // địa chỉ hiển thị RIÊNG
                            distanceKm = distanceKm,          // khoảng cách hiển thị RIÊNG
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}
