package com.bxt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

import com.mapbox.geojson.Point
import com.mapbox.search.autocomplete.PlaceAutocomplete
import com.mapbox.search.autocomplete.PlaceAutocompleteOptions
import com.mapbox.search.autocomplete.PlaceAutocompleteSuggestion
import com.mapbox.search.common.IsoCountryCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapboxSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    proximity: Point?,
    onPlacePicked: (point: Point, fullAddress: String) -> Unit
) {
    val placeAutocomplete = remember { PlaceAutocomplete.create() }
    var suggestions by remember { mutableStateOf<List<PlaceAutocompleteSuggestion>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var suppressNext by remember { mutableStateOf(false) } // chặn reopen sau khi pick
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // Debounce input & fetch suggestions (I/O trên Dispatchers.IO)
    LaunchedEffect(value, proximity, suppressNext) {
        snapshotFlow { value }
            .debounce(300)
            .filter { it.length >= 3 && !suppressNext }
            .collectLatest { q ->
                isLoading = true
                try {
                    val resp = withContext(Dispatchers.IO) {
                        placeAutocomplete.suggestions(
                            query = q,
                            proximity = proximity,
                            options = PlaceAutocompleteOptions(
                                countries = listOf(IsoCountryCode("VN"))
                            )
                        )
                    }
                    if (!suppressNext) {
                        suggestions = if (resp.isValue) (resp.value ?: emptyList()) else emptyList()
                        showSuggestions = suggestions.isNotEmpty()
                    }
                } catch (_: Throwable) {
                    // ignore, đóng list
                    suggestions = emptyList()
                    showSuggestions = false
                } finally {
                    isLoading = false
                }
            }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                if (suppressNext) {
                    // lần set sau khi pick -> không mở list
                    showSuggestions = false
                    suggestions = emptyList()
                    suppressNext = false
                } else {
                    showSuggestions = it.length >= 3
                }
            },
            label = { Text("Tìm địa chỉ (Mapbox)…") },
            leadingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = {
                        onValueChange("")
                        showSuggestions = false
                        suggestions = emptyList()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Xóa")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (showSuggestions && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                LazyColumn {
                    items(suggestions.take(5)) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val selection = withContext(Dispatchers.IO) {
                                            placeAutocomplete.select(suggestion)
                                        }
                                        if (selection.isValue) {
                                            val result = selection.value!!
                                            val pickedPoint = result.coordinate
                                            val addr =
                                                result.address?.formattedAddress ?: result.name
                                            // chặn reopen, đóng list, clear focus
                                            suppressNext = true
                                            showSuggestions = false
                                            suggestions = emptyList()
                                            focusManager.clearFocus()
                                            onPlacePicked(pickedPoint, addr)
                                            onValueChange(addr)
                                        } else {
                                            showSuggestions = false
                                            suggestions = emptyList()
                                            focusManager.clearFocus()
                                        }
                                    } catch (_: Throwable) {
                                        showSuggestions = false
                                        suggestions = emptyList()
                                        focusManager.clearFocus()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: PlaceAutocompleteSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = suggestion.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!suggestion.formattedAddress.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = suggestion.formattedAddress!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
