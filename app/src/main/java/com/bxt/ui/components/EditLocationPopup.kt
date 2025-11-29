package com.bxt.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point

@Composable
fun EditLocationPopup(
    currentLocation: String,
    proximity: Point?,
    onDismiss: () -> Unit,
    onSave: (point: Point?, fullAddress: String) -> Unit,
    onGetCurrentLocation: () -> Unit,
    isGettingCurrent: Boolean = false
) {
    var query by remember { mutableStateOf(currentLocation) }
    var selectedPoint by remember { mutableStateOf<Point?>(null) }
    var selectedAddress by remember { mutableStateOf("") }


    LaunchedEffect(currentLocation) {
        if (currentLocation.isNotBlank()) {
            query = currentLocation
            selectedPoint = null
            selectedAddress = currentLocation
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change address") },
        text = {
            Column {

                MapboxSearchBar(
                    value = query,
                    onValueChange = { q ->
                        query = q
                        if (q != selectedAddress) {
                            selectedPoint = null
                            selectedAddress = ""
                        }
                    },
                    proximity = proximity,
                    onPlacePicked = { p, addr ->
                        selectedPoint = p
                        selectedAddress = addr
                        query = addr
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (selectedPoint == null)
                        "Type address or pick suggestion"
                    else
                        "Selected: $selectedAddress",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onGetCurrentLocation,
                    enabled = !isGettingCurrent
                ) {
                    if (isGettingCurrent) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        "Current",
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        val addr = selectedAddress.ifBlank { query.trim() }
                        onSave(selectedPoint, addr)
                    },
                    enabled = query.isNotBlank()
                ) {
                    Text("Save", maxLines = 1)
                }
            }
        }
    )
}
