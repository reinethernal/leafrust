package com.leafrust.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspections ORDER BY id DESC")
    fun observeAll(): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections ORDER BY id DESC")
    suspend fun getAll(): List<InspectionEntity>

    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun getById(id: Long): InspectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InspectionEntity): Long

    @Query("DELETE FROM inspections WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE inspections SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)
}
