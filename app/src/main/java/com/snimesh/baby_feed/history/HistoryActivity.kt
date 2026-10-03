package com.snimesh.baby_feed.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snimesh.baby_feed.ui.theme.BabyfeedTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BabyfeedTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: HistoryViewModel = viewModel(
                        factory = HistoryViewModel.factory(applicationContext),
                    )
                    HistoryScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.entries.collectAsState()

    if (entries.isEmpty()) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("No feeds logged yet.")
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        items(entries, key = { it.id }) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatForDisplay(entry.timestampMillis))
                Row {
                    Button(
                        onClick = {
                            viewModel.updateEntry(
                                entry.copy(
                                    timestampMillis = entry.timestampMillis - TimeUnit.MINUTES.toMillis(15),
                                ),
                            )
                        },
                    ) { Text("-15m") }
                    Button(onClick = { viewModel.deleteEntry(entry) }) { Text("Delete") }
                }
            }
        }
    }
}

private fun formatForDisplay(millis: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(millis))
