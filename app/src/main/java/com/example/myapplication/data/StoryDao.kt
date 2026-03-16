package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface StoryDao {
    @Query("SELECT * FROM story")
    suspend fun getAllStories(): List<Story>
}
