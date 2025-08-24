// file: com/bxt/ui/screen/TransportServiceScreen.kt

package com.bxt.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bxt.ui.components.ExpandableFab
import com.bxt.ui.components.TransportServiceCard
import com.bxt.ui.state.TransportServiceListState
import com.bxt.util.FabActions
import com.bxt.viewmodel.TransportServiceViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun TransportServiceScreen(
    navController: NavController,
    viewModel: TransportServiceViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }



    // Thay thế Scaffold bằng Column
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tạo TopAppBar thủ công
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = "Dịch vụ vận chuyển",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
            }
        }

        // Nội dung chính
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            val isRefreshing = uiState is TransportServiceListState.Loading

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadTransportServices() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is TransportServiceListState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is TransportServiceListState.Success -> {
                        if (state.services.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        "Không có dịch vụ nào.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            // Hiển thị danh sách ở nửa dưới màn hình để không che hết bản đồ
                            LazyColumn(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.6f),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.services, key = { it.id!! }) { service ->
                                    TransportServiceCard(
                                        service = service,
                                        onDelete = { serviceId ->
                                            viewModel.deleteTransportService(serviceId)
                                        },
                                        onClick = {
                                            // Optional: Navigate to detail screen
                                            // navController.navigate("transport_detail/${service.id}")
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is TransportServiceListState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(state.message, color = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { viewModel.loadTransportServices() }) { Text("Thử lại") }
                                }
                            }
                        }
                    }
                }

            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        ExpandableFab(
            actions = FabActions.transport(navController)
        )
    }
}