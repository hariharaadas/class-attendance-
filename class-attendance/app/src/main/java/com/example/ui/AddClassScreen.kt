package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddClassScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onStartAttendance: () -> Unit
) {
    val defaultDate = remember {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())
    }

    var date by remember { mutableStateOf(defaultDate) }
    var classSection by remember { mutableStateOf("IT-A") }
    var subject by remember { mutableStateOf("Database Management Systems") }
    var courseCode by remember { mutableStateOf("CS301") }
    var hour by remember { mutableStateOf("2nd Hour") }
    var notes by remember { mutableStateOf("Regular class") }

    var validationError by remember { mutableStateOf<String?>(null) }

    val commonClasses = listOf("IT-A", "IT-B", "CSE-A", "CSE-B", "ECE-A", "MECH-A")
    val commonHours = listOf("1st Hour", "2nd Hour", "3rd Hour", "4th Hour", "5th Hour", "6th Hour")
    val commonSubjects = listOf(
        "Database Management Systems" to "CS301",
        "Object Oriented Programming" to "CS204",
        "Computer Networks" to "IT402",
        "Data Structures & Algorithms" to "CS201",
        "Operating Systems" to "CS303"
    )
    val commonNotes = listOf("Regular class", "Internal test", "Lab session", "Tutorial", "Revision")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Class",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_class_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Class Session Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Date Input
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        placeholder = { Text("e.g. 29 Sep 2026") },
                        leadingIcon = { Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_session_date")
                    )

                    // Class / Section
                    Column {
                        OutlinedTextField(
                            value = classSection,
                            onValueChange = { classSection = it },
                            label = { Text("Class / Section") },
                            placeholder = { Text("e.g. IT-A, CSE-B") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Class, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_class_section")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            commonClasses.forEach { cls ->
                                FilterChip(
                                    selected = classSection == cls,
                                    onClick = { classSection = cls },
                                    label = { Text(cls) }
                                )
                            }
                        }
                    }

                    // Subject
                    Column {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject / Course") },
                            placeholder = { Text("e.g. Database Management Systems") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Book, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_subject")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            commonSubjects.forEach { (sub, code) ->
                                FilterChip(
                                    selected = subject == sub,
                                    onClick = {
                                        subject = sub
                                        if (courseCode.isEmpty() || commonSubjects.any { it.second == courseCode }) {
                                            courseCode = code
                                        }
                                    },
                                    label = { Text(sub) }
                                )
                            }
                        }
                    }

                    // Course Code
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course Code (Optional)") },
                        placeholder = { Text("e.g. CS301") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Code, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_course_code")
                    )

                    // Hour / Period
                    Column {
                        Text(
                            text = "Hour / Period",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonHours.forEach { hr ->
                                FilterChip(
                                    selected = hour == hr,
                                    onClick = { hour = hr },
                                    label = { Text(hr) },
                                    leadingIcon = {
                                        if (hour == hr) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Notes
                    Column {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (Optional)") },
                            placeholder = { Text("e.g. Regular class, Internal test, Lab session") },
                            leadingIcon = { Icon(imageVector = Icons.Default.EditNote, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_notes")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            commonNotes.forEach { n ->
                                FilterChip(
                                    selected = notes == n,
                                    onClick = { notes = n },
                                    label = { Text(n) }
                                )
                            }
                        }
                    }

                    // Error Message
                    if (validationError != null) {
                        Text(
                            text = validationError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("validation_error_text")
                        )
                    }
                }
            }

            // Start Attendance Action Button
            Button(
                onClick = {
                    if (classSection.isBlank()) {
                        validationError = "Please enter or select a class / section"
                    } else if (subject.isBlank()) {
                        validationError = "Please enter or select a subject"
                    } else if (hour.isBlank()) {
                        validationError = "Please select an hour / period"
                    } else {
                        validationError = null
                        viewModel.startNewSession(
                            date = date,
                            classSection = classSection,
                            subject = subject,
                            courseCode = courseCode,
                            hour = hour,
                            notes = notes
                        )
                        onStartAttendance()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_start_attendance"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Start Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
