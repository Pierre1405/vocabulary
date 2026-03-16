package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "story_category",
    primaryKeys = ["story_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = ["id"],
            childColumns = ["story_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StoryCategory(
    val story_id: Int,
    val category_id: Int
)
