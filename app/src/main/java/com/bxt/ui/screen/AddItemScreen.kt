package com.bxt.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.api.dto.status.AvailabilityStatus
import com.bxt.data.api.dto.status.ConditionStatus
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.state.AddItemState
import com.bxt.ui.state.CategoriesUiState
import com.bxt.ui.theme.LocalDimens
import com.bxt.viewmodel.AddItemViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel = hiltViewModel(),
    onItemAdded: () -> Unit,
    onUserNull: () -> Unit
) {
    val d = LocalDimens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Form states ---
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var deposit by rememberSaveable { mutableStateOf("") }
    var rentalPrice by rememberSaveable { mutableStateOf("") }
    var isActive by rememberSaveable { mutableStateOf(true) }

    // --- Enum dropdown states ---
    var conditionExpanded by rememberSaveable { mutableStateOf(false) }
    var availabilityExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedCondition by rememberSaveable { mutableStateOf<ConditionStatus?>(null) }
    var selectedAvailability by rememberSaveable { mutableStateOf<AvailabilityStatus?>(null) }
    val conditionOptions = remember { ConditionStatus.values().toList() }
    val availabilityOptions = remember { AvailabilityStatus.values().toList() }

    // --- Category picker states ---
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    var categorySearch by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedCategoryName by rememberSaveable { mutableStateOf("") }

    // --- Images ---
    var images by rememberSaveable(
        stateSaver = listSaver(
            save = { it.map(Uri::toString) },
            restore = { it.map(Uri::parse) }
        )
    ) { mutableStateOf(emptyList()) }

    val uiState by viewModel.uiState.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val isLoadingUser by viewModel.isUserLoading.collectAsState()
    val categoriesState by viewModel.categoriesState.collectAsState()

    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) images = images + uris.take(3 - images.size)
    }

    // Auth state
    if (isLoadingUser) {
        LoadingIndicator()
        return
    }
    if (userId == 0L) {
        LaunchedEffect(Unit) { onUserNull() }
        return
    }

    // React to submit result
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AddItemState.Error -> snackbarHostState.showSnackbar(state.message, withDismissAction = true)
            is AddItemState.Success -> {
                state.warning?.let { snackbarHostState.showSnackbar(it, withDismissAction = true) }
                onItemAdded()
            }
            else -> Unit
        }
    }

    val isBusy = uiState is AddItemState.Submitting || uiState is AddItemState.Uploading

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(d.pagePadding)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(d.sectionGap)
            ) {
                // --- Product info ---
                Text("Product info", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *", style = MaterialTheme.typography.labelSmall) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = d.fieldMinHeight)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", style = MaterialTheme.typography.labelSmall) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = d.fieldMinHeight * 1.6f)
                )

                // --- Pricing ---
                Text("Pricing", style = MaterialTheme.typography.titleSmall)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.rowGap)) {
                    OutlinedTextField(
                        value = deposit,
                        onValueChange = { deposit = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Deposit", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = d.fieldMinHeight)
                    )
                    OutlinedTextField(
                        value = rentalPrice,
                        onValueChange = { rentalPrice = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Hourly rental price", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = d.fieldMinHeight)
                    )
                }

                // --- Status ---
                Text("Status", style = MaterialTheme.typography.titleSmall)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.rowGap)) {
                    // Condition
                    ExposedDropdownMenuBox(
                        expanded = conditionExpanded,
                        onExpandedChange = { conditionExpanded = !conditionExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCondition?.label ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Condition", style = MaterialTheme.typography.labelSmall) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .heightIn(min = d.fieldMinHeight)
                        )
                        ExposedDropdownMenu(
                            expanded = conditionExpanded,
                            onDismissRequest = { conditionExpanded = false }
                        ) {
                            conditionOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label, style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        selectedCondition = option
                                        conditionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Availability
                    ExposedDropdownMenuBox(
                        expanded = availabilityExpanded,
                        onExpandedChange = { availabilityExpanded = !availabilityExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedAvailability?.label ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Availability", style = MaterialTheme.typography.labelSmall) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = availabilityExpanded) },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .heightIn(min = d.fieldMinHeight)
                        )
                        ExposedDropdownMenu(
                            expanded = availabilityExpanded,
                            onDismissRequest = { availabilityExpanded = false }
                        ) {
                            availabilityOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label, style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        selectedAvailability = option
                                        availabilityExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // --- Category ---
                Text("Category", style = MaterialTheme.typography.titleSmall)

                when (val cs = categoriesState) {
                    is CategoriesUiState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(d.progressSmall), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Loading categories…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is CategoriesUiState.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cs.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(
                                onClick = { viewModel.loadCategories() },
                                modifier = Modifier.height(d.smallButtonHeight),
                                shape = MaterialTheme.shapes.medium
                            ) { Text("Retry", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                    is CategoriesUiState.Success -> {
                        val all = cs.categories
                        val filtered = remember(categorySearch, all) {
                            if (categorySearch.isBlank()) all
                            else all.filter { it.name!!.contains(categorySearch, ignoreCase = true) }
                        }

                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedCategoryName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select category", style = MaterialTheme.typography.labelSmall) },
                                textStyle = MaterialTheme.typography.bodySmall,
                                placeholder = { Text("Pick a category from the list", style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .heightIn(min = d.fieldMinHeight)
                            )

                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                OutlinedTextField(
                                    value = categorySearch,
                                    onValueChange = { categorySearch = it },
                                    label = { Text("Search categories…", style = MaterialTheme.typography.labelSmall) },
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(d.rowGap)
                                        .heightIn(min = d.fieldMinHeight)
                                )

                                if (filtered.isEmpty()) {
                                    DropdownMenuItem(text = { Text("No results", style = MaterialTheme.typography.bodySmall) }, onClick = { })
                                } else {
                                    filtered.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name ?: "", style = MaterialTheme.typography.bodySmall) },
                                            onClick = {
                                                selectedCategoryId = cat.id
                                                selectedCategoryName = cat.name.orEmpty()
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Active switch ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Active", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                // --- Images ---
                Text("Images", style = MaterialTheme.typography.titleSmall)

                if (images.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(d.rowGap), modifier = Modifier.fillMaxWidth()) {
                        items(images) { uri ->
                            Box(Modifier.size(d.imageSize)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(d.imageSize)
                                        .clip(MaterialTheme.shapes.medium)
                                )
                                IconButton(
                                    onClick = { images = images - uri },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                        .size(d.iconSmall)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove image", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { pickImagesLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(d.smallButtonHeight),
                    enabled = !isBusy && images.size < 3,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Add images (${images.size}/3)", style = MaterialTheme.typography.bodySmall)
                }

                // --- Submit ---
                Button(
                    onClick = {
                        when {
                            title.isBlank() ->
                                scope.launch { snackbarHostState.showSnackbar("Please enter a title", withDismissAction = true) }
                            selectedCategoryId == null ->
                                scope.launch { snackbarHostState.showSnackbar("Please select a category", withDismissAction = true) }
                            selectedCondition == null ->
                                scope.launch { snackbarHostState.showSnackbar("Please select a condition", withDismissAction = true) }
                            selectedAvailability == null ->
                                scope.launch { snackbarHostState.showSnackbar("Please select availability", withDismissAction = true) }
                            images.isEmpty() ->
                                scope.launch { snackbarHostState.showSnackbar("Please add at least one image", withDismissAction = true) }
                            else -> {
                                val req = userId?.let {
                                    ItemRequest(
                                        ownerId = it,
                                        categoryId = selectedCategoryId!!,
                                        title = title.trim(),
                                        description = description,
                                        depositAmount = deposit.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                                        rentalPricePerHour = rentalPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                                        conditionStatus = selectedCondition!!.name,
                                        availabilityStatus = selectedAvailability!!.name,
                                        isActive = isActive
                                    )
                                }
                                if (req != null) viewModel.addItem(context, req, images)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(d.buttonHeight),
                    enabled = !isBusy,
                    shape = MaterialTheme.shapes.medium
                ) {
                    when (val state = uiState) {
                        is AddItemState.Submitting -> Text("Creating item…", style = MaterialTheme.typography.bodySmall)
                        is AddItemState.Uploading -> Text("Uploading images (${state.uploaded}/${state.total})", style = MaterialTheme.typography.bodySmall)
                        else -> Text("Create item", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // --- Upload progress ---
                if (uiState is AddItemState.Uploading) {
                    val state = uiState as AddItemState.Uploading
                    val progress = if (state.total == 0) 1f else state.uploaded.toFloat() / max(1, state.total).toFloat()

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("Uploading: ${state.uploaded}/${state.total} images", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // --- Submit overlay ---
            if (uiState is AddItemState.Submitting) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(d.progressSmall + 10.dp), strokeWidth = 2.dp) }
            }
        }
    }
}
