package com.snimesh.baby_feed.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.snimesh.baby_feed.ui.theme.BabyfeedTheme
import com.snimesh.baby_feed.ui.theme.MutedText
import com.snimesh.baby_feed.ui.theme.SurfaceVariant
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

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = "📜 History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No feeds logged yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedText,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariant)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatForDisplay(entry.timestampMillis),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateEntry(
                                            entry.copy(
                                                timestampMillis = entry.timestampMillis - TimeUnit.MINUTES.toMillis(15),
                                            ),
                                        )
                                    },
                                ) { Text("-15m") }
                                Button(
                                    onClick = { viewModel.deleteEntry(entry) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    ),
                                ) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatForDisplay(millis: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(millis))
