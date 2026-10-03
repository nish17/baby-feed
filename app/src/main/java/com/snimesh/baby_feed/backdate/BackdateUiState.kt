package com.snimesh.baby_feed.backdate

/** See the plan's High-Level Technical Design for the full state diagram this mirrors. */
sealed class BackdateUiState {
    data object Idle : BackdateUiState()
    data object Listening : BackdateUiState()
    data object Parsing : BackdateUiState()
    data class ManualPicker(val nowMillis: Long) : BackdateUiState()
    data class Confirming(val timestampMillis: Long) : BackdateUiState()
}
