package io.github.devhyper.openvideoeditor.misc

import android.content.Context
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.TypefaceSpan
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


fun getFileNameFromUri(context: Context, uri: Uri): String {
    val fileName: String?
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    cursor?.moveToFirst()
    fileName = nameIndex?.let { cursor.getString(it) }
    cursor?.close()
    if (fileName.isNullOrEmpty()) {
        return "null"
    }
    return fileName
}

fun getVideoFileDuration(context: Context, uri: Uri): Long? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, uri)
    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    retriever.release()
    return time?.toLong()
}

fun ComponentActivity.setupSystemUi() {
    enableEdgeToEdge()
    actionBar?.hide()
}

fun ComponentActivity.setImmersiveMode(enabled: Boolean) {
    val windowInsetsController =
        WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (enabled) {
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}

fun Long.formatMinSec(): String {
    return if (this == 0L) {
        "00:00"
    } else {
        String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) -
                    TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(this)
                    )
        )
    }
}

fun <T> MutableList<T>.move(item: T, newIndex: Int) {
    val currentIndex = indexOf(item)
    if (currentIndex < 0) return
    removeAt(currentIndex)
    add(newIndex, item)
}

fun Modifier.repeatingClickable(
    interactionSource: InteractionSource,
    enabled: Boolean,
    maxDelayMillis: Long = 1000,
    minDelayMillis: Long = 5,
    delayDecayFactor: Float = .40f,
    onClick: () -> Unit
): Modifier = this.composed {

    val currentClickListener by rememberUpdatedState(onClick)

    pointerInput(interactionSource, enabled) {
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val heldButtonJob = launch {
                    var currentDelayMillis = maxDelayMillis
                    while (enabled && down.pressed) {
                        currentClickListener()
                        delay(currentDelayMillis)
                        val nextMillis =
                            currentDelayMillis - (currentDelayMillis * delayDecayFactor)
                        currentDelayMillis = nextMillis.toLong().coerceAtLeast(minDelayMillis)
                    }
                }
                waitForUpOrCancellation()
                heldButtonJob.cancel()
            }
        }
    }
}

fun TextureOverlay.toOverlayEffect(): OverlayEffect {
    val overlaysBuilder = ImmutableList.Builder<TextureOverlay>()
    overlaysBuilder.add(this)
    return OverlayEffect(overlaysBuilder.build())
}

fun SpannableString.setFullLengthSpan(what: Any) {
    setSpan(what, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
}

fun screenToNdc(screen: Float, screenSize: Float): Float {
    return (((screen / screenSize) * 2f) - 1f).coerceIn(-1f..1f)
}

fun spToPx(sp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        Resources.getSystem().displayMetrics
    ).toInt()
}

fun LongRange.toLongPair(): Pair<Long, Long> {
    return Pair(this.first, this.last)
}

fun Pair<Long, Long>.toLongRange(): LongRange {
    return LongRange(this.first, this.second)
}

@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }

@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

class CompatTypefaceSpan(private val typeface: Typeface) :
    TypefaceSpan(null) {
    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, typeface)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, typeface)
    }

    companion object {
        private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
            val oldStyle: Int
            val old = paint.typeface
            oldStyle = old?.style ?: 0
            val fake = oldStyle and tf.style.inv()
            if (fake and Typeface.BOLD != 0) {
                paint.isFakeBoldText = true
            }
            if (fake and Typeface.ITALIC != 0) {
                paint.textSkewX = -0.25f
            }
            paint.typeface = tf
        }
    }
}
