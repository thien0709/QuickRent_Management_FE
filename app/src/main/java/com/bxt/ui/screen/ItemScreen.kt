package com.bxt.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
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
import com.bxt.ui.components.ImagePager
import com.bxt.ui.state.ItemState
import com.bxt.viewmodel.ItemViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(
    itemId: Long,
    onClickBack: () -> Unit,
    onClickOwner: (Long) -> Unit,
    onClickRent: (itemId: Long, price: String) -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {
    LaunchedEffect(itemId) { viewModel.load(itemId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val moneyFmt = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")).apply { maximumFractionDigits = 0 } }
    fun money(v: BigDecimal?): String =
        if (v == null) "—" else runCatching { moneyFmt.format(v) }.getOrDefault("—")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chi tiết sản phẩm") },
                navigationIcon = { IconButton(onClick = onClickBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is ItemState.Loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is ItemState.Error -> {
                val msg = state.message ?: "Đã có lỗi xảy ra"
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(msg)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.load(itemId) }) { Text("Thử lại") }
                    }
                }
            }

            is ItemState.Success -> {
                val data = state.data
                val photos: List<String> = remember(data) {
                    buildList {
                        val p = data.imagePrimary
                        if (!p.isNullOrBlank()) add(p)
                    }
                }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    ImagePager(
                        photos = photos,
                        contentDescription = data.title ?: "Hình ảnh sản phẩm"
                    )

                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = data.title.orEmpty(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(8.dp))

                        Row {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF)) {
                                Text(
                                    text = "Tình trạng: ${data.conditionStatus.orEmpty().ifBlank { "—" }}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF)) {
                                Text(
                                    text = "Trạng thái: ${data.availabilityStatus.orEmpty().ifBlank { "—" }}",
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
                                    Text("Giá theo giờ"); Text(money(data.rentalPricePerHour))
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tiền đặt cọc"); Text(money(data.depositAmount))
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(text = data.description.orEmpty(), style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val price = data.rentalPricePerHour?.toPlainString() ?: "0"
                                onClickRent(itemId, price)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = data.rentalPricePerHour != null
                        ) { Text("Thuê ngay") }
                    }
                }
            }
        }
    }
}