package io.github.devhyper.openvideoeditor.settings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.devhyper.openvideoeditor.R
import io.github.devhyper.openvideoeditor.misc.DropdownSetting
import io.github.devhyper.openvideoeditor.misc.SwitchSetting
import io.github.devhyper.openvideoeditor.misc.move
import io.github.devhyper.openvideoeditor.ui.theme.OpenVideoEditorTheme
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val dataStore = remember { SettingsDataStore(context) }
    OpenVideoEditorTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.settings),
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { activity.finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        }
                    )
                }, content = { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 32.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    )
                    {
                        item {
                            val theme = dataStore.getThemeBlocking()
                            val options = mutableListOf("System", "Light", "Dark")
                            options.move(theme, 0)
                            SettingRow(
                                name = stringResource(R.string.theme)
                            ) {
                                DropdownSetting(
                                    name = stringResource(R.string.theme),
                                    options = options.toImmutableList(),
                                    onSelectionChanged = {
                                        if (it != theme) {
                                            scope.launch {
                                                dataStore.setTheme(it)
                                            }
                                        }
                                    })
                            }
                        }
                        item {
                            val useLegacyFilePicker = dataStore.getLegacyFilePickerBlocking()
                            SwitchSetting(
                                name = stringResource(R.string.use_legacy_file_picker),
                                startChecked = useLegacyFilePicker,
                                onCheckChanged = {
                                    if (it != useLegacyFilePicker) {
                                        scope.launch {
                                            dataStore.setLegacyFilePicker(it)
                                        }
                                    }
                                })
                        }
                        item {
                            val useUiCascadingEffect = dataStore.getUiCascadingEffectBlocking()
                            SwitchSetting(
                                name = stringResource(R.string.use_ui_cascading_effect),
                                startChecked = useUiCascadingEffect,
                                onCheckChanged = {
                                    if (it != useUiCascadingEffect) {
                                        scope.launch {
                                            dataStore.setUiCascadingEffect(it)
                                        }
                                    }
                                })
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SettingRow(name: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(modifier = Modifier.padding(end = 16.dp), text = name, textAlign = TextAlign.Center)
        value()
    }
}
