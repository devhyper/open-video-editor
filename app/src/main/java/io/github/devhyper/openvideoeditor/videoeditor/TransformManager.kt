package io.github.devhyper.openvideoeditor.videoeditor

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition.HDR_MODE_EXPERIMENTAL_FORCE_INTERPRET_HDR_AS_SDR
import androidx.media3.transformer.Composition.HDR_MODE_KEEP_HDR
import androidx.media3.transformer.Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC
import androidx.media3.transformer.Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Transformer.PROGRESS_STATE_NOT_STARTED
import androidx.media3.transformer.Transformer.PROGRESS_STATE_UNAVAILABLE
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

typealias Trim = LongRange
typealias Editor = @Composable (MutableStateFlow<Effect?>) -> Unit

class EffectDialogSetting(
    val name: String,
    val textfieldValidation: ((String) -> String)? = null,
    val dropdownOptions: MutableList<String>? = null
) {
    var selection = ""
}

@UnstableApi
class ExportSettings {
    var exportAudio = true
    var exportVideo = true
    var hdrMode: Int = HDR_MODE_KEEP_HDR
    var audioMimeType: String? = null
    var videoMimeType: String? = null
    var framerate: Float = 0F
    var speed: Float = 0F
    var outputPath: String = ""

    /*
    fun log() {
        Log.i(
            "open-video-editor",
            "\nexportVideo: $exportVideo\nexportAudio: $exportAudio\nhdrMode: $hdrMode\naudioMimeType: $audioMimeType\nvideoMimeType: $videoMimeType\noutputPath: $outputPath"
        )
    }
     */

    fun setMediaToExportString(string: String) {
        when (string) {
            "Video and Audio" -> {
                exportVideo = true; exportAudio = true; }

            "Video only" -> {
                exportVideo = true; exportAudio = false; }

            "Audio only" -> {
                exportVideo = false; exportAudio = true; }
        }
    }

    fun setHdrModeString(string: String) {
        when (string) {
            "Keep HDR" -> {
                hdrMode = HDR_MODE_KEEP_HDR
            }

            "HDR as SDR" -> {
                hdrMode = HDR_MODE_EXPERIMENTAL_FORCE_INTERPRET_HDR_AS_SDR
            }

            "HDR to SDR (Mediacodec)" -> {
                hdrMode = HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC
            }

            "HDR to SDR (OpenGL)" -> {
                hdrMode = HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL
            }
        }
    }

    fun setAudioMimeTypeString(string: String) {
        audioMimeType = if (string == "Original") {
            null
        } else {
            string
        }
    }

    fun setVideoMimeTypeString(string: String) {
        videoMimeType = if (string == "Original") {
            null
        } else {
            string
        }
    }
}

fun getMediaToExportStrings(): ImmutableList<String> {
    return persistentListOf("Video and Audio", "Video only", "Audio only")
}

fun getHdrModesStrings(): ImmutableList<String> {
    return persistentListOf(
        "Keep HDR",
        "HDR as SDR",
        "HDR to SDR (Mediacodec)",
        "HDR to SDR (OpenGL)"
    )
}

fun getAudioMimeTypesStrings(): ImmutableList<String> {
    return persistentListOf(
        "Original",
        MimeTypes.AUDIO_AAC,
        MimeTypes.AUDIO_AMR_NB,
        MimeTypes.AUDIO_AMR_WB
    )
}

fun getVideoMimeTypesStrings(): ImmutableList<String> {
    return persistentListOf(
        "Original",
        MimeTypes.VIDEO_H263,
        MimeTypes.VIDEO_H264,
        MimeTypes.VIDEO_H265,
        MimeTypes.VIDEO_MP4V
    )
}

class DialogUserEffect(
    val name: String,
    val icon: ImageVector,
    val args: PersistentList<EffectDialogSetting>,
    val callback: (Map<String, String>) -> Effect
)

class OnVideoUserEffect(
    val name: String,
    val icon: ImageVector,
    val editor: Editor,
) {
    var callback: (Effect) -> Unit = {}

    private val effect = MutableStateFlow<Effect?>(null)

    fun runCallback() {
        effect.value?.let { callback(it) }
    }

    @Composable
    fun Editor() {
        editor(effect)
    }
}

class UserEffect(val name: String, val icon: ImageVector, val effect: Effect)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class TransformManager {
    lateinit var player: ExoPlayer

    private var hasInitialized = false

    private var transformer: Transformer? = null

    val videoEffects = mutableListOf<UserEffect>()
    val audioProcessors = mutableListOf<AudioProcessor>()
    val mediaTrims = mutableListOf<Trim>()

    private lateinit var originalMedia: MediaItem
    private lateinit var trimmedMedia: MediaItem

    private var originalMediaLength: Long = -1

    fun init(exoPlayer: ExoPlayer, uri: String) {
        if (hasInitialized) {
            if (exoPlayer != player) {
                player.release()
                player = exoPlayer
            }
        } else {
            player = exoPlayer
            originalMedia = MediaItem.fromUri(uri)
            trimmedMedia = MediaItem.fromUri(uri)
            hasInitialized = true
        }
        player.apply {
            setMediaItem(trimmedMedia)
            setVideoEffects(getEffectArray())
            prepare()
        }
    }

    private fun getEffectArray(): MutableList<Effect> {
        val effectArray = mutableListOf<Effect>()
        for (userEffect in videoEffects) {
            effectArray.add(userEffect.effect)
        }
        return effectArray
    }

    fun onPlayerDurationReady() {
        if (originalMediaLength == -1L) {
            originalMediaLength = player.duration
        }
    }

    fun addVideoEffect(effect: UserEffect) {
        videoEffects.add(effect)
        updateVideoEffects()
    }

    fun addAudioProcessor(processor: AudioProcessor) {
        audioProcessors.add(processor)
        updateAudioProcessors()
    }

    fun addMediaTrim(trim: Trim) {
        if (mediaTrims.isEmpty()) {
            if (trim == 0L..originalMediaLength) {
                return
            }
        } else if (trim == mediaTrims.last()) {
            return
        }
        mediaTrims.add(trim)
        updateMediaTrims()
    }

    fun removeVideoEffect(effect: UserEffect) {
        videoEffects.remove(effect)
        updateVideoEffects()
    }

    fun removeAudioProcessor(processor: AudioProcessor) {
        audioProcessors.remove(processor)
        updateAudioProcessors()
    }

    fun removeMediaTrim(trim: Trim) {
        mediaTrims.remove(trim)
        updateMediaTrims()
    }

    private fun updateVideoEffects() {
        player.apply {
            stop()
            setVideoEffects(getEffectArray())
            prepare()
        }
    }

    private fun updateAudioProcessors() {
        // TODO
    }

    private fun updateMediaTrims() {
        trimmedMedia = originalMedia
        for (trim in mediaTrims) {
            val clipConfig = ClippingConfiguration.Builder().setStartPositionMs(trim.first)
                .setEndPositionMs(trim.last).build()
            trimmedMedia = trimmedMedia.buildUpon().setClippingConfiguration(clipConfig).build()
        }

        player.apply {
            stop()
            setMediaItem(trimmedMedia)
            setVideoEffects(getEffectArray())
            prepare()
        }
    }

    @SuppressLint("Recycle")
    fun export(
        context: Context,
        exportSettings: ExportSettings,
        transformerListener: Transformer.Listener,
    ) {
        // exportSettings.log()
        player.stop()
        val outputPath = exportSettings.outputPath
        val fd =
            context.contentResolver.openFileDescriptor(
                outputPath.toUri(),
                "rw"
            )?.fileDescriptor
        val effectArray = getEffectArray()
        effectArray.apply {
            if (exportSettings.speed > 0) {
                add(SpeedChangeEffect(exportSettings.speed))
            }
            if (exportSettings.framerate > 0) {
                add(FrameDropEffect.createDefaultFrameDropEffect(exportSettings.framerate))
            }
        }
        val editedMediaItem = EditedMediaItem.Builder(trimmedMedia)
            .setEffects(Effects(audioProcessors, effectArray))
            .setRemoveAudio(!exportSettings.exportAudio)
            .setRemoveVideo(!exportSettings.exportVideo)
            .build()
        transformer = Transformer.Builder(context)
            .setTransformationRequest(
                TransformationRequest.Builder()
                    .setHdrMode(exportSettings.hdrMode)
                    .setAudioMimeType(exportSettings.audioMimeType)
                    .setVideoMimeType(exportSettings.videoMimeType)
                    .build()
            )
            .setMuxerFactory(CustomMuxer.Factory(fd))
            .addListener(transformerListener)
            .build()
        if (fd != null) {
            transformer!!.start(editedMediaItem, "")
        } else {
            transformer!!.start(editedMediaItem, outputPath)
        }
    }

    fun cancel() {
        transformer?.cancel()
    }

    fun getProgress(): Float {
        val progressHolder = ProgressHolder()
        return when (transformer?.getProgress(progressHolder)) {
            PROGRESS_STATE_UNAVAILABLE -> -1F
            PROGRESS_STATE_NOT_STARTED -> 1F
            else -> progressHolder.progress.toFloat() / 100F
        }
    }

    fun onExportFinished() {
        player.apply {
            setVideoEffects(getEffectArray())
            prepare()
        }
    }
}
