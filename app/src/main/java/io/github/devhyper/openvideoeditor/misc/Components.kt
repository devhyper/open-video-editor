package io.github.devhyper.openvideoeditor.misc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import io.github.devhyper.openvideoeditor.R
import kotlinx.collections.immutable.ImmutableList
import java.io.File
import kotlin.math.roundToInt

@Composable
fun CheckboxSetting(
    name: String,
    startChecked: Boolean,
    enabled: Boolean,
    onCheckChanged: (Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(startChecked) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(modifier = Modifier.padding(end = 16.dp), text = name)
        Checkbox(
            enabled = enabled,
            checked = checked,
            onCheckedChange = { checked = !checked; onCheckChanged(it) })
    }
}

@Composable
fun SwitchSetting(
    name: String,
    enabled: Boolean = true,
    startChecked: Boolean,
    onCheckChanged: (Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(startChecked) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(modifier = Modifier.padding(end = 16.dp), text = name)
        Switch(
            enabled = enabled,
            checked = checked,
            onCheckedChange = { checked = !checked; onCheckChanged(it) })
    }
}

@Composable
fun TextfieldSetting(name: String, onValueChanged: (String) -> String) {
    var text by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = {
            errorMsg = onValueChanged(it)
            text = it
        },
        label = { Text(name) },
        isError = errorMsg.isNotEmpty(),
        supportingText = { Text(errorMsg) })
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

@Composable
fun ColorPickerSetting(
    name: String,
    defaultColor: Color,
    onSelectionChanged: (Color) -> Unit
) {
    var color by remember { mutableStateOf(defaultColor) }
    var pickerColor by remember { mutableStateOf(color) }
    var pickerOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(modifier = Modifier.padding(end = 16.dp), text = name)
        Canvas(modifier = Modifier
            .size(20.dp)
            .clickable { pickerOpen = true }
            .drawBehind {
                if (color == Color.Transparent) {
                    val tileSize = 4f
                    val tileCount = (size.width / tileSize).toInt()
                    val darkColor = Color.hsl(0f, 0f, 0.8f)
                    val lightColor = Color.hsl(1f, 1f, 1f)
                    for (i in 0..tileCount) {
                        for (j in 0..tileCount) {
                            drawRect(
                                topLeft = Offset(i * tileSize, j * tileSize),
                                color = if ((i + j) % 2 == 0) darkColor else lightColor,
                                size = Size(tileSize, tileSize)
                            )
                        }
                    }
                }
            }, onDraw = {
            drawCircle(color)
        })
    }

    val onDismissRequest = { pickerOpen = false; pickerColor = color }
    if (pickerOpen) {
        Dialog(onDismissRequest) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = stringResource(R.string.color_picker),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    ClassicColorPicker(
                        modifier = Modifier.weight(1f),
                        color = HsvColor.from(color = pickerColor),
                        onColorChanged = { pickerColor = it.toColor() })
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismissRequest,
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                color = pickerColor; onSelectionChanged(color); onDismissRequest()
                            }
                        ) {
                            Text(stringResource(R.string.accept))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FontPickerSetting(
    name: String,
    onFontChosen: (font: Font?, fontPath: String?) -> Unit,
) {
    val fonts = File("/system/fonts")
        .listFiles()!!.associateWith { Font(it) }

    var showMenu by remember { mutableStateOf(false) }
    var fontPath: String? by remember { mutableStateOf(null) }

    val fontTextModifier = Modifier.clickable { showMenu = true }

    Box {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(modifier = Modifier.padding(end = 16.dp), text = name)
            Box {
                if (fontPath == null) {
                    Text(stringResource(R.string.default_text), modifier = fontTextModifier)
                } else {
                    val fontFile = File(fontPath!!)
                    Text(
                        fontFile.nameWithoutExtension,
                        fontFamily = FontFamily(Font(fontFile)),
                        modifier = fontTextModifier
                    )
                }
                // https://issuetracker.google.com/issues/242398344
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.default_text), fontSize = 25.sp) },
                        onClick = {
                            showMenu = false
                            fontPath = null
                            onFontChosen(null, null)
                        }
                    )
                    Box(modifier = Modifier.size(width = 200.dp, height = 300.dp)) {
                        LazyColumn {
                            for ((file, font) in fonts) {
                                item {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                file.nameWithoutExtension,
                                                fontFamily = FontFamily(font),
                                                fontSize = 25.sp,
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            fontPath = file.path
                                            onFontChosen(font, fontPath)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ListDialog(
    title: String,
    dismissText: String,
    acceptText: String,
    onDismissRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
    listItems: LazyListScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = title,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge
                )
                LazyColumn(
                    modifier = Modifier.weight(1f, false),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = listItems
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                    ) {
                        Text(dismissText)
                    }
                    TextButton(
                        onClick = onAcceptRequest
                    ) {
                        Text(acceptText)
                    }
                }
            }
        }
    }
}

@Composable
fun AcceptDeclineRow(
    modifier: Modifier,
    acceptDescription: String,
    acceptOnClick: () -> Unit,
    declineDescription: String,
    declineOnClick: () -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        IconButton(
            modifier = Modifier.weight(1f), onClick = acceptOnClick
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = acceptDescription
            )
        }
        IconButton(
            modifier = Modifier.weight(1f), onClick = declineOnClick
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = declineDescription
            )
        }
    }
}

@Composable
fun ResizableRectangle(
    startingWidth: Float, startingHeight: Float, startingX: Float, startingY: Float, onResize: (
        width: Float, height: Float, x: Float, y: Float
    ) -> Unit
) {
    var width by remember { mutableFloatStateOf(startingWidth) }
    var height by remember { mutableFloatStateOf(startingHeight) }
    var x by remember { mutableFloatStateOf(startingX) }
    var y by remember { mutableFloatStateOf(startingY) }

    val colorScheme = MaterialTheme.colorScheme

    val rectangleColor = colorScheme.inversePrimary
    val rectangleStroke = 2F
    val circleColor = colorScheme.primary
    val circleSize = 32F
    val croppedAlpha = 0.5F

    val update = {
        onResize(width, height, x, y)
    }

    Canvas(
        modifier = Modifier.fillMaxSize(),
        onDraw = {
            val rectPath = Path().apply {
                moveTo(x, y)
                lineTo(x + width, y)
                lineTo(x + width, y + height)
                lineTo(x, y + height)
                lineTo(x, y)
                close()
            }
            clipPath(rectPath, clipOp = ClipOp.Difference) {
                drawRect(colorScheme.scrim.copy(alpha = croppedAlpha))
            }
        }
    )

    Box(modifier = Modifier
        .size(
            width
                .roundToInt()
                .pxToDp(),
            height
                .roundToInt()
                .pxToDp()
        )
        .absoluteOffset {
            IntOffset(
                x.roundToInt(),
                y.roundToInt()
            )
        }
        .border(rectangleStroke.dp, rectangleColor)
    ) {
        Box(
            modifier = Modifier
                .size(circleSize.dp)
                .align(Alignment.TopCenter)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newY = y + dragAmount.y
                        val newHeight = height - dragAmount.y
                        if (newY > 0 && newY < startingY + startingHeight && newHeight > 0 && newHeight < startingY + startingHeight) {
                            y = newY
                            height = newHeight
                        }
                        update()
                    }
                }
                .clip(CircleShape)
                .background(circleColor)
        )
        Box(
            modifier = Modifier
                .size(circleSize.dp)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = x + dragAmount.x
                        val newWidth = width - dragAmount.x
                        if (newX > 0 && newX < startingX + startingWidth && newWidth > 0 && newWidth < startingX + startingWidth) {
                            x = newX
                            width = newWidth
                        }
                        update()
                    }
                }
                .clip(CircleShape)
                .background(circleColor)
        )
        Box(
            modifier = Modifier
                .size(circleSize.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newWidth = width + dragAmount.x
                        if (newWidth > 0 && newWidth < startingX + startingWidth) {
                            width = newWidth
                        }
                        update()
                    }
                }
                .clip(CircleShape)
                .background(circleColor)
        )
        Box(
            modifier = Modifier
                .size(circleSize.dp)
                .align(Alignment.BottomCenter)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newHeight = height + dragAmount.y
                        if (newHeight > 0 && newHeight < startingY + startingHeight) {
                            height = newHeight
                        }
                        update()
                    }
                }
                .clip(CircleShape)
                .background(circleColor)
        )
    }
}
