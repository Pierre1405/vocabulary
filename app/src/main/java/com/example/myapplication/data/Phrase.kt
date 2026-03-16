package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "phrases",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Story::class,
            parentColumns = ["id"],
            childColumns = ["story_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Phrase(
    @PrimaryKey val id: Int,
    val francais: String,
    val allemand: String,
    val category_id: Int,
    val story_id: Int,
    val apprise: Boolean = false
)
