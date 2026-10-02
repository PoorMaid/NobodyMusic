package com.nobodymusic.tyxypoor.data.dao

import androidx.room.*
import com.nobodymusic.tyxypoor.data.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY priority ASC, updatedAt DESC")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE enabled = 1 ORDER BY priority ASC")
    suspend fun enabled(): List<SourceEntity>

    @Query("SELECT * FROM sources WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SourceEntity)

    @Query("UPDATE sources SET enabled = :on WHERE id = :id")
    suspend fun setEnabled(id: String, on: Boolean)

    @Query("UPDATE sources SET priority = :p WHERE id = :id")
    suspend fun setPriority(id: String, p: Int)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM sources ORDER BY priority ASC, updatedAt DESC")
    suspend fun allOnce(): List<SourceEntity>
}