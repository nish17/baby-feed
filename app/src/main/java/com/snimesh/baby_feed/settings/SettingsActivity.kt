package com.snimesh.baby_feed.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snimesh.baby_feed.ui.theme.AccentColor
import com.snimesh.baby_feed.ui.theme.BabyfeedTheme
import com.snimesh.baby_feed.ui.theme.MutedText
import com.snimesh.baby_feed.ui.theme.SurfaceVariant
import java.util.concurrent.TimeUnit

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BabyfeedTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.factory(applicationContext),
                    )
                    SettingsScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private val INTERVAL_OPTIONS_HOURS = listOf(2, 3, 4)

@Composable
private fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val intervalMillis by viewModel.intervalMillis.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = "⚙️ Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Feed every:",
            style = MaterialTheme.typography.bodyLarge,
            color = MutedText,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (hours in INTERVAL_OPTIONS_HOURS) {
                val optionMillis = TimeUnit.HOURS.toMillis(hours.toLong())
                val selected = intervalMillis == optionMillis
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariant)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                onClick = { viewModel.setIntervalMillis(optionMillis) },
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { viewModel.setIntervalMillis(optionMillis) },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentColor),
                        )
                        Text("$hours hours", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
