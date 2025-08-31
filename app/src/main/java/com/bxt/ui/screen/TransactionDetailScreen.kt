package com.bxt.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bxt.data.api.dto.response.TransactionImageResponse
import com.bxt.viewmodel.Capabilities
import com.bxt.viewmodel.FullTransactionDetails
import com.bxt.viewmodel.RentalStep
import com.bxt.viewmodel.TransactionDetailState
import com.bxt.viewmodel.TransactionDetailViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBackClick: () -> Unit,
    onUploadSuccess: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(ui.error) {
        ui.error?.let {
            scope.launch { snackbar.showSnackbar(it) }
            viewModel.clearError()
        }
    }
    LaunchedEffect(ui.uploadSuccess) {
        if (ui.uploadSuccess) {
            scope.launch { snackbar.showSnackbar("Tải ảnh thành công!") }
            onUploadSuccess()
            viewModel.clearUploadSuccess()
        }
    }
    LaunchedEffect(ui.actionSuccess) {
        if (ui.actionSuccess) {
            scope.launch { snackbar.showSnackbar("Thao tác thành công!") }
            viewModel.clearActionSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết giao dịch") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = ui.state) {
                is TransactionDetailState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is TransactionDetailState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Lỗi: ${s.message}")
                    }
                }
                is TransactionDetailState.Success -> {
                    TransactionDetailContent(
                        details = s.details,
                        caps = s.caps,
                        isUploading = ui.isUploading,
                        isActionInProgress = ui.isActionInProgress,
                        onOwnerConfirmRequest = { viewModel.ownerConfirmRequest() },
                        onOwnerConfirmCash = { viewModel.ownerConfirmCashReceived() },
                        onOwnerComplete = { viewModel.ownerComplete() },
                        onRenterConfirmPickup = { viewModel.renterConfirmPickup() },
                        onUpload = { ctx, type, uris -> viewModel.uploadImages(ctx, uris, type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailContent(
    details: FullTransactionDetails,
    caps: Capabilities,
    isUploading: Boolean,
    isActionInProgress: Boolean,
    onOwnerConfirmRequest: () -> Unit,
    onOwnerConfirmCash: () -> Unit,
    onOwnerComplete: () -> Unit,
    onRenterConfirmPickup: () -> Unit,
    onUpload: (Context, String, List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    var ownerPickupUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val ownerPickupPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        ownerPickupUris = it.orEmpty()
    }

    var renterReturnUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val renterReturnPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        renterReturnUris = it.orEmpty()
    }

    val pickupImages = details.images.filter { it.imageType.equals("PICKUP", true) }
    val returnImages = details.images.filter { it.imageType.equals("RETURN", true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Trạng thái hiện tại
        if (caps.showCurrentStep) {
            CurrentStepCard(details.currentStep)
        }

        // Timeline của quy trình
        RentalTimelineCard(details.currentStep)

        // Sản phẩm
        Text("Sản phẩm", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = details.item.imagePrimary,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(details.item.title ?: "N/A", fontWeight = FontWeight.SemiBold)
                details.item.rentalPricePerHour?.let { Text("Giá thuê: ${nf.format(it)}/giờ") }
                details.item.depositAmount?.let { Text("Đặt cọc: ${nf.format(it)}") }
            }
        }

        // Thông tin giao dịch
        Text("Thông tin giao dịch", style = MaterialTheme.typography.titleMedium)
        InfoRow("Mã giao dịch:", details.transaction.transactionCode ?: "-")
        InfoRow("Trạng thái yêu cầu:", details.request.status ?: "-")
        InfoRow("Phương thức thanh toán:", details.transaction.paymentMethod ?: "-")
        InfoRow("Trạng thái thanh toán:", details.transaction.paymentStatus ?: "-")
        InfoRow("Bắt đầu thuê:", formatInstant(details.request.rentalStartTime))
        InfoRow("Kết thúc thuê:", formatInstant(details.request.rentalEndTime))

        if (caps.showBankingPendingHint) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Đang chờ xác nhận từ cổng thanh toán…")
            }
        }

        Divider()

        // Ảnh
        Text("Ảnh bàn giao (PICKUP)", style = MaterialTheme.typography.titleMedium)
        ImageRow(pickupImages)
        Text("Ảnh trả hàng (RETURN)", style = MaterialTheme.typography.titleMedium)
        ImageRow(returnImages)

        Divider()

        // Hành động của CHỦ CHO THUÊ
        if (details.isOwner) {
            Text("Hành động (Chủ cho thuê)", style = MaterialTheme.typography.titleMedium)

            // Xác nhận yêu cầu thuê
            if (caps.ownerCanConfirmRequest) {
                Button(
                    onClick = onOwnerConfirmRequest,
                    enabled = !isActionInProgress,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isActionInProgress) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Xác nhận yêu cầu thuê")
                }
                Text(
                    "Xác nhận để bắt đầu quy trình cho thuê.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Upload ảnh PICKUP
            if (caps.ownerCanUploadPickup) {
                OutlinedButton(
                    onClick = { ownerPickupPicker.launch("image/*") },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Chọn ảnh PICKUP (${ownerPickupUris.size})") }
                if (ownerPickupUris.isNotEmpty()) PreviewRow(ownerPickupUris)

                Button(
                    onClick = { onUpload(context, "PICKUP", ownerPickupUris) },
                    enabled = ownerPickupUris.isNotEmpty() && !isUploading,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isUploading) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Tải ảnh PICKUP")
                }
                Text(
                    "Chụp ảnh tình trạng sản phẩm TRƯỚC khi cho thuê.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Xác nhận thanh toán tiền mặt
            if (caps.ownerCanConfirmCashPaid) {
                Button(
                    onClick = onOwnerConfirmCash,
                    enabled = !isActionInProgress,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isActionInProgress) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Xác nhận đã nhận tiền (CASH)")
                }
                Text(
                    "Xác nhận khi người thuê đã thanh toán tiền mặt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Hoàn tất giao dịch
            if (caps.ownerCanComplete) {
                var verified by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = verified, onCheckedChange = { verified = it })
                    Text("Đã nhận lại hàng & hoàn cọc (nếu có)")
                }
                Button(
                    onClick = onOwnerComplete,
                    enabled = verified && !isActionInProgress,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isActionInProgress) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Hoàn tất giao dịch")
                }
                Text(
                    "Xác nhận đã nhận lại hàng từ người thuê và hoàn cọc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Hành động của NGƯỜI THUÊ
        if (details.isRenter) {
            Text("Hành động (Người thuê)", style = MaterialTheme.typography.titleMedium)

            // Thanh toán (chỉ hiển thị nếu cần)
            if (caps.renterCanMakePayment) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Cần thanh toán",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Vui lòng thanh toán theo phương thức đã chọn để nhận hàng.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Chỉ đường tới điểm nhận hàng
            if (caps.renterCanOpenPickupMap) {
                Button(
                    onClick = {
                        details.pickupLocation?.let { openMapsForNavigation(context, it.lat, it.lng, it.label) }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Chỉ đường tới điểm nhận hàng") }
            }

            // Xác nhận đã nhận hàng
            if (caps.renterCanConfirmPickup) {
                Button(
                    onClick = onRenterConfirmPickup,
                    enabled = !isActionInProgress,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isActionInProgress) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Xác nhận đã nhận hàng")
                }
                Text(
                    "Xác nhận khi bạn đã nhận được hàng từ chủ cho thuê.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Upload ảnh RETURN
            if (caps.renterCanUploadReturn) {
                OutlinedButton(
                    onClick = { renterReturnPicker.launch("image/*") },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Chọn ảnh RETURN (${renterReturnUris.size})") }
                if (renterReturnUris.isNotEmpty()) PreviewRow(renterReturnUris)

                Button(
                    onClick = { onUpload(context, "RETURN", renterReturnUris) },
                    enabled = renterReturnUris.isNotEmpty() && !isUploading,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isUploading) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Tải ảnh RETURN")
                }
                Text(
                    "Chụp ảnh tình trạng sản phẩm TRƯỚC khi trả lại cho chủ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun CurrentStepCard(currentStep: RentalStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Trạng thái hiện tại",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                currentStep.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                currentStep.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RentalTimelineCard(currentStep: RentalStep) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Quy trình thuê đồ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            val steps = listOf(
                RentalStep.REQUEST_CREATED,
                RentalStep.OWNER_CONFIRMED,
                RentalStep.PICKUP_IMAGES_UPLOADED,
                RentalStep.PAYMENT_COMPLETED,
                RentalStep.ITEM_PICKED_UP,
                RentalStep.RENTAL_DUE,
                RentalStep.RETURN_IMAGES_UPLOADED,
                RentalStep.COMPLETED
            )

            steps.forEachIndexed { index, step ->
                TimelineItem(
                    step = step,
                    isCompleted = step.ordinal <= currentStep.ordinal,
                    isCurrent = step == currentStep,
                    isLast = index == steps.size - 1
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    step: RentalStep,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                }
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = step.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
            if (isCurrent) {
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(if (isLast) 0.dp else 8.dp))
        }
    }
}

@Composable
private fun PreviewRow(uris: List<Uri>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
        items(uris) { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ImageRow(images: List<TransactionImageResponse>) {
    if (images.isEmpty()) {
        Text("Chưa có hình ảnh.", style = MaterialTheme.typography.bodySmall)
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(images) { img ->
            AsyncImage(
                model = img.imageUrl,
                contentDescription = img.imageType,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(value)
    }
}

private fun formatInstant(instant: Instant?): String {
    if (instant == null) return "-"
    return runCatching {
        DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy")
            .withLocale(Locale("vi", "VN"))
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }.getOrDefault("-")
}

private fun openMapsForNavigation(context: Context, lat: BigDecimal, lng: BigDecimal, label: String?) {
    val gmaps = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng"))
        .setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(gmaps); return
    } catch (_: ActivityNotFoundException) {}

    val labelEnc = Uri.encode(label ?: "Điểm nhận hàng")
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng($labelEnc)"))
    try {
        context.startActivity(geo); return
    } catch (_: ActivityNotFoundException) {}

    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving"))
    context.startActivity(web)
}