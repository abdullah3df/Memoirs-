package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val thumbnailUrl: String = "",
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "ar"
) : Serializable
