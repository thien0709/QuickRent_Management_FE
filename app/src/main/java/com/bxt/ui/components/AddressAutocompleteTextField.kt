package com.bxt.ui.components

import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressAutocompleteTextField(
    modifier: Modifier = Modifier,
    context: Context,
    value: String,
    onValueChange: (String) -> Unit,
    onPlaceSelected: (Place) -> Unit,
    label: @Composable (() -> Unit)? = { Text("Địa chỉ nhận hàng") }
) {
    val placesClient = remember { Places.createClient(context) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(),
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = it.isNotEmpty()
                if (it.isNotEmpty()) {
                    scope.launch {
                        val token = AutocompleteSessionToken.newInstance()
                        val request = FindAutocompletePredictionsRequest.builder()
                            .setSessionToken(token)
                            .setQuery(it)
                            .setCountries("VN") // Giới hạn tìm kiếm ở Việt Nam
                            .build()
                        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                            predictions = response.autocompletePredictions
                        }
                    }
                } else {
                    predictions = emptyList()
                }
            },
            label = label
        )

        if (predictions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                predictions.forEach { prediction ->
                    DropdownMenuItem(
                        text = { Text(prediction.getFullText(null).toString()) },
                        onClick = {
                            onValueChange(prediction.getFullText(null).toString())
                            expanded = false
                            val placeFields = listOf(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.ADDRESS)
                            val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
                            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                                onPlaceSelected(response.place)
                            }
                        }
                    )
                }
            }
        }
    }
}