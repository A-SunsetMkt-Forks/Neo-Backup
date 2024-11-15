package com.machiav3lli.backup.dbs.repository

import android.app.Application
import com.machiav3lli.backup.dbs.Converters
import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.dbs.entity.Schedule
import com.machiav3lli.backup.preferences.traceSchedule
import com.machiav3lli.backup.tasks.ScheduleWork
import com.machiav3lli.backup.utils.scheduleNext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleRepository(
    private val db: ODatabase,
    private val appContext: Application,
) {
    fun getAllFlow() = db.getScheduleDao().getAllFlow()
        .flowOn(Dispatchers.IO)

    fun getAll() = db.getScheduleDao().getAll()

    fun getScheduleFlow(id: Flow<Long>) = id.flatMapLatest {
        db.getScheduleDao().getScheduleFlow(it)
    }.flowOn(Dispatchers.IO)

    fun getSchedule(id: Long) = db.getScheduleDao().getSchedule(id)

    fun getSchedule(name: String) = db.getScheduleDao().getSchedule(name)

    fun getCustomListFlow(id: Flow<Long>): Flow<Set<String>> = id.flatMapLatest {
        db.getScheduleDao()._getCustomListFlow(it)
            .map { string -> Converters().toStringSet(string) }
    }.flowOn(Dispatchers.IO)

    fun getBlockListFlow(id: Flow<Long>): Flow<Set<String>> = id.flatMapLatest {
        db.getScheduleDao()._getBlockListFlow(it)
            .map { string -> Converters().toStringSet(string) }
    }.flowOn(Dispatchers.IO)

    fun update(value: Schedule) {
        db.getScheduleDao().update(value)
    }

    suspend fun updateSchedule(schedule: Schedule, rescheduleBoolean: Boolean) {
        withContext(Dispatchers.IO) {
            update(schedule)
            if (schedule.enabled) {
                traceSchedule { "[$schedule.id] ScheduleViewModel.updateS -> ${if (rescheduleBoolean) "re-" else ""}schedule" }
                scheduleNext(
                    appContext,
                    schedule.id,
                    rescheduleBoolean
                )
            } else {
                traceSchedule { "[$schedule.id] ScheduleViewModel.updateS -> cancelAlarm" }
                ScheduleWork.cancel(appContext, schedule.id)
            }
        }
    }

    suspend fun deleteById(id: Long) {
        withContext(Dispatchers.IO) {
            db.getScheduleDao().deleteById(id)
        }
    }
}

