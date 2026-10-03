package com.snimesh.baby_feed.backdate

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snimesh.baby_feed.history.HistoryActivity
import com.snimesh.baby_feed.settings.SettingsActivity
import com.snimesh.baby_feed.ui.theme.BabyfeedTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * The app's LAUNCHER activity (see Key Technical Decisions) and the hub for backdating plus
 * navigation to History/Settings. Only renders state -- BackdateViewModel is the sole
 * orchestrator.
 */
class BackdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BabyfeedTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: BackdateViewModel = viewModel(
                        factory = BackdateViewModel.factory(applicationContext),
                    )
                    BackdateScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun BackdateScreen(viewModel: BackdateViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var typedText by remember { mutableStateOf("") }

    val recordAudioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onSpeakClicked() else viewModel.onManualEntryClicked()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val state = uiState) {
            is BackdateUiState.Idle -> {
                Text("Forgot to log a feed? Tell me when it happened.")
                Button(onClick = { recordAudioPermission.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Speak")
                }
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    label = { Text("e.g. \"she fed at 1:30\"") },
                )
                Button(onClick = { viewModel.onTypedTextSubmitted(typedText) }) {
                    Text("Submit typed time")
                }
                Button(onClick = { viewModel.onManualEntryClicked() }) {
                    Text("Enter manually")
                }
                Button(onClick = { context.startActivity(Intent(context, HistoryActivity::class.java)) }) {
                    Text("History")
                }
                Button(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                    Text("Settings")
                }
            }

            is BackdateUiState.Listening -> Text("Listening…")

            is BackdateUiState.Parsing -> Text("Thinking…")

            is BackdateUiState.ManualPicker -> {
                Text("How long ago was she fed?")
                for (minutesAgo in listOf(0, 15, 30, 60, 120, 180)) {
                    Button(
                        onClick = {
                            val millis = state.nowMillis - TimeUnit.MINUTES.toMillis(minutesAgo.toLong())
                            viewModel.onManualTimeSelected(millis)
                        },
                    ) {
                        Text(if (minutesAgo == 0) "Just now" else "$minutesAgo minutes ago")
                    }
                }
            }

            is BackdateUiState.Confirming -> {
                Text("Logged: fed at ${formatForDisplay(state.timestampMillis)}")
                Button(onClick = { viewModel.onConfirm(state.timestampMillis) }) { Text("Confirm") }
                Button(onClick = { viewModel.onUndo() }) { Text("Undo") }
            }
        }
    }
}

private fun formatForDisplay(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.US).format(Date(millis))
