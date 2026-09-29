package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.data.AttendanceSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ScanResult {
    data class Success(val rollNumber: String, val count: Int) : ScanResult()
    data class Duplicate(val rollNumber: String) : ScanResult()
    data class Invalid(val message: String) : ScanResult()
}

data class ActiveSessionState(
    val isActive: Boolean = false,
    val date: String = "",
    val timestamp: Long = 0L,
    val classSection: String = "",
    val subject: String = "",
    val courseCode: String = "",
    val hour: String = "",
    val notes: String = "",
    val presentRollNumbers: List<String> = emptyList(),
    val lastScannedRollNumber: String? = null,
    val lastScanResult: ScanResult? = null
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = AttendanceRepository(db.attendanceDao())
    }

    // Active attendance session state
    private val _activeSession = MutableStateFlow(ActiveSessionState())
    val activeSession: StateFlow<ActiveSessionState> = _activeSession.asStateFlow()

    // Search query for previous attendance sessions
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // All saved attendance sessions from Room database
    val savedSessions: StateFlow<List<AttendanceSession>> = repository.allSessions
        .combine(_searchQuery) { sessions, query ->
            if (query.isBlank()) {
                sessions
            } else {
                val q = query.trim().lowercase(Locale.getDefault())
                sessions.filter { session ->
                    session.classSection.lowercase(Locale.getDefault()).contains(q) ||
                    session.subject.lowercase(Locale.getDefault()).contains(q) ||
                    session.courseCode.lowercase(Locale.getDefault()).contains(q) ||
                    session.date.lowercase(Locale.getDefault()).contains(q) ||
                    session.hour.lowercase(Locale.getDefault()).contains(q) ||
                    session.notes.lowercase(Locale.getDefault()).contains(q) ||
                    session.presentRollNumbersJson.contains(q)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Initializes a new attendance session with user-selected class metadata.
     */
    fun startNewSession(
        date: String,
        classSection: String,
        subject: String,
        courseCode: String,
        hour: String,
        notes: String
    ) {
        _activeSession.value = ActiveSessionState(
            isActive = true,
            date = date.trim(),
            timestamp = System.currentTimeMillis(),
            classSection = classSection.trim(),
            subject = subject.trim(),
            courseCode = courseCode.trim().uppercase(Locale.getDefault()),
            hour = hour.trim(),
            notes = notes.trim(),
            presentRollNumbers = emptyList(),
            lastScannedRollNumber = null,
            lastScanResult = null
        )
    }

    /**
     * Processes a scanned or manually entered student roll number.
     * Enforces strict duplicate prevention: if already present, does NOT add again.
     */
    fun processRollNumber(raw: String): ScanResult {
        val cleanRoll = raw.trim()
        if (cleanRoll.isEmpty()) {
            val result = ScanResult.Invalid("Empty barcode value")
            _activeSession.update { it.copy(lastScanResult = result) }
            return result
        }

        val currentList = _activeSession.value.presentRollNumbers
        if (currentList.contains(cleanRoll)) {
            val result = ScanResult.Duplicate(cleanRoll)
            _activeSession.update {
                it.copy(
                    lastScannedRollNumber = cleanRoll,
                    lastScanResult = result
                )
            }
            return result
        }

        // Add to ordered list
        val updatedList = currentList + cleanRoll
        val result = ScanResult.Success(cleanRoll, updatedList.size)
        _activeSession.update {
            it.copy(
                presentRollNumbers = updatedList,
                lastScannedRollNumber = cleanRoll,
                lastScanResult = result
            )
        }
        return result
    }

    /**
     * Removes an accidentally added roll number from the current session.
     */
    fun removeRollNumber(rollNumber: String) {
        _activeSession.update { state ->
            val updated = state.presentRollNumbers.filter { it != rollNumber }
            state.copy(
                presentRollNumbers = updated,
                lastScannedRollNumber = if (state.lastScannedRollNumber == rollNumber) null else state.lastScannedRollNumber,
                lastScanResult = null
            )
        }
    }

    /**
     * Clears the current scan result banner so UI can revert to neutral state.
     */
    fun clearLastScanResult() {
        _activeSession.update { it.copy(lastScanResult = null) }
    }

    /**
     * Saves the completed attendance session into the local Room database.
     */
    fun saveActiveSession(onSaved: () -> Unit) {
        val state = _activeSession.value
        if (!state.isActive) return

        viewModelScope.launch {
            val sessionEntity = AttendanceSession(
                date = state.date.ifEmpty { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) },
                timestamp = if (state.timestamp > 0) state.timestamp else System.currentTimeMillis(),
                classSection = state.classSection,
                subject = state.subject,
                courseCode = state.courseCode,
                hour = state.hour,
                presentRollNumbersJson = state.presentRollNumbers.joinToString(","),
                presentCount = state.presentRollNumbers.size,
                notes = state.notes
            )

            repository.saveSession(sessionEntity)

            // Reset active session state
            _activeSession.value = ActiveSessionState()
            onSaved()
        }
    }

    fun discardActiveSession() {
        _activeSession.value = ActiveSessionState()
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}
