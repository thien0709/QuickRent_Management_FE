package com.bxt.ui.screen

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bxt.data.api.dto.response.ItemDetail
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.ui.components.ImagePager
import com.bxt.ui.state.ItemState
import com.bxt.ui.theme.LocalDimens
import com.bxt.viewmodel.ItemViewModel
import com.google.gson.Gson
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(
    itemId: Long,
    navController: NavController,
    onClickBack: () -> Unit,
    onClickRent: (itemId: Long, price: String) -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {
    LaunchedEffect(itemId) { viewModel.load(itemId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chi tiết sản phẩm", style = MaterialTheme.typography.titleSmall) },
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    val detail = state.data
                    if (detail.item != null) {
                        ItemDetailContent(
                            itemDetail = detail,
                            navController = navController,
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
    navController: NavController,
    onClickRent: (price: String) -> Unit
) {
    val d = LocalDimens.current
    val item = requireNotNull(itemDetail.item)

    val photos: List<String> = remember(itemDetail) {
        buildList {
            item.imagePrimary?.takeIf { it.isNotBlank() }?.let { add(it) }
            itemDetail.images.forEach { url ->
                if (url.isNotBlank() && url != item.imagePrimary) add(url)
            }
        }
    }

    // Hàm để navigate tới chat với thông tin item
    val navigateToChat = remember(item) {
        {
            if (item.ownerId != null) {
                // Tạo thông tin attachable để truyền vào chat
                val attachableInfo = mapOf(
                    "type" to "Item",
                    "title" to (item.title ?: "Sản phẩm"),
                    "subtitle" to (item.description?.take(100) ?: ""),
                    "image" to (item.imagePrimary ?: ""),
                    "price" to money(item.rentalPricePerHour),
                    "itemId" to item.id.toString(),
                    "condition" to (item.conditionStatus ?: ""),
                    "availability" to (item.availabilityStatus ?: "")
                )

                val attachableJson = Gson().toJson(attachableInfo)
                val encodedJson = Uri.encode(attachableJson)

                // Navigate tới chat với thông tin item đính kèm
                navController.navigate("chat_screen/${item.ownerId}?attachableJson=$encodedJson")
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

        Column(Modifier.padding(d.pagePadding)) {
            // Title
            Text(
                text = item.title.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(d.rowGap))

            // Chips trạng thái
            Row(horizontalArrangement = Arrangement.spacedBy(d.rowGap)) {
                AssistChipBox(
                    text = "Tình trạng: ${item.conditionStatus.orEmpty().ifBlank { "—" }}",
                )
                AssistChipBox(
                    text = "Trạng thái: ${item.availabilityStatus.orEmpty().ifBlank { "—" }}",
                )
            }

            Spacer(Modifier.height(d.sectionGap))

            // Card giá
            Card(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(d.pagePadding)) {
                    Text(
                        "Giá & Đặt cọc",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(d.rowGap))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Giá theo giờ", style = MaterialTheme.typography.bodySmall)
                        Text(money(item.rentalPricePerHour), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height((d.rowGap - 2.dp).coerceAtLeast(2.dp)))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tiền đặt cọc", style = MaterialTheme.typography.bodySmall)
                        Text(money(item.depositAmount), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(d.sectionGap))

            // Mô tả
            Text(
                text = item.description.orEmpty(),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(d.sectionGap + 4.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.rowGap)
            ) {
                OutlinedButton(
                    onClick = navigateToChat,
                    modifier = Modifier
                        .weight(1f)
                        .height(d.buttonHeight),
                    shape = MaterialTheme.shapes.medium,
                    enabled = item.ownerId != null
                ) {
                    Icon(
                        Icons.Default.Message,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Chat ngay", style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        val price = item.rentalPricePerHour?.toPlainString() ?: "0"
                        onClickRent(price)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(d.buttonHeight),
                    shape = MaterialTheme.shapes.medium,
                    enabled = item.rentalPricePerHour != null
                ) {
                    Text("Thuê ngay", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (item.ownerId != null) {
                Spacer(Modifier.height(d.sectionGap))

                Card(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                }
            }
        }
    }
}

@Composable
private fun AssistChipBox(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ErrorStateContent(message: String, onRetry: () -> Unit) {
    val d = LocalDimens.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(d.rowGap))
            Button(onClick = onRetry, shape = MaterialTheme.shapes.medium) {
                Text("Thử lại", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun money(value: BigDecimal?): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).apply {
        maximumFractionDigits = 0
    }
    return if (value == null) "—" else runCatching { fmt.format(value) }.getOrDefault("—")
}