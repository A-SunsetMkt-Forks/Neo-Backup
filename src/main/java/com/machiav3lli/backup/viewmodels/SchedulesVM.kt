/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.viewmodels

import androidx.lifecycle.viewModelScope
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.dbs.repository.ScheduleRepository
import com.machiav3lli.backup.utils.extensions.NeoViewModel
import kotlinx.coroutines.launch

class SchedulesVM(
    private val scheduleRepository: ScheduleRepository,
) : NeoViewModel() {
    val schedules = scheduleRepository.getAllFlow()

    fun addSchedule(withSpecial: Boolean) {
        viewModelScope.launch {
            scheduleRepository.addNew(withSpecial)
        }
    }

    fun updateSchedule(schedule: Schedule?, rescheduleBoolean: Boolean) {
        viewModelScope.launch {
            schedule?.let { scheduleRepository.updateSchedule(it, rescheduleBoolean) }
        }
    }
}
