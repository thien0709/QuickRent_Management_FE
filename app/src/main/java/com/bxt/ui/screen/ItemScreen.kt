package com.bxt.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bxt.ui.state.ItemState
import com.bxt.viewmodel.ItemViewModel
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(
    itemId: Long,
    onClickBack: () -> Unit,
    onClickOwner: (Long) -> Unit,
    onClickRent: (Long) -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {

    LaunchedEffect(itemId) { viewModel.load(itemId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chi tiết sản phẩm") },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is ItemState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is ItemState.Error -> {
                val msg = (uiState as ItemState.Error).message ?: "Đã có lỗi xảy ra"
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(msg)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.load(itemId) }) { Text("Thử lại") }
                    }
                }
            }

            is ItemState.Success -> {
                val data = (uiState as ItemState.Success).data
                val images = data.images
                val pagerState = rememberPagerState(pageCount = { images.size.coerceAtLeast(1) })
                val nf = remember {
                    NumberFormat.getCurrencyInstance(Locale("vi", "VN")).apply { maximumFractionDigits = 0 }
                }
                val parseIso: (String?) -> String = { s ->
                    if (s.isNullOrBlank()) "—"
                    else runCatching {
                        OffsetDateTime.parse(s) // để parser tự nhận ISO mặc định
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    }.getOrElse { "—" }

                }
                val money: (java.math.BigDecimal?) -> String = { a ->
                    if (a == null) "—" else runCatching { nf.format(a) }.getOrDefault("—")
                }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    // --------- ẢNH ----------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(Color(0xFFF2F2F2))
                    ) {
                        if (images.isNotEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                val url = images[page]
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(url)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = data.item.title ?: "Hình ảnh",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (images.size > 1) {
                                Row(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(images.size) { i ->
                                        val selected = pagerState.currentPage == i
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .size(if (selected) 10.dp else 8.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    if (selected) Color.White
                                                    else Color.White.copy(alpha = 0.5f)
                                                )
                                        )
                                    }
                                }
                            }
                        } else {
                            val primary = data.item.imagePrimary
                            if (!primary.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(primary)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = data.item.title ?: "Hình ảnh",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Không có hình ảnh")
                                }
                            }
                        }
                    }

                    // --------- NỘI DUNG ----------
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = data.item.title.orEmpty(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(8.dp))

                        // Chips đơn giản (inline)
                        Row {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF)) {
                                Text(
                                    text = "Tình trạng: ${data.item.conditionStatus.orEmpty().ifBlank { "—" }}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF)) {
                                Text(
                                    text = "Trạng thái: ${data.item.availabilityStatus.orEmpty().ifBlank { "—" }}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Giá & đặt cọc (inline)
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Giá & Đặt cọc", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Giá theo giờ")
                                    Text(money(data.item.rentalPricePerHour))
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tiền đặt cọc")
                                    Text(money(data.item.depositAmount))
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(text = data.item.description.orEmpty(), style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(16.dp))

                        // Thông tin khác (inline)
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Thông tin khác", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Chủ sở hữu"); Text(data.item.ownerId?.toString() ?: "—")
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Danh mục"); Text(data.item.categoryId?.toString() ?: "—")
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tạo lúc"); Text(parseIso(data.item.createdAt))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cập nhật"); Text(parseIso(data.item.updatedAt))
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = { data.item.id?.let(onClickRent) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Thuê ngay") }
                    }
                }
            }
        }
    }
}
