package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bxt.data.api.dto.response.ItemDetail
import com.bxt.ui.components.ImagePager
import com.bxt.ui.state.ItemState
import com.bxt.viewmodel.ItemViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(
    itemId: Long,
    onClickBack: () -> Unit,
    onClickOwner: (ownerId: Long) -> Unit,
    onClickRent: (itemId: Long, price: String) -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {
    LaunchedEffect(itemId) {
        viewModel.load(itemId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chi tiết sản phẩm") },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is ItemState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ItemState.Error -> {
                    ErrorStateContent(
                        message = state.message ?: "Đã có lỗi xảy ra",
                        onRetry = { viewModel.load(itemId) }
                    )
                }
                is ItemState.Success -> {
                    if (state.data.item != null) {
                        ItemDetailContent(
                            itemDetail = state.data,
                            onClickOwner = onClickOwner,
                            onClickRent = { price -> onClickRent(itemId, price) }
                        )
                    } else {
                        ErrorStateContent(
                            message = "Không tìm thấy dữ liệu sản phẩm.",
                            onRetry = { viewModel.load(itemId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemDetailContent(
    itemDetail: ItemDetail,
    onClickOwner: (ownerId: Long) -> Unit,
    onClickRent: (price: String) -> Unit
) {
    val item = requireNotNull(itemDetail.item)

    val photos: List<String> = remember(itemDetail) {
        buildList {
            item.imagePrimary?.takeIf { it.isNotBlank() }?.let { add(it) }
            itemDetail.images.forEach { imageUrl ->
                if (imageUrl.isNotBlank() && imageUrl != item.imagePrimary) {
                    add(imageUrl)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ImagePager(
            photos = photos,
            contentDescription = item.title ?: "Hình ảnh sản phẩm"
        )

        Column(Modifier.padding(16.dp)) {
            Text(
                text = item.title.orEmpty(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF)) {
                    Text(
                        text = "Tình trạng: ${item.conditionStatus.orEmpty().ifBlank { "—" }}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF)) {
                    Text(
                        text = "Trạng thái: ${item.availabilityStatus.orEmpty().ifBlank { "—" }}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Giá & Đặt cọc", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Giá theo giờ")
                        Text(money(item.rentalPricePerHour))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tiền đặt cọc")
                        Text(money(item.depositAmount))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(text = item.description.orEmpty(), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        item.ownerId?.let { onClickOwner(it) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = item.ownerId != null
                ) {
                    Text("Chat ngay")
                }

                Button(
                    onClick = {
                        val price = item.rentalPricePerHour?.toPlainString() ?: "0"
                        onClickRent(price)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = item.rentalPricePerHour != null
                ) {
                    Text("Thuê ngay")
                }
            }
        }
    }
}

@Composable
private fun ErrorStateContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Thử lại")
            }
        }
    }
}

@Composable
private fun money(value: BigDecimal?): String {
    val moneyFmt = remember {
        NumberFormat.getCurrencyInstance(Locale("vi", "VN")).apply {
            maximumFractionDigits = 0
        }
    }
    return if (value == null) "—" else runCatching { moneyFmt.format(value) }.getOrDefault("—")
}