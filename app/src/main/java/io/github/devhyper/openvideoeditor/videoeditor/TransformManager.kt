package io.github.devhyper.openvideoeditor.videoeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
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
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.SessionState
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
    val key: String,
    val stringResId: Int,
    val textfieldValidation: ((String) -> String)? = null,
    val dropdownOptions: MutableList<String>? = null
) {
    var selection = ""
}

class ExportSettings {
    var exportAudio = true
    var exportVideo = true
    var hdrMode: Int = HDR_MODE_KEEP_HDR
    var audioMimeType: String? = null
    var videoMimeType: String? = null
    var framerate: Float = 0F
    var speed: Float = 0F
    var outputPath: String = ""
    var losslessCut: Boolean = false

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
    val stringResId: Int,
    val icon: ImageConstructor,
    val args: PersistentList<EffectDialogSetting>,
    val callback: (Map<String, String>) -> EffectConstructor
)

class OnVideoUserEffect(
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
    val stringResId: Int,
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

class TransformManager {
    lateinit var player: ExoPlayer

    private var hasInitialized = false

    private var transformer: Transformer? = null

    private lateinit var originalMedia: MediaItem

    private lateinit var trimmedMedia: MediaItem

    lateinit var projectData: ProjectData

    fun init(
        exoPlayer: ExoPlayer,
        uri: String,
        context: Context,
        viewModel: VideoEditorViewModel,
        requestVideoPermission: ActivityResultLauncher<String>
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
            var projectSavingSupported = false
            if (requestPersistablePermissions(
                    context,
                    uri.toUri(),
                    requestVideoPermission
                )
            ) {
                projectSavingSupported = true
            }
            viewModel.setProjectSavingSupported(projectSavingSupported)
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

    private fun requestPersistablePermissions(
        context: Context,
        uri: Uri,
        requestVideoPermission: ActivityResultLauncher<String>
    ): Boolean {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                    requestVideoPermission.launch(Manifest.permission.READ_MEDIA_VIDEO)
                }
            } else {
                if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestVideoPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            return false
        }
        return true
    }

    private fun getEffectArray(): MutableList<Effect> {
        val effectArray = mutableListOf<Effect>()
        for (userEffect in projectData.videoEffects) {
            effectArray.add(userEffect.effect())
        }
        return effectArray
    }

    fun getMergedTrim(): Trim? {
        if (projectData.mediaTrims.isNotEmpty()) {
            var currentPair = projectData.mediaTrims[0]

            if (projectData.mediaTrims.size > 1) {
                for (i in 1 until projectData.mediaTrims.size) {
                    val cutStart =
                        currentPair.first - (projectData.mediaTrims[i - 1].first - projectData.mediaTrims[i].first)
                    val cutEnd =
                        currentPair.second - (projectData.mediaTrims[i - 1].second - projectData.mediaTrims[i].second)
                    currentPair = Trim(cutStart, cutEnd)
                }
            }

            return currentPair
        }
        return null
    }

    fun clearMediaTrims() {
        projectData.mediaTrims.clear()
        updateMediaTrims()
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
        if (projectData.mediaTrims.isNotEmpty() && trim == projectData.mediaTrims.last()) {
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
        val trim = getMergedTrim()
        trimmedMedia = if (trim != null) {
            val clipConfig = ClippingConfiguration.Builder().setStartPositionMs(trim.first)
                .setEndPositionMs(trim.second).build()
            originalMedia.buildUpon().setClippingConfiguration(clipConfig).build()
        } else {
            originalMedia
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

    private fun ffmpegLosslessCut(
        context: Context,
        trim: Trim,
        outputPath: String,
        audioFallback: Boolean,
        onFFmpegError: () -> Unit
    ) {
        val ffmpegInputPath =
            FFmpegKitConfig.getSafParameterForRead(context, projectData.uri.toUri())
        val ffmpegOutputPath = FFmpegKitConfig.getSafParameterForWrite(context, outputPath.toUri())
        val audioCodec = if (audioFallback) "aac" else "copy"
        FFmpegKit.executeAsync(
            "-i $ffmpegInputPath -ss ${trim.first}ms -to ${trim.second}ms -c:v copy -c:a $audioCodec $ffmpegOutputPath"
        ) {
            val fd = context.contentResolver.openAssetFileDescriptor(outputPath.toUri(), "r")
            if (fd != null) {
                val fileSize = fd.length
                fd.close()
                if (fileSize != 0L) {
                    return@executeAsync
                }
            }
            if (audioFallback) {
                onFFmpegError()
            } else {
                ffmpegLosslessCut(context, trim, outputPath, true, onFFmpegError)
            }
        }
    }

    @SuppressLint("Recycle")
    fun export(
        context: Context,
        exportSettings: ExportSettings,
        transformerListener: Transformer.Listener,
        onFFmpegError: () -> Unit
    ) {
        // exportSettings.log()
        player.release()
        val outputPath = exportSettings.outputPath
        if (exportSettings.losslessCut) {
            val trim = getMergedTrim()
            if (trim != null) {
                ffmpegLosslessCut(context, trim, outputPath, false, onFFmpegError)
            }
        } else {
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
    }

    fun cancel() {
        FFmpegKit.cancel()
        transformer?.cancel()
    }

    fun getProgress(): Float {
        val ffmpegSessions = FFmpegKit.listSessions()
        return if (ffmpegSessions.isNotEmpty()) {
            val sessionState = ffmpegSessions.last().state
            return when (sessionState) {
                SessionState.COMPLETED -> 1F
                SessionState.RUNNING -> 0.5F
                SessionState.CREATED -> 0F
                else -> -1F
            }
        } else {
            val progressHolder = ProgressHolder()
            when (transformer?.getProgress(progressHolder)) {
                PROGRESS_STATE_UNAVAILABLE -> -1F
                PROGRESS_STATE_NOT_STARTED -> 1F
                else -> progressHolder.progress.toFloat() / 100F
            }
        }
    }
}
