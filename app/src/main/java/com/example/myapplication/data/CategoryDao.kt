package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category")
    suspend fun getAllCategories(): List<Category>
}
