package io.github.devhyper.openvideoeditor.videoeditor

import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextOverlay
import io.github.devhyper.openvideoeditor.R
import io.github.devhyper.openvideoeditor.misc.ColorPickerSetting
import io.github.devhyper.openvideoeditor.misc.ListDialog
import io.github.devhyper.openvideoeditor.misc.ResizableRectangle
import io.github.devhyper.openvideoeditor.misc.TextfieldSetting
import io.github.devhyper.openvideoeditor.misc.screenToNdc
import io.github.devhyper.openvideoeditor.misc.setFullLengthSpan
import io.github.devhyper.openvideoeditor.misc.spToPx
import io.github.devhyper.openvideoeditor.misc.toOverlayEffect
import io.github.devhyper.openvideoeditor.misc.validateUIntAndNonzero
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

@Composable
fun TextEditor(effectFlow: MutableStateFlow<EffectConstructor?>) {
    val update =
        { offsetX: Float, offsetY: Float, textValue: String, videoWidth: Float, videoHeight: Float, textBackgroundColor: Color, textForegroundColor: Color, textSize: Int ->
            effectFlow.update {
                {
                    val overlaySettings = OverlaySettings.Builder()
                    val x = screenToNdc(offsetX, videoWidth)
                    val y = screenToNdc(offsetY, videoHeight)
                    overlaySettings.setOverlayFrameAnchor(1f, -1f)
                    overlaySettings.setBackgroundFrameAnchor(x, -y)
                    val spanString = SpannableString(textValue)
                    spanString.run {
                        setFullLengthSpan(ForegroundColorSpan(textForegroundColor.toArgb()))
                        setFullLengthSpan(BackgroundColorSpan(textBackgroundColor.toArgb()))
                        setFullLengthSpan(AbsoluteSizeSpan(spToPx(textSize.sp.value)))
                    }
                    TextOverlay.createStaticTextOverlay(
                        spanString,
                        overlaySettings.build()
                    ).toOverlayEffect()
                }
            }
        }
    var showListDialog by remember { mutableStateOf(true) }
    var textSize by remember { mutableIntStateOf(12) }
    var textBackgroundColor by remember { mutableStateOf(Color.Transparent) }
    var textForegroundColor by remember { mutableStateOf(Color.Black) }
    if (showListDialog) {
        ListDialog(
            title = stringResource(R.string.text),
            dismissText = stringResource(R.string.cancel),
            acceptText = stringResource(R.string.set),
            onDismissRequest = { showListDialog = false },
            onAcceptRequest = {
                showListDialog = false
            }
        ) {
            item {
                TextfieldSetting(
                    name = stringResource(R.string.size),
                    stringResId = R.string.size,
                    onValueChanged = {
                        val error = validateUIntAndNonzero(it)
                        textSize = if (error.isEmpty()) {
                            it.toInt()
                        } else {
                            12
                        }
                        error
                    })
            }
            item {
                ColorPickerSetting(
                    name = stringResource(R.string.background_color),
                    defaultColor = Color.Transparent,
                    onSelectionChanged = {
                        textBackgroundColor = it
                    })
                ColorPickerSetting(
                    name = stringResource(R.string.foreground_color),
                    defaultColor = Color.Black,
                    onSelectionChanged = {
                        textForegroundColor = it
                    })
            }
        }
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val videoWidth = constraints.maxWidth.toFloat()
            val videoHeight = constraints.maxHeight.toFloat()
            var offsetX by remember { mutableFloatStateOf(videoWidth / 2f) }
            var offsetY by remember { mutableFloatStateOf(videoHeight / 2f) }
            var textValue by remember { mutableStateOf("Text") }
            val updateFlow = {
                update(
                    offsetX,
                    offsetY,
                    textValue,
                    videoWidth,
                    videoHeight,
                    textBackgroundColor,
                    textForegroundColor,
                    textSize
                )
            }
            updateFlow()
            BasicTextField(modifier = Modifier
                .absoluteOffset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (offsetX + dragAmount.x > 0 && offsetX + dragAmount.x < videoWidth) {
                            offsetX += dragAmount.x
                        }
                        if (offsetY + dragAmount.y > 0 && offsetY + dragAmount.y < videoHeight) {
                            offsetY += dragAmount.y
                        }
                        updateFlow()
                    }
                },
                textStyle = TextStyle.Default.copy(
                    fontSize = textSize.sp,
                    color = textForegroundColor,
                    background = textBackgroundColor
                ),
                value = textValue,
                onValueChange = {
                    textValue = it
                    updateFlow()
                })
        }
    }
}

@Composable
fun CropEditor(effectFlow: MutableStateFlow<EffectConstructor?>) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val videoWidth = constraints.maxWidth.toFloat()
        val videoHeight = constraints.maxHeight.toFloat()
        ResizableRectangle(videoWidth, videoHeight, 0F, 0F) { width, height, x, y ->
            val left = screenToNdc(x, videoWidth)
            val right = screenToNdc(x + width, videoWidth)
            val bottom = screenToNdc(videoHeight - y - height, videoHeight)
            val top = screenToNdc(videoHeight - y, videoHeight)
            if (right > left && top > bottom) {
                effectFlow.update {
                    {
                        Crop(
                            left, right, bottom, top
                        )
                    }
                }
            }
        }
    }
}
