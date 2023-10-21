package io.github.devhyper.openvideoeditor.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.devhyper.openvideoeditor.misc.setImmersiveMode
import io.github.devhyper.openvideoeditor.misc.setupSystemUi

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSystemUi()

        setContent {
            setImmersiveMode(false)
            SettingsScreen()
        }
    }
}
