package com.palavraviva.app.data.repositories

import com.palavraviva.app.data.local.AppDatabase
import com.palavraviva.app.data.local.PlanDay
import com.palavraviva.app.data.local.ReadPlan
import kotlinx.coroutines.flow.Flow

/** Acesso aos planos de leitura. */
class PlanRepository(private val database: AppDatabase) {

    private val planDao = database.planDao()

    fun plans(): Flow<List<ReadPlan>> = planDao.plans()

    fun days(planId: String): Flow<List<PlanDay>> = planDao.days(planId)

    suspend fun day(planId: String, dayNumber: Int): PlanDay? =
        planDao.day(planId, dayNumber)
}
