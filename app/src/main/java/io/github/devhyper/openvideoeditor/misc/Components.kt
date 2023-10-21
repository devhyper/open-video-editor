package io.github.devhyper.openvideoeditor.misc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CheckboxSetting(name: String, startChecked: Boolean, onCheckChanged: (Boolean) -> Unit) {
    var checked by remember { mutableStateOf(startChecked) }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name)
        Checkbox(checked = checked, onCheckedChange = { checked = !checked; onCheckChanged(it) })
    }
}

@Composable
fun TextfieldSetting(name: String, onValueChanged: (String) -> String) {
    var text by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    OutlinedTextField(value = text, onValueChange = {
        errorMsg = onValueChanged(it)
        text = it
    }, label = { Text(name) }, isError = errorMsg.isNotEmpty(), supportingText = { Text(errorMsg) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSetting(
    name: String,
    options: ImmutableList<String>,
    onSelectionChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(options[0]) }
    val dropOptions = options.filterNot { it == selectedOptionText }
    onSelectionChanged(selectedOptionText)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = selectedOptionText,
            onValueChange = {},
            label = { Text(name) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            dropOptions.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        expanded = false
                        selectedOptionText = selectionOption
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
