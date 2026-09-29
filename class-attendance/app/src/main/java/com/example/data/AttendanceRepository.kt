package com.example.data

import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val attendanceDao: AttendanceDao) {

    val allSessions: Flow<List<AttendanceSession>> = attendanceDao.getAllSessions()

    fun getSessionById(id: Long): Flow<AttendanceSession?> = attendanceDao.getSessionById(id)

    suspend fun saveSession(session: AttendanceSession): Long {
        return attendanceDao.insertSession(session)
    }

    suspend fun deleteSession(id: Long) {
        attendanceDao.deleteSessionById(id)
    }
}
