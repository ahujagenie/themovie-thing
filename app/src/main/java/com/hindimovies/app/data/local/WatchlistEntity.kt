package com.hindimovies.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String,
    val channel: String,
    val youtubeId: String,
    val posterUrl: String,
    val year: Int,
    val duration: String,
    val rating: String,
    val starring: String = "",
    val description: String = "",
    val savedAt: Long = System.currentTimeMillis()
)
