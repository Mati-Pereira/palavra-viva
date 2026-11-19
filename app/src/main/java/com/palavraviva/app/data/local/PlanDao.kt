package com.palavraviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<ReadPlan>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<PlanDay>)

    @Query("SELECT * FROM ReadPlan")
    fun plans(): Flow<List<ReadPlan>>

    @Query("SELECT * FROM PlanDay WHERE planId = :planId ORDER BY dayNumber")
    fun days(planId: String): Flow<List<PlanDay>>

    @Query("SELECT * FROM PlanDay WHERE planId = :planId AND dayNumber = :dayNumber")
    suspend fun day(planId: String, dayNumber: Int): PlanDay?
}
