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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snimesh.baby_feed.history.HistoryActivity
import com.snimesh.baby_feed.settings.SettingsActivity
import com.snimesh.baby_feed.ui.theme.BabyfeedTheme
import com.snimesh.baby_feed.ui.theme.MutedText
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
private fun ScreenHeader() {
    Text(
        text = "🍼 Baby Feed",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun CenteredStatus(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = text, style = MaterialTheme.typography.titleMedium)
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

    when (val state = uiState) {
        is BackdateUiState.Idle -> {
            Column(
                modifier = modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScreenHeader()

                Text(
                    text = "Forgot to log a feed? Tell me when it happened.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedText,
                )

                Button(
                    onClick = { recordAudioPermission.launch(Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("🎙️  Speak", fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    label = { Text("e.g. \"she fed at 1:30\"") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = { viewModel.onTypedTextSubmitted(typedText) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Submit typed time")
                }
                TextButton(
                    onClick = { viewModel.onManualEntryClicked() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Enter manually")
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(context, HistoryActivity::class.java)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("📜 History")
                    }
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("⚙️ Settings")
                    }
                }
            }
        }

        is BackdateUiState.Listening -> CenteredStatus("Listening…")

        is BackdateUiState.Parsing -> CenteredStatus("Thinking…")

        is BackdateUiState.ManualPicker -> {
            Column(
                modifier = modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScreenHeader()
                Text(
                    text = "How long ago was she fed?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MutedText,
                )
                for (minutesAgo in listOf(0, 15, 30, 60, 120, 180)) {
                    OutlinedButton(
                        onClick = {
                            val millis = state.nowMillis - TimeUnit.MINUTES.toMillis(minutesAgo.toLong())
                            viewModel.onManualTimeSelected(millis)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (minutesAgo == 0) "Just now" else "$minutesAgo minutes ago")
                    }
                }
            }
        }

        is BackdateUiState.Confirming -> {
            Column(
                modifier = modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Logged: fed at ${formatForDisplay(state.timestampMillis)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.onConfirm(state.timestampMillis) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.onUndo() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Undo")
                }
            }
        }
    }
}

private fun formatForDisplay(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.US).format(Date(millis))
