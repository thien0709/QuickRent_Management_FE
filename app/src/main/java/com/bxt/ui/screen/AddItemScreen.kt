package com.bxt.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.ui.state.AddItemState
import com.bxt.viewmodel.AddItemViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel = hiltViewModel(),
    onItemAdded: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var deposit by rememberSaveable { mutableStateOf("") }
    var rentalPrice by rememberSaveable { mutableStateOf("") }
    var conditionStatus by rememberSaveable { mutableStateOf("") }
    var availabilityStatus by rememberSaveable { mutableStateOf("") }
    var isActive by rememberSaveable { mutableStateOf(true) }
    var categoryIdText by rememberSaveable { mutableStateOf("") }

    var images by rememberSaveable(
        stateSaver = listSaver(
            save = { it.map(Uri::toString) },
            restore = { it.map(Uri::parse) }
        )
    ) { mutableStateOf(emptyList()) }

    val uiState by viewModel.uiState.collectAsState()

    val userId by viewModel.userId.collectAsState()

    if (userId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (!uris.isNullOrEmpty()) images = images + uris }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AddItemState.Error -> snackbarHostState.showSnackbar(s.message)
            is AddItemState.Success -> {
                if (!s.warning.isNullOrBlank()) snackbarHostState.showSnackbar(s.warning!!)
                onItemAdded()
            }
            else -> Unit
        }
    }

    val isBusy = uiState is AddItemState.Submitting || uiState is AddItemState.Uploading

    Scaffold(
        topBar = { TopAppBar(title = { Text("Thêm sản phẩm mới") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(
//                modifier = Modifier.padding(16.dp).fillMaxSize(),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Tiêu đề *") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Mô tả") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deposit, onValueChange = { deposit = it.filter(Char::isDigit) },
                    label = { Text("Tiền cọc") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rentalPrice, onValueChange = { rentalPrice = it.filter(Char::isDigit) },
                    label = { Text("Giá thuê / giờ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = conditionStatus, onValueChange = { conditionStatus = it },
                    label = { Text("Tình trạng") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = availabilityStatus, onValueChange = { availabilityStatus = it },
                    label = { Text("Tình trạng sẵn có") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoryIdText,
                    onValueChange = { categoryIdText = it.filter(Char::isDigit) },
                    label = { Text("Category ID *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kích hoạt:")
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                if (images.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(images) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable(enabled = !isBusy) { images = images - uri }
                            )
                        }
                    }
                }

                Button(
                    onClick = { pickImagesLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isBusy
                ) { Text("Chọn ảnh") }

                Button(
                    onClick = {
                        val categoryId = categoryIdText.toLongOrNull()
                        if (title.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Thiếu tiêu đề") }
                            return@Button
                        }
                        if (categoryId == null) {
                            scope.launch { snackbarHostState.showSnackbar("Thiếu Category ID") }
                            return@Button
                        }

                        val req = ItemRequest(
                            ownerId = userId!!,
                            categoryId = categoryId,
                            title = title.trim(),
                            description = description,
                            depositAmount = deposit.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            rentalPricePerHour = rentalPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            conditionStatus = conditionStatus,
                            availabilityStatus = availabilityStatus,
                            isActive = isActive
                        )
                        viewModel.addItem(context, req, images)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isBusy
                ) {
                    Text(
                        when (val s = uiState) {
                            is AddItemState.Submitting -> "Đang tạo sản phẩm..."
                            is AddItemState.Uploading -> "Đang chuẩn bị ảnh ${s.uploaded}/${s.total}..."
                            else -> "Thêm sản phẩm"
                        }
                    )
                }

                if (uiState is AddItemState.Uploading) {
                    val u = uiState as AddItemState.Uploading
                    val progress = if (u.total == 0) 1f else u.uploaded.toFloat() / max(1, u.total).toFloat()
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp))
                    Text("Đã chuẩn bị ảnh ${u.uploaded}/${u.total}", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (uiState is AddItemState.Submitting) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
