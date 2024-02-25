package io.github.devhyper.openvideoeditor.videoeditor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Tv
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import io.github.devhyper.openvideoeditor.misc.validateFloat
import io.github.devhyper.openvideoeditor.misc.validateFloatAndNonzero
import io.github.devhyper.openvideoeditor.misc.validateUIntAndNonzero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@UnstableApi
val userEffectsArray: ImmutableList<UserEffect> = persistentListOf(
    UserEffect("Grayscale", Icons.Filled.Filter, RgbFilter.createGrayscaleFilter()),
    UserEffect("Invert Colors", Icons.Filled.InvertColors, RgbFilter.createInvertedFilter())
)

@UnstableApi
val dialogUserEffectsArray: ImmutableList<DialogUserEffect> = persistentListOf(
    DialogUserEffect(
        "Resolution",
        Icons.Filled.Tv,
        persistentListOf(
            EffectDialogSetting(name = "Width", textfieldValidation = {
                validateUIntAndNonzero(it)
            }
            ),
            EffectDialogSetting(name = "Height", textfieldValidation =
            {
                validateUIntAndNonzero(it)
            }
            ),
            EffectDialogSetting(
                name = "Layout", dropdownOptions =
                mutableListOf(
                    "Scale to fit",
                    "Scale to fit with crop",
                    "Stretch to fit",
                )
            )
        )
    ) { args ->
        val width = args["Width"]!!.toInt()
        val height = args["Height"]!!.toInt()
        val layout: Int = when (args["Layout"]) {
            "Scale to fit" -> Presentation.LAYOUT_SCALE_TO_FIT
            "Scale to fit with crop" -> Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            "Stretch to fit" -> Presentation.LAYOUT_STRETCH_TO_FIT
            else -> Presentation.LAYOUT_SCALE_TO_FIT
        }
        Presentation.createForWidthAndHeight(width, height, layout)
    },
    DialogUserEffect(
        "Scale",
        Icons.Filled.FormatSize,
        persistentListOf(
            EffectDialogSetting(name = "X", textfieldValidation = {
                validateFloatAndNonzero(it)
            }
            ),
            EffectDialogSetting(name = "Y", textfieldValidation =
            {
                validateFloatAndNonzero(it)
            }
            )
        )
    ) { args ->
        val x = args["X"]!!.toFloat()
        val y = args["Y"]!!.toFloat()
        ScaleAndRotateTransformation.Builder().setScale(x, y).build()
    },
    DialogUserEffect(
        "Rotate",
        Icons.AutoMirrored.Filled.RotateRight,
        persistentListOf(
            EffectDialogSetting(name = "Degrees", textfieldValidation = {
                validateFloat(it)
            }
            )
        )
    ) { args ->
        val degrees = args["Degrees"]!!.toFloat()
        ScaleAndRotateTransformation.Builder().setRotationDegrees(degrees).build()
    }
)

@UnstableApi
val onVideoUserEffectsArray: ImmutableList<OnVideoUserEffect> = persistentListOf(
    OnVideoUserEffect(
        "Text",
        Icons.Filled.TextFormat
    ) { TextEditor(it) },
    OnVideoUserEffect(
        "Crop",
        Icons.Filled.Crop
    ) { CropEditor(it) }
)
