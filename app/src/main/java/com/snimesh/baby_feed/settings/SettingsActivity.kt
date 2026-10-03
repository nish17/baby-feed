package com.snimesh.baby_feed.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snimesh.baby_feed.ui.theme.BabyfeedTheme
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

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Feed every:")
        for (hours in INTERVAL_OPTIONS_HOURS) {
            val optionMillis = TimeUnit.HOURS.toMillis(hours.toLong())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = intervalMillis == optionMillis,
                        onClick = { viewModel.setIntervalMillis(optionMillis) },
                    )
                    .padding(vertical = 8.dp),
            ) {
                RadioButton(
                    selected = intervalMillis == optionMillis,
                    onClick = { viewModel.setIntervalMillis(optionMillis) },
                )
                Text("$hours hours")
            }
        }
    }
}
