package io.github.devhyper.openvideoeditor.videoeditor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
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
import io.github.devhyper.openvideoeditor.misc.PROJECT_FILE_EXT
import io.github.devhyper.openvideoeditor.misc.getFileNameFromUri
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

typealias Trim = Pair<Long, Long>
typealias ImageConstructor = () -> ImageVector
typealias EffectConstructor = () -> Effect
typealias Editor = @Composable (MutableStateFlow<EffectConstructor?>) -> Unit

class EffectDialogSetting(
    val name: String,
    val stringResId: Int,
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
    val stringResId: Int,
    val icon: ImageConstructor,
    val args: PersistentList<EffectDialogSetting>,
    val callback: (Map<String, String>) -> EffectConstructor
)

class OnVideoUserEffect(
    val name: String,
    val stringResId: Int,
    val icon: ImageConstructor,
    val editor: Editor,
) {
    var callback: (EffectConstructor) -> Unit = {}

    private val effect = MutableStateFlow<EffectConstructor?>(null)

    fun runCallback() {
        effect.value?.let { callback(it) }
    }

    @Composable
    fun Editor() {
        editor(effect)
    }
}

class UserEffect(
    val name: String,
    val stringResId:Int,
    val icon: ImageConstructor,
    val effect: EffectConstructor
) : java.io.Serializable

data class ProjectData(
    val uri: String,

    val videoEffects: MutableList<UserEffect> = mutableListOf(),
    val audioProcessors: MutableList<AudioProcessor> = mutableListOf(),
    val mediaTrims: MutableList<Trim> = mutableListOf(),
) : java.io.Serializable {
    companion object {
        fun read(uri: String, context: Context): ProjectData? {
            var projectData: ProjectData? = null
            context.contentResolver.openInputStream(uri.toUri())?.let {
                val input = ObjectInputStream(it)
                projectData = input.readObject() as ProjectData?
                input.close()
            }
            return projectData
        }
    }

    fun write(uri: String, context: Context) {
        context.contentResolver.openOutputStream(uri.toUri())?.let {
            val output = ObjectOutputStream(it)
            output.writeObject(this)
            output.close()
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class TransformManager {
    lateinit var player: ExoPlayer

    private var hasInitialized = false

    private var transformer: Transformer? = null

    private lateinit var originalMedia: MediaItem

    private lateinit var trimmedMedia: MediaItem

    lateinit var projectData: ProjectData

    private var originalMediaLength: Long = -1

    fun init(
        exoPlayer: ExoPlayer, uri: String, context: Context
    ) {
        if (hasInitialized) {
            if (exoPlayer != player) {
                if (player.isCommandAvailable(Player.COMMAND_RELEASE)) {
                    player.release()
                }
                player = exoPlayer
            }
        } else {
            player = exoPlayer
            projectData = if (getFileNameFromUri(context, uri.toUri()).substringAfterLast(
                    '.',
                    ""
                ).substringBeforeLast(' ') == PROJECT_FILE_EXT
            ) {
                ProjectData.read(uri, context) ?: ProjectData(uri)
            } else {
                ProjectData(uri)
            }
            context.contentResolver.takePersistableUriPermission(
                uri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            hasInitialized = true
        }
        originalMedia = MediaItem.fromUri(projectData.uri)
        trimmedMedia = MediaItem.fromUri(projectData.uri)
        rebuildMediaTrims()
        player.apply {
            stop()
            setMediaItem(trimmedMedia)
            setVideoEffects(getEffectArray())
            prepare()
        }
    }

    private fun getEffectArray(): MutableList<Effect> {
        val effectArray = mutableListOf<Effect>()
        for (userEffect in projectData.videoEffects) {
            effectArray.add(userEffect.effect())
        }
        return effectArray
    }

    fun onPlayerDurationReady() {
        if (originalMediaLength == -1L) {
            originalMediaLength = player.duration
        }
    }

    fun addVideoEffect(effect: UserEffect) {
        projectData.videoEffects.add(effect)
        updateVideoEffects()
    }

    fun addAudioProcessor(processor: AudioProcessor) {
        projectData.audioProcessors.add(processor)
        updateAudioProcessors()
    }

    fun addMediaTrim(trim: Trim) {
        if (projectData.mediaTrims.isEmpty()) {
            if (trim == Pair(0L, originalMediaLength)) {
                return
            }
        } else if (trim == projectData.mediaTrims.last()) {
            return
        }
        projectData.mediaTrims.add(trim)
        updateMediaTrims()
    }

    fun removeVideoEffect(effect: UserEffect) {
        projectData.videoEffects.remove(effect)
        updateVideoEffects()
    }

    fun removeAudioProcessor(processor: AudioProcessor) {
        projectData.audioProcessors.remove(processor)
        updateAudioProcessors()
    }

    fun removeMediaTrim(trim: Trim) {
        projectData.mediaTrims.remove(trim)
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

    private fun rebuildMediaTrims() {
        trimmedMedia = originalMedia
        for (trim in projectData.mediaTrims) {
            val clipConfig = ClippingConfiguration.Builder().setStartPositionMs(trim.first)
                .setEndPositionMs(trim.second).build()
            trimmedMedia =
                trimmedMedia.buildUpon().setClippingConfiguration(clipConfig).build()
        }
    }

    private fun updateMediaTrims() {
        rebuildMediaTrims()

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
        player.release()
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
            .setEffects(Effects(projectData.audioProcessors, effectArray))
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

    }
}
