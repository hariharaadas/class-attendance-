package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a saved college classroom attendance session.
 */
@Entity(tableName = "attendance_sessions")
data class AttendanceSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,             // e.g. "29 Sep 2026"
    val timestamp: Long,          // Epoch timestamp for sorting
    val classSection: String,     // e.g. "IT-A"
    val subject: String,          // e.g. "Database Management Systems"
    val courseCode: String,       // e.g. "CS301"
    val hour: String,             // e.g. "2nd Hour"
    val presentRollNumbersJson: String, // JSON or comma-separated roll numbers
    val presentCount: Int,
    val notes: String = ""        // e.g. "Internal test", "Regular class"
)
