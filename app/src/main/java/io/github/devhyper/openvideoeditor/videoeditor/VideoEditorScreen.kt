package io.github.devhyper.openvideoeditor.videoeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper.getMainLooper
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_GET_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.Commands
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer.Listener
import io.github.devhyper.openvideoeditor.R
import io.github.devhyper.openvideoeditor.misc.DropdownSetting
import io.github.devhyper.openvideoeditor.misc.TextfieldSetting
import io.github.devhyper.openvideoeditor.misc.formatMinSec
import io.github.devhyper.openvideoeditor.misc.getFileNameFromUri
import io.github.devhyper.openvideoeditor.misc.repeatingClickable
import io.github.devhyper.openvideoeditor.settings.SettingsActivity
import io.github.devhyper.openvideoeditor.ui.theme.OpenVideoEditorTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun VideoEditorScreen(
    uri: String,
    createDocument: ActivityResultLauncher<String?>
) {

    val viewModel = viewModel { VideoEditorViewModel() }

    val controlsVisible by viewModel.controlsVisible.collectAsState()

    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context)
            .apply {
                setSeekBackIncrementMs(PLAYER_SEEK_BACK_INCREMENT)
                setSeekForwardIncrementMs(PLAYER_SEEK_FORWARD_INCREMENT)
            }
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(uri))
                setVideoEffects(emptyList()) // Needed to setup effects processor, so effects can be enabled at runtime
                prepare()
            }
    }

    val transformManager = remember { TransformManager(player, uri) }

    var listenerRepeating by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(player.isPlaying) }

    var fpm by remember { mutableFloatStateOf(0F) }

    var totalDuration by remember { mutableLongStateOf(0L) }

    var totalDurationFrames by remember { mutableLongStateOf(0L) }

    var currentTime by remember { mutableLongStateOf(0L) }

    var currentTimeFrames by remember { mutableLongStateOf(0L) }

    var playbackState by remember { mutableIntStateOf(player.playbackState) }

    val filterDurationEditorSliderPosition by viewModel.filterDurationEditorSliderPosition.collectAsState()

    OpenVideoEditorTheme(forceDarkTheme = true, forceBlackStatusBar = true) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box {
                DisposableEffect(key1 = Unit) {
                    val listenerHandler = Handler(getMainLooper())
                    val listener =
                        object : Player.Listener {
                            override fun onAvailableCommandsChanged(
                                availableCommands: Commands
                            ) {
                                super.onAvailableCommandsChanged(availableCommands)

                                if (availableCommands.contains(COMMAND_GET_CURRENT_MEDIA_ITEM)) {
                                    transformManager.onPlayerDurationReady()
                                    if (filterDurationEditorSliderPosition.endInclusive == 0f) {
                                        viewModel.setFilterDurationEditorSliderPosition(0f..player.duration.toFloat())
                                    }
                                }
                            }

                            override fun onEvents(
                                regularPlayer: Player,
                                events: Player.Events
                            ) {
                                super.onEvents(player, events)

                                fpm = (player.videoFormat?.frameRate ?: 0F) / 1000F
                                if (player.duration > 0L && fpm > 0F) {
                                    totalDuration = player.duration
                                    totalDurationFrames = (totalDuration * fpm).toLong()
                                }
                                isPlaying = player.isPlaying
                                playbackState = player.playbackState

                                if (isPlaying) {
                                    if (!listenerRepeating) {
                                        listenerRepeating = true
                                        listenerHandler.post(
                                            object : Runnable {
                                                override fun run() {
                                                    currentTime =
                                                        player.currentPosition.coerceAtLeast(0L)
                                                    currentTimeFrames =
                                                        ((currentTime * fpm).toLong()).coerceAtMost(
                                                            totalDurationFrames
                                                        )
                                                    if (listenerRepeating) {
                                                        listenerHandler.postDelayed(
                                                            this,
                                                            REFRESH_RATE
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    listenerRepeating = false
                                    currentTime =
                                        player.currentPosition.coerceAtLeast(0L)
                                    currentTimeFrames =
                                        ((currentTime * fpm).toLong()).coerceAtMost(
                                            totalDurationFrames
                                        )
                                }
                            }
                        }

                    player.addListener(listener)

                    onDispose {
                        player.removeListener(listener)
                        player.release()
                    }
                }

                AndroidView(
                    modifier =
                    Modifier.clickable {
                        viewModel.setControlsVisible(!controlsVisible)
                    },
                    factory = {
                        SurfaceView(context).apply {
                            layoutParams =
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                            player.setVideoSurfaceView(this)
                        }
                    }
                )

                PlayerControls(
                    modifier = Modifier
                        .fillMaxSize(),
                    isVisible = { controlsVisible },
                    isPlaying = { isPlaying },
                    title = { getFileNameFromUri(context, Uri.parse(uri)) },
                    transformManager = transformManager,
                    createDocument = createDocument,
                    playbackState = { playbackState },
                    onReplayClick = { player.seekBack() },
                    onForwardClick = { player.seekForward() },
                    onPauseToggle = {
                        when {
                            player.isPlaying -> {
                                player.pause()
                            }

                            player.isPlaying.not() &&
                                    playbackState == Player.STATE_ENDED -> {
                                player.seekTo(0)
                                player.playWhenReady = true
                            }

                            else -> {
                                player.play()
                            }
                        }
                        isPlaying = isPlaying.not()
                    },
                    fpm = { fpm },
                    totalDuration = { totalDuration },
                    totalDurationFrames = { totalDurationFrames },
                    currentTime = { currentTime },
                    currentTimeFrames = { currentTimeFrames }
                ) { timeMs: Float ->
                    player.seekTo(timeMs.toLong())
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PlayerControls(
    modifier: Modifier = Modifier,
    isVisible: () -> Boolean,
    isPlaying: () -> Boolean,
    title: () -> String,
    transformManager: TransformManager,
    createDocument: ActivityResultLauncher<String?>,
    onReplayClick: () -> Unit,
    onForwardClick: () -> Unit,
    onPauseToggle: () -> Unit,
    fpm: () -> Float,
    totalDuration: () -> Long,
    totalDurationFrames: () -> Long,
    currentTime: () -> Long,
    currentTimeFrames: () -> Long,
    playbackState: () -> Int,
    onSeekChanged: (timeMs: Float) -> Unit
) {

    val visible = remember(isVisible()) { isVisible() }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = SolidColor(MaterialTheme.colorScheme.scrim),
                    alpha = 0.5F
                )
        ) {
            Box(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize()
            ) {
                TopControls(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    title = title,
                    transformManager = transformManager,
                    createDocument = createDocument
                )

                CenterControls(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    isPlaying = isPlaying,
                    onReplayClick = onReplayClick,
                    onForwardClick = onForwardClick,
                    onPauseToggle = onPauseToggle,
                    playbackState = playbackState
                )

                BottomControls(
                    modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .animateEnterExit(
                            enter =
                            slideInVertically(
                                initialOffsetY = { fullHeight: Int ->
                                    fullHeight
                                }
                            ),
                            exit =
                            slideOutVertically(
                                targetOffsetY = { fullHeight: Int ->
                                    fullHeight
                                }
                            )
                        ),
                    fpm = fpm,
                    totalDuration = totalDuration,
                    totalDurationFrames = totalDurationFrames,
                    currentTime = currentTime,
                    currentTimeFrames = currentTimeFrames,
                    onSeekChanged = onSeekChanged,
                    transformManager = transformManager
                )
            }
        }
    }
}

@Composable
private fun TopControls(
    modifier: Modifier = Modifier,
    title: () -> String,
    transformManager: TransformManager,
    createDocument: ActivityResultLauncher<String?>
) {
    val activity = LocalContext.current as Activity
    val videoTitle = remember(title()) { title() }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { activity.finish() }) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back)
            )
        }

        Text(
            text = videoTitle,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f, false)
        )

        IconButton(onClick = { showThreeDotMenu = !showThreeDotMenu }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.more_vertical_options)
            )
            DropdownMenu(
                expanded = showThreeDotMenu,
                onDismissRequest = { showThreeDotMenu = false },
                content = {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings)) },
                        onClick = {
                            showThreeDotMenu = false
                            val intent = Intent(activity, SettingsActivity::class.java)
                            activity.startActivity(intent)
                        })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.export)) },
                        onClick = { showThreeDotMenu = false; showExportDialog = true })
                })
        }
    }

    if (showExportDialog) {
        ExportDialog(transformManager, createDocument, videoTitle) { showExportDialog = false }
    }
}

@Composable
private fun CenterControls(
    modifier: Modifier = Modifier,
    isPlaying: () -> Boolean,
    playbackState: () -> Int,
    onReplayClick: () -> Unit,
    onPauseToggle: () -> Unit,
    onForwardClick: () -> Unit
) {
    val isVideoPlaying = remember(isPlaying()) { isPlaying() }

    val playerState = remember(playbackState()) { playbackState() }

    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(modifier = Modifier.size(40.dp), onClick = onReplayClick) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Filled.Replay5,
                contentDescription = stringResource(R.string.replay_5_seconds),
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = onPauseToggle) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector =
                when {
                    isVideoPlaying -> {
                        Icons.Filled.Pause
                    }

                    isVideoPlaying.not() && playerState == Player.STATE_ENDED -> {
                        Icons.Filled.Replay
                    }

                    else -> {
                        Icons.Filled.PlayArrow
                    }
                },
                contentDescription = stringResource(R.string.play_pause),
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = onForwardClick) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Filled.Forward10,
                contentDescription = stringResource(R.string.forward_10_seconds),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomControls(
    modifier: Modifier = Modifier,
    fpm: () -> Float,
    totalDuration: () -> Long,
    totalDurationFrames: () -> Long,
    currentTime: () -> Long,
    currentTimeFrames: () -> Long,
    onSeekChanged: (timeMs: Float) -> Unit,
    transformManager: TransformManager
) {

    val scope = rememberCoroutineScope()
    val filterSheetState = rememberModalBottomSheetState()
    val layerSheetState = rememberModalBottomSheetState()
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var showLayerBottomSheet by remember { mutableStateOf(false) }

    val videoFpm = remember(fpm()) { fpm() }
    val duration = remember(totalDuration()) { totalDuration() }
    val durationFrames = remember(totalDurationFrames()) { totalDurationFrames() }
    val videoTime = remember(currentTime()) { currentTime() }
    val videoTimeFrames = remember(currentTimeFrames()) { currentTimeFrames() }

    val viewModel = viewModel { VideoEditorViewModel() }
    val filterDurationEditorEnabled by viewModel.filterDurationEditorEnabled.collectAsState()
    val filterDurationCallback by viewModel.filterDurationCallback.collectAsState()
    val prevFilterDurationEditorSliderPosition by viewModel.prevFilterDurationEditorSliderPosition.collectAsState()
    val filterDurationEditorSliderPosition by viewModel.filterDurationEditorSliderPosition.collectAsState()

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (filterDurationEditorEnabled) {
                RangeSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    value = filterDurationEditorSliderPosition,
                    onValueChange = { rangeArg ->
                        var range = rangeArg
                        if (range.endInclusive == 0f) {
                            range = range.start..1f
                        }
                        viewModel.setPrevFilterDurationEditorSliderPosition(
                            filterDurationEditorSliderPosition
                        )
                        viewModel.setFilterDurationEditorSliderPosition(range)
                        if (prevFilterDurationEditorSliderPosition.start != filterDurationEditorSliderPosition.start) {
                            onSeekChanged(range.start)
                        } else {
                            onSeekChanged(range.endInclusive)
                        }
                    },
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = MaterialTheme.colorScheme.inversePrimary
                    ),
                    valueRange = 0f..duration.toFloat(),
                )
            } else {
                Slider(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = videoTime.toFloat(),
                    onValueChange = onSeekChanged,
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = MaterialTheme.colorScheme.inversePrimary
                    ),
                    valueRange = 0f..duration.toFloat(),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(2f, false),
                text = videoTime.formatMinSec() + "/" + duration.formatMinSec(),
            )
            Row(
                modifier = Modifier.weight(2f, false),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier
                        .weight(1f, false)
                        .repeatingClickable(remember { MutableInteractionSource() },
                            true,
                            onClick = {
                                onSeekChanged((videoTime.toFloat() - (1F / videoFpm)) + 1F)
                            }), onClick = {
                        onSeekChanged((videoTime.toFloat() - (1F / videoFpm)) + 1F)
                    }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.decrement_frame)
                    )
                }
                Text(
                    modifier = Modifier.weight(1f, false),
                    text = "$videoTimeFrames/$durationFrames",
                    textAlign = TextAlign.Center
                )
                IconButton(
                    modifier = Modifier
                        .weight(1f, false)
                        .repeatingClickable(remember { MutableInteractionSource() },
                            true,
                            onClick = {
                                onSeekChanged((videoTime.toFloat() + (1F / videoFpm)) + 1F)
                            }), onClick = {
                        onSeekChanged((videoTime.toFloat() + (1F / videoFpm)) + 1F)
                    }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.increment_frame)
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f),
            ) {
                if (filterDurationEditorEnabled) {
                    IconButton(modifier = Modifier.weight(1f), onClick = {
                        viewModel.setFilterDurationEditorEnabled(false)
                        filterDurationCallback(
                            LongRange(
                                filterDurationEditorSliderPosition.start.toLong(),
                                filterDurationEditorSliderPosition.endInclusive.toLong()
                            )
                        )
                    }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.accept_filter)
                        )
                    }
                    IconButton(modifier = Modifier.weight(1f), onClick = {
                        viewModel.setFilterDurationEditorEnabled(false)
                    }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.decline_filter)
                        )
                    }
                } else {
                    IconButton(modifier = Modifier.weight(1f), onClick = {
                        showLayerBottomSheet = true
                    }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = stringResource(R.string.open_layer_drawer)
                        )
                    }
                    IconButton(modifier = Modifier.weight(1f), onClick = {
                        showFilterBottomSheet = true
                    }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Filter,
                            contentDescription = stringResource(R.string.open_filter_drawer)
                        )
                    }
                }
            }
        }
    }
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxSize(),
            onDismissRequest = {
                showFilterBottomSheet = false
            },
            sheetState = filterSheetState
        ) {
            FilterDrawer(transformManager) {
                scope.launch { filterSheetState.hide() }.invokeOnCompletion {
                    if (!filterSheetState.isVisible) {
                        showFilterBottomSheet = false
                    }
                }
            }
        }
    } else if (showLayerBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxSize(),
            onDismissRequest = {
                showLayerBottomSheet = false
            },
            sheetState = layerSheetState
        ) {
            LayerDrawer(transformManager)
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun LayerDrawer(transformManager: TransformManager) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.video_layers),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.headlineMedium
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            items(transformManager.videoEffects)
            { effect ->
                LayerDrawerItem(
                    name = effect.name,
                    icon = effect.icon,
                    range = 0L..transformManager.player.duration,
                    onClick = {
                        transformManager.removeVideoEffect(effect)
                    }
                )
            }
            items(transformManager.audioProcessors)
            { processor ->
                LayerDrawerItem(
                    name = processor.toString(),
                    icon = Icons.Filled.Audiotrack,
                    range = 0L..transformManager.player.duration,
                    onClick = {
                        transformManager.removeAudioProcessor(processor)
                    }
                )
            }
            items(transformManager.mediaTrims)
            { trim ->
                LayerDrawerItem(
                    name = stringResource(R.string.trim),
                    icon = Icons.Filled.ContentCut,
                    range = trim.first..trim.last,
                    onClick = {
                        transformManager.removeMediaTrim(trim)
                    }
                )
            }
        }
    }
}

@Composable
private fun LayerDrawerItem(
    name: String,
    icon: ImageVector,
    range: LongRange,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(imageVector = icon, contentDescription = stringResource(R.string.layer_icon))
            Column(
                modifier = Modifier
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = name)
                Text(
                    text = "${range.first.formatMinSec()}:${range.last.formatMinSec()}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.remove_filter)
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun FilterDrawer(transformManager: TransformManager, onDismissRequest: () -> Unit) {
    val viewModel = viewModel { VideoEditorViewModel() }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.video_filters),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.headlineMedium
        )
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            columns = GridCells.Adaptive(100.dp)
        ) {
            item {
                FilterDrawerItem(stringResource(R.string.trim), Icons.Filled.ContentCut, onClick = {
                    viewModel.setFilterDurationEditorEnabled(true)
                    viewModel.setFilterDurationCallback { range ->
                        transformManager.addMediaTrim(
                            range
                        )
                    }
                    onDismissRequest()
                })
            }
            items(userEffectsArray) { userEffect ->
                FilterDrawerItem(
                    userEffect,
                    transformManager
                )
            }
            items(dialogUserEffectsArray) { dialogUserEffect ->
                dialogUserEffect.run {
                    FilterDrawerItem(name, icon, args, transformManager, callback)
                }
            }
        }
    }
}

@Composable
private fun FilterDrawerItem(
    name: String,
    icon: ImageVector,
    args: PersistentList<EffectDialogSetting>,
    transformManager: TransformManager,
    callback: (Map<String, String>) -> Effect
) {
    val viewModel = viewModel { VideoEditorViewModel() }
    var showFilterDialog by remember { mutableStateOf(false) }
    FilterDrawerItem(
        name,
        icon,
        onClick = { showFilterDialog = true; viewModel.setFilterDialogArgs(args) })
    if (showFilterDialog) {
        FilterDialog(name = name, { argMap ->
            val effect = callback(argMap)
            UserEffect(name, icon, effect)
        }, transformManager) {
            showFilterDialog = false
        }
    }
}

@Composable
private fun FilterDrawerItem(
    userEffect: UserEffect,
    transformManager: TransformManager
) {
    FilterDrawerItem(
        userEffect.name,
        userEffect.icon,
        onClick = { transformManager.addVideoEffect(userEffect) })
}

@Composable
private fun FilterDrawerItem(name: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = name
            )
        }
        Text(
            textAlign = TextAlign.Center,
            softWrap = false,
            text = name
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun FilterDialog(
    name: String,
    callback: (Map<String, String>) -> UserEffect,
    transformManager: TransformManager,
    onDismissRequest: () -> Unit
) {
    val viewModel = viewModel { VideoEditorViewModel() }
    val args by viewModel.filterDialogArgs.collectAsState()
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for (arg in args) {
                        val textfield = arg.textfieldValidation
                        val dropdown = arg.dropdownOptions
                        if (textfield != null) {
                            item {
                                TextfieldSetting(name = arg.name, onValueChanged = {
                                    val error = textfield(it)
                                    if (error.isEmpty()) {
                                        arg.selection = it
                                    } else {
                                        arg.selection = ""
                                    }
                                    viewModel.setFilterDialogArgs(args)
                                    error
                                })
                            }
                        } else if (dropdown != null) {
                            item {
                                DropdownSetting(
                                    name = arg.name,
                                    options = dropdown.toImmutableList()
                                ) {
                                    arg.selection = it
                                    viewModel.setFilterDialogArgs(args)
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.End)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            var error = false
                            val callbackArgsMap = mutableMapOf<String, String>()
                            for (arg in args) {
                                val string = arg.selection
                                if (string.isEmpty()) {
                                    error = true
                                    break
                                }
                                callbackArgsMap[arg.name] = string
                            }
                            if (!error) {
                                val userEffect = callback(callbackArgsMap.toMap())
                                transformManager.addVideoEffect(userEffect)
                                onDismissRequest()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add))
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ExportDialog(
    transformManager: TransformManager,
    createDocument: ActivityResultLauncher<String?>,
    title: String,
    onDismissRequest: () -> Unit
) {
    val exportSettings: ExportSettings by remember { mutableStateOf(ExportSettings()) }
    val viewModel = viewModel { VideoEditorViewModel() }
    val outputPath by viewModel.outputPath.collectAsState()
    var exportException: ExportException? by remember { mutableStateOf(null) }
    if (outputPath.isNotEmpty()) {
        exportSettings.outputPath = outputPath
        if (exportException != null) {
            ExportFailedAlertDialog(exportException) {
                onDismissRequest(); exportException = null; viewModel.setOutputPath("")
            }
        } else {
            val transformerListener: Listener =
                object : Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        transformManager.onExportFinished()
                    }

                    override fun onError(
                        composition: Composition, result: ExportResult,
                        exception: ExportException
                    ) {
                        transformManager.onExportFinished()
                        exportException = exception
                        // Log.e("open-video-editor", "Export exception: ", exception)
                    }
                }
            transformManager.export(LocalContext.current, exportSettings, transformerListener)
            ExportProgressDialog(transformManager) { onDismissRequest(); viewModel.setOutputPath("") }
        }
    } else {
        Dialog(onDismissRequest = { onDismissRequest() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.export),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.SpaceAround,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            DropdownSetting(
                                name = stringResource(R.string.media_to_export),
                                options = getMediaToExportStrings()
                            ) {
                                exportSettings.setMediaToExportString(it)
                            }
                        }
                        item {
                            DropdownSetting(
                                name = stringResource(R.string.hdr_mode),
                                options = getHdrModesStrings()
                            ) {
                                exportSettings.setHdrModeString(it)
                            }
                        }
                        item {
                            DropdownSetting(
                                name = stringResource(R.string.audio_type),
                                options = getAudioMimeTypesStrings()
                            ) {
                                exportSettings.setAudioMimeTypeString(it)
                            }
                        }
                        item {
                            DropdownSetting(
                                name = stringResource(R.string.video_type),
                                options = getVideoMimeTypesStrings()
                            ) {
                                exportSettings.setVideoMimeTypeString(it)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.End)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = { onDismissRequest() },
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                createDocument.launch(title)
                            }
                        ) {
                            Text(stringResource(R.string.export))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportProgressDialog(
    transformManager: TransformManager,
    onDismissRequest: () -> Unit
) {
    var exportProgress by remember { mutableFloatStateOf(0F) }
    val animatedProgress = animateFloatAsState(
        targetValue = exportProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = stringResource(R.string.export_progress_animation)
    ).value
    val exportComplete = exportProgress == 1F
    val progressHandler = Handler(getMainLooper())
    progressHandler.postDelayed(
        object : Runnable {
            override fun run() {
                exportProgress = transformManager.getProgress()
                if (exportProgress != 1F && exportProgress != -1F) {
                    progressHandler.postDelayed(this, REFRESH_RATE)
                }
            }
        }, REFRESH_RATE
    )
    Dialog(onDismissRequest = {
        if (exportComplete) {
            onDismissRequest()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (exportComplete) stringResource(R.string.exported) else stringResource(
                        R.string.exporting
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Column(verticalArrangement = Arrangement.SpaceBetween) {
                    LinearProgressIndicator(
                        modifier = Modifier.padding(vertical = 4.dp),
                        progress = animatedProgress,
                        trackColor = MaterialTheme.colorScheme.inversePrimary
                    )
                    Text(
                        modifier = Modifier.padding(vertical = 4.dp),
                        text = "${(exportProgress * 100).toInt()}%"
                    )
                }
                TextButton(
                    onClick = {
                        if (!exportComplete) {
                            transformManager.cancel()
                        }
                        onDismissRequest()
                    },
                ) {
                    Text(if (exportComplete) stringResource(R.string.dismiss) else stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
fun ExportFailedAlertDialog(exception: ExportException?, onDismissRequest: () -> Unit) {
    AlertDialog(
        title = {
            Text(text = stringResource(R.string.error))
        },
        text = {
            Text(text = exception.toString())
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {

        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}

private const val PLAYER_SEEK_BACK_INCREMENT = 5 * 1000L // 5 seconds
private const val PLAYER_SEEK_FORWARD_INCREMENT = 10 * 1000L // 10 seconds
private const val REFRESH_RATE = 100L // 100 milliseconds
