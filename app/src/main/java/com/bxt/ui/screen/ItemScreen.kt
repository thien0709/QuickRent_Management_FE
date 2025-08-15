// com/bxt/ui/screen/ItemScreen.kt
package com.bxt.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bxt.ui.state.ItemState
import com.bxt.ui.state.RentalState
import com.bxt.viewmodel.ItemViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val rentalState by viewModel.rentalState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Booking state
    var showSheet by remember { mutableStateOf(false) }
    var startAt by remember { mutableStateOf<OffsetDateTime?>(null) }
    var endAt by remember { mutableStateOf<OffsetDateTime?>(null) }
    var address by remember { mutableStateOf("") }
    val userAddress by viewModel.userAddress.collectAsStateWithLifecycle(initialValue = "")

    // Formatters / env
    val moneyFmt = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")).apply { maximumFractionDigits = 0 } }
    val dtFmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }
    val zone = remember { ZoneId.systemDefault() }
    val context = LocalContext.current

    fun money(v: BigDecimal?): String =
        if (v == null) "—" else runCatching { moneyFmt.format(v) }.getOrDefault("—")

    fun parseIso(s: String?): String =
        if (s.isNullOrBlank()) "—"
        else runCatching { OffsetDateTime.parse(s).format(dtFmt) }.getOrElse { "—" }

    // ❗ KHÔNG dùng LocalContext ở đây nữa; truyền Context vào
    fun pickDateTime(
        ctx: Context,
        initial: OffsetDateTime? = null,
        onPicked: (OffsetDateTime) -> Unit
    ) {
        val baseMillis = (initial ?: OffsetDateTime.now()).atZoneSameInstant(zone).toInstant().toEpochMilli()
        val cal = Calendar.getInstance().apply { timeInMillis = baseMillis }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(ctx, { _, yy, mm, dd ->
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)
            TimePickerDialog(ctx, { _, hh, mm2 ->
                val picked = OffsetDateTime.now()
                    .withYear(yy).withMonth(mm + 1).withDayOfMonth(dd)
                    .withHour(hh).withMinute(mm2).withSecond(0).withNano(0)
                onPicked(picked)
            }, hour, min, true).show()
        }, y, m, d).show()
    }

    fun chargeableHours(s: OffsetDateTime, e: OffsetDateTime): Long {
        val mins = Duration.between(s, e).toMinutes().coerceAtLeast(0)
        return max(1L, ceil(mins / 60.0).toLong())
    }

    LaunchedEffect(rentalState) {
        when (val rs = rentalState) {
            is RentalState.Error -> {
                snackbarHostState.showSnackbar(rs.message)
                viewModel.resetRentalState()
            }
            is RentalState.Success -> {
                snackbarHostState.showSnackbar("Đặt thuê thành công")
                viewModel.resetRentalState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chi tiết sản phẩm") },
                navigationIcon = { IconButton(onClick = onClickBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is ItemState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is ItemState.Error -> {
                val msg = state.message ?: "Đã có lỗi xảy ra"
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(msg)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.load(itemId) }) { Text("Thử lại") }
                    }
                }
            }

            is ItemState.Success -> {
                val data = state.data

                // Dùng imagePrimary như list 1 phần tử
                val photos: List<String> = remember(data) {
                    buildList {
                        val p = data.imagePrimary
                        if (!p.isNullOrBlank()) add(p)
                    }
                }
                val pagerState = rememberPagerState(pageCount = { max(1, photos.size) })

                val hourly = data.rentalPricePerHour ?: BigDecimal.ZERO
                val canCalc = startAt != null && endAt != null && endAt!!.isAfter(startAt!!)
                val hours = if (canCalc) chargeableHours(startAt!!, endAt!!) else 0L
                val total = if (canCalc) hourly.multiply(BigDecimal.valueOf(hours)) else BigDecimal.ZERO

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Images
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(Color(0xFFF2F2F2))
                    ) {
                        if (photos.isNotEmpty()) {
                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photos[page])
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = data.title ?: "Hình ảnh",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (photos.size > 1) {
                                Row(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(photos.size) { i ->
                                        val selected = pagerState.currentPage == i
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .size(if (selected) 10.dp else 8.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    if (selected) Color.White else Color.White.copy(alpha = 0.5f)
                                                )
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Không có hình ảnh")
                            }
                        }
                    }

                    // Info
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
                                    Text("Giá theo giờ"); Text(money(hourly))
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
                                val now = OffsetDateTime.now().withSecond(0).withNano(0).plusMinutes(30)
                                if (startAt == null) startAt = now
                                if (endAt == null) endAt = now.plusHours(1)
                                if (address.isBlank()) address = userAddress
                                showSheet = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Thuê ngay") }
                    }
                }

                // Booking sheet
                if (showSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showSheet = false },
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        val canSubmit = startAt != null &&
                                endAt != null &&
                                endAt!!.isAfter(startAt!!) &&
                                address.isNotBlank()

                        val hoursLabel = if (startAt != null && endAt != null && endAt!!.isAfter(startAt!!)) {
                            "${chargeableHours(startAt!!, endAt!!)} giờ"
                        } else "—"

                        val totalLabel = if (startAt != null && endAt != null && endAt!!.isAfter(startAt!!)) {
                            val h = chargeableHours(startAt!!, endAt!!)
                            money(hourly.multiply(BigDecimal.valueOf(h)))
                        } else "—"

                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Đặt lịch thuê", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                            OutlinedButton(
                                onClick = {
                                    pickDateTime(context, startAt) { picked ->
                                        startAt = picked
                                        if (endAt == null || !endAt!!.isAfter(picked)) endAt = picked.plusHours(1)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Bắt đầu: " + (startAt?.format(dtFmt) ?: "Chọn thời gian")) }

                            OutlinedButton(
                                onClick = { pickDateTime(context, endAt) { picked -> endAt = picked } },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Kết thúc: " + (endAt?.format(dtFmt) ?: "Chọn thời gian")) }

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Địa chỉ nhận/trả (tự điền từ hồ sơ)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                maxLines = 3
                            )

                            Divider()

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Số giờ tính phí")
                                Text(hoursLabel)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tổng tiền")
                                Text(totalLabel, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val s = startAt ?: return@Button
                                    val e = endAt ?: return@Button
                                    val h = chargeableHours(s, e)
                                    val totalToSubmit = hourly.multiply(BigDecimal.valueOf(h))
                                    viewModel.createRentalRequest(
                                        itemId = itemId,
                                        startAt = s.toInstant(),
                                        endAt = e.toInstant(),
                                        totalPrice = totalToSubmit
                                    )
                                    showSheet = false
                                },
                                enabled = canSubmit && rentalState !is RentalState.Submitting,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Xác nhận thuê") }

                            if (rentalState is RentalState.Submitting) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
