package io.github.devhyper.openvideoeditor.videoeditor

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import io.github.devhyper.openvideoeditor.misc.PROJECT_MIME_TYPE
import io.github.devhyper.openvideoeditor.misc.setImmersiveMode
import io.github.devhyper.openvideoeditor.misc.setupSystemUi
import org.objenesis.strategy.StdInstantiatorStrategy


class VideoEditorActivity : ComponentActivity() {
    private lateinit var createDocument: ActivityResultLauncher<String?>
    private lateinit var createProject: ActivityResultLauncher<String?>
    private lateinit var viewModel: VideoEditorViewModel
    private lateinit var kryo: Kryo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSystemUi()

        viewModel = VideoEditorViewModel()

        window.decorView.setOnSystemUiVisibilityChangeListener {
            viewModel.setControlsVisible(it == 0)
        }

        createDocument = registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "video/mp4"
            )
        ) { uri ->
            if (uri != null) {
                viewModel.setOutputPath(uri.toString())
            }
        }
        createProject = registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                PROJECT_MIME_TYPE
            )
        ) { uri ->
            if (uri != null) {
                viewModel.setProjectOutputPath(uri.toString())
            }
        }

        kryo = Kryo()
        kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())
        kryo.isRegistrationRequired = false

        var uri: String? = null
        if (intent.action == Intent.ACTION_EDIT) {
            intent.dataString?.let {
                uri = it
            }
        } else if (intent.action == Intent.ACTION_SEND) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                (intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))?.let {
                    uri = it.toString()
                }
            } else {
                @Suppress("DEPRECATION")
                (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let {
                    uri = it.toString()
                }
            }
        }

        uri?.let {
            setContent {
                viewModel = viewModel { viewModel }
                val controlsVisible by viewModel.controlsVisible.collectAsState()
                setImmersiveMode(!controlsVisible)
                VideoEditorScreen(it, createDocument, createProject, kryo)
            }
        } ?: finish()
    }
}
