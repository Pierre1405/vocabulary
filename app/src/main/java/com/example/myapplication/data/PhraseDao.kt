package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrases")
    suspend fun getAllPhrases(): List<Phrase>
}
