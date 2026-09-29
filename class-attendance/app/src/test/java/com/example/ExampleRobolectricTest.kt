package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Class Attendance", appName)
    }

    @Test
    fun `duplicate roll numbers are prevented`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = AttendanceViewModel(app)

        viewModel.startNewSession(
            date = "29 Sep 2026",
            classSection = "IT-A",
            subject = "Database Management Systems",
            courseCode = "CS301",
            hour = "2nd Hour",
            notes = "Regular class"
        )

        // First scan
        val result1 = viewModel.processRollNumber("251001091")
        assertTrue(result1 is ScanResult.Success)
        assertEquals(1, viewModel.activeSession.value.presentRollNumbers.size)

        // Second duplicate scan
        val result2 = viewModel.processRollNumber("251001091")
        assertTrue(result2 is ScanResult.Duplicate)
        assertEquals(1, viewModel.activeSession.value.presentRollNumbers.size)

        // Third duplicate scan
        val result3 = viewModel.processRollNumber("251001091")
        assertTrue(result3 is ScanResult.Duplicate)
        assertEquals(1, viewModel.activeSession.value.presentRollNumbers.size)

        // Distinct student scan
        val result4 = viewModel.processRollNumber("251001092")
        assertTrue(result4 is ScanResult.Success)
        assertEquals(2, viewModel.activeSession.value.presentRollNumbers.size)
    }
}
