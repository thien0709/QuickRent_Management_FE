package com.bxt.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bxt.data.api.dto.response.TransactionImageResponse
import com.bxt.viewmodel.*
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBackClick: () -> Unit,
    onNavigateToTransport: (fromLat: Float, fromLng: Float, toLat: Float, toLng: Float) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(ui.error) {
        ui.error?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearError()
        }
    }
    LaunchedEffect(ui.actionSuccess) {
        if (ui.actionSuccess) {
            scope.launch { snackbarHostState.showSnackbar("Thao tác thành công!") }
            viewModel.clearActionSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết yêu cầu thuê") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = ui.state) {
                is DetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is DetailState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) { Text("Đã xảy ra lỗi: ${state.message}") }
                is DetailState.Success -> {
                    TransactionDetailContent(
                        details = state.details,
                        caps = state.caps,
                        isUploading = ui.isUploading,
                        isActionInProgress = ui.isActionInProgress,
                        viewModel = viewModel,
                        onNavigateToTransport = onNavigateToTransport
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailContent(
    details: FullRentalDetails,
    caps: Capabilities,
    isUploading: Boolean,
    isActionInProgress: Boolean,
    viewModel: TransactionDetailViewModel,
    onNavigateToTransport: (fromLat: Float, fromLng: Float, toLat: Float, toLng: Float) -> Unit
) {
    val context = LocalContext.current
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }
    val pickupImages = details.images.filter { it.imageType.equals("PICKUP", true) }
    val returnImages = details.images.filter { it.imageType.equals("RETURN", true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        CurrentStepCard(details.currentStep)

        // --- Info Sections ---
        Section(title = "Sản phẩm") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = details.item.imagePrimary,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(details.item.title ?: "N/A", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    details.item.rentalPricePerHour?.let { Text("Giá thuê: ${nf.format(it)}/giờ") }
                    details.item.depositAmount?.let { Text("Đặt cọc: ${nf.format(it)}") }
                }
            }
        }

        Section(title = "Thông tin giao dịch") {
            details.transaction?.let {
                InfoRow("Mã giao dịch:", it.transactionCode ?: "-")
                InfoRow("Phương thức TT:", it.paymentMethod ?: "-")
                InfoRow("Trạng thái TT:", it.paymentStatus ?: "-")
            }
            InfoRow("Bắt đầu thuê:", formatInstant(details.request.rentalStartTime))
            InfoRow("Kết thúc thuê:", formatInstant(details.request.rentalEndTime))
        }

        Section(title = "Ảnh bàn giao (PICKUP)") { ImageRow(pickupImages) }
        Section(title = "Ảnh trả hàng (RETURN)") { ImageRow(returnImages) }

        // --- Action Sections ---
        if (details.isOwner) {
            OwnerActions(
                caps = caps,
                isActionInProgress = isActionInProgress,
                isUploading = isUploading,
                onConfirm = viewModel::ownerConfirmRequest,
                onReject = viewModel::ownerRejectRequest,
                onComplete = viewModel::ownerCompleteRequest,
                onUpload = { uris -> viewModel.uploadImages(context, uris, "PICKUP") }
            )
        }

        if (details.isRenter) {
            RenterActions(
                details = details,
                caps = caps,
                isUploading = isUploading,
                isActionInProgress = isActionInProgress,
                onConfirmPickup = viewModel::renterConfirmPickup,
                onUploadReturn = { uris -> viewModel.uploadImages(context, uris, "RETURN") },
                onHireTransport = onNavigateToTransport
            )
        }

        if (caps.userCanCancel) {
            Section(title = "Hành động khác") {
                ActionCard(
                    title = "Hủy yêu cầu thuê",
                    description = "Bạn có chắc muốn hủy yêu cầu này không?",
                    isLoading = isActionInProgress
                ) {
                    Button(
                        onClick = viewModel::userCancelRequest,
                        enabled = !isActionInProgress,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Tôi chắc chắn muốn hủy") }
                }
            }
        }
    }
}

// --- Action Components ---

@Composable
private fun OwnerActions(
    caps: Capabilities,
    isActionInProgress: Boolean,
    isUploading: Boolean,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onComplete: () -> Unit,
    onUpload: (List<Uri>) -> Unit
) {
    var ownerPickupUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val ownerPickupPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { ownerPickupUris = it.orEmpty() }

    Section(title = "Hành động của bạn (Chủ sở hữu)") {
        if (caps.ownerCanConfirmOrReject) {
            ActionCard(title = "Xác nhận yêu cầu?", description = "Xác nhận để bắt đầu quy trình cho thuê.", isLoading = isActionInProgress) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReject, enabled = !isActionInProgress) { Text("Từ chối") }
                    Button(onClick = onConfirm, enabled = !isActionInProgress) { Text("Xác nhận") }
                }
            }
        }
        if (caps.ownerCanUploadPickupImages) {
            ImageUploadCard(
                title = "Tải ảnh bàn giao (PICKUP)",
                description = "Chụp ảnh tình trạng sản phẩm TRƯỚC khi cho thuê.",
                uris = ownerPickupUris, isLoading = isUploading,
                onSelectImages = { ownerPickupPicker.launch("image/*") },
                onUpload = { onUpload(ownerPickupUris).also { ownerPickupUris = emptyList() } }
            )
        }
        if (caps.ownerCanComplete) {
            ActionCard(title = "Hoàn tất giao dịch", description = "Xác nhận đã nhận lại hàng và hoàn cọc (nếu có) để kết thúc.", isLoading = isActionInProgress) {
                Button(onClick = onComplete, enabled = !isActionInProgress) { Text("Hoàn tất") }
            }
        }
    }
}

@Composable
private fun RenterActions(
    details: FullRentalDetails,
    caps: Capabilities,
    isUploading: Boolean,
    isActionInProgress: Boolean,
    onConfirmPickup: () -> Unit,
    onUploadReturn: (List<Uri>) -> Unit,
    onHireTransport: (fromLat: Float, fromLng: Float, toLat: Float, toLng: Float) -> Unit
) {
    var renterReturnUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val renterReturnPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { renterReturnUris = it.orEmpty() }
    var pickupChoice by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Section(title = "Hành động của bạn (Người thuê)") {

        if (caps.renterCanChoosePickupOrDelivery) {
            ActionCard(
                title = "Phương thức nhận hàng",
                description = "Sản phẩm đã sẵn sàng. Vui lòng chọn cách bạn muốn nhận hàng.",
                isLoading = isActionInProgress
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            pickupChoice = "SELF"
                            val pickupLat = details.request.latFrom
                            val pickupLng = details.request.lngFrom

                            if (pickupLat != null && pickupLng != null) {
                                val gmmIntentUri =
                                    "google.navigation:q=$pickupLat,$pickupLng".toUri()

                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")

                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, "Vui lòng cài đặt Google Maps để sử dụng tính năng này.", Toast.LENGTH_LONG).show()
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&origin=lat,lng&destination=lat,lng"))
                                    context.startActivity(browserIntent)
                                }
                            } else {
                                Toast.makeText(context, "Không tìm thấy địa chỉ cửa hàng.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (pickupChoice == "SELF") ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Tôi sẽ tự đến lấy")
                    }
                    Button(
                        onClick = {
                            pickupChoice = "DELIVERY"
                            val fromLat = details.request.latFrom?.toFloat()
                            val fromLng = details.request.lngFrom?.toFloat()
                            val toLat = details.request.latTo?.toFloat()
                            val toLng = details.request.lngTo?.toFloat()

                            if (fromLat != null && fromLng != null && toLat != null && toLng != null) {
                                onHireTransport(fromLat, fromLng, toLat, toLng)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Thuê người vận chuyển")
                    }
                }
            }
        }

        if (pickupChoice == "SELF") {
            ActionCard(
                title = "Xác nhận đã nhận hàng",
                description = "Vui lòng kiểm tra kỹ sản phẩm trước khi xác nhận với chủ sở hữu.",
                isLoading = isActionInProgress
            ) {
                Button(onClick = onConfirmPickup, enabled = !isActionInProgress) {
                    Text("Tôi đã nhận hàng")
                }
            }
        }

        if (caps.renterCanUploadReturnImages) {
            ImageUploadCard(
                title = "Tải ảnh trả hàng (RETURN)",
                description = "Chụp ảnh tình trạng sản phẩm TRƯỚC khi trả lại.",
                uris = renterReturnUris,
                isLoading = isUploading,
                onSelectImages = { renterReturnPicker.launch("image/*") },
                onUpload = { onUploadReturn(renterReturnUris).also { renterReturnUris = emptyList() } }
            )
        }
    }
}

// --- UI Helper Components (Không thay đổi) ---

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    isLoading: Boolean,
    buttons: @Composable RowScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(4.dp))
            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    buttons()
                }
            }
        }
    }
}

@Composable
fun ImageUploadCard(
    title: String,
    description: String,
    uris: List<Uri>,
    isLoading: Boolean,
    onSelectImages: () -> Unit,
    onUpload: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            OutlinedButton(onClick = onSelectImages, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                Text(if (uris.isEmpty()) "Chọn ảnh" else "Chọn lại (${uris.size} ảnh)")
            }
            if (uris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(uris) { uri ->
                        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    }
                }
            }
            Button(onClick = onUpload, enabled = uris.isNotEmpty() && !isLoading, modifier = Modifier.fillMaxWidth()) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("Tải ảnh lên")
            }
        }
    }
}

@Composable
private fun CurrentStepCard(currentStep: RentalStep) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Trạng thái hiện tại", style = MaterialTheme.typography.labelMedium)
            Text(currentStep.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(currentStep.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun ImageRow(images: List<TransactionImageResponse>) {
    if (images.isEmpty()) {
        Text("Chưa có hình ảnh.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(images) { img ->
            AsyncImage(model = img.imageUrl, contentDescription = img.imageType, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatInstant(instant: Instant?): String {
    if (instant == null) return "-"
    return runCatching {
        DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy").withLocale(Locale("vi", "VN")).withZone(ZoneId.systemDefault()).format(instant)
    }.getOrDefault("-")
}