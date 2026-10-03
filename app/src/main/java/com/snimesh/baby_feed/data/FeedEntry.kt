package com.snimesh.baby_feed.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single feeding event. Time only, per the origin requirements doc's scope boundary
 * (no amount, type, duration, or side tracking).
 */
@Entity(tableName = "feed_entries")
data class FeedEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long,
)
