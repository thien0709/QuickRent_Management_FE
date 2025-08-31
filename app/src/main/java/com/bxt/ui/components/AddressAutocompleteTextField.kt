// file: com/bxt/ui/components/AddressAutocompleteTextField.kt
package com.bxt.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bxt.viewmodel.PredictionItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddressAutocompleteTextField(
    label: String,
    text: String,
    isLoading: Boolean,
    suggestions: List<PredictionItem>,
    onTextChange: (String) -> Unit,
    onSelect: (PredictionItem) -> Unit
) {
    var internal by remember { mutableStateOf(TextFieldValue(text)) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // đồng bộ khi ViewModel update text
    LaunchedEffect(text) {
        if (text != internal.text) internal = internal.copy(text = text)
    }

    // debounce 250ms
    LaunchedEffect(internal.text) {
        val q = internal.text
        delay(250)
        onTextChange(q)
        expanded = q.length >= 3
    }

    Box {
        OutlinedTextField(
            value = internal,
            onValueChange = {
                internal = it
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.heightIn(20.dp), strokeWidth = 2.dp)
            }
        )

        DropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
        ) {
            LazyColumn {
                items(suggestions) { item ->
                    DropdownMenuItem(
                        text = { Text("${item.primary} — ${item.secondary}") },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        }
                    )
                }
            }
        }
    }
}
