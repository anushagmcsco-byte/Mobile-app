package com.example.data.dao

import androidx.room.*
import com.example.data.entity.ModuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules WHERE courseId = :courseId ORDER BY moduleOrder ASC")
    fun getModulesForCourse(courseId: Int): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules WHERE courseId = :courseId ORDER BY moduleOrder ASC")
    suspend fun getModulesForCourseDirect(courseId: Int): List<ModuleEntity>

    @Query("SELECT * FROM modules WHERE id = :id")
    fun getModuleById(id: Int): Flow<ModuleEntity?>

    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getModuleByIdDirect(id: Int): ModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: ModuleEntity): Long

    @Update
    suspend fun updateModule(module: ModuleEntity)

    @Query("DELETE FROM modules WHERE id = :id")
    suspend fun deleteModuleById(id: Int)

    @Query("SELECT COUNT(*) FROM modules WHERE courseId = :courseId")
    suspend fun getModuleCountForCourse(courseId: Int): Int
}
