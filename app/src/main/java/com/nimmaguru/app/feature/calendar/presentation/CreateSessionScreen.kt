package com.nimmaguru.app.feature.calendar.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateSessionViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateSessionEvent.NavigateBack -> onBack()
                is CreateSessionEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.create_session_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_navigation_desc),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val form = (uiState as? CreateSessionUiState.Form)?.form ?: CreateSessionFormState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = form.subject,
                onValueChange = { viewModel.onAction(CreateSessionAction.SubjectChanged(it)) },
                label = { Text(stringResource(R.string.session_subject_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = form.description,
                onValueChange = { viewModel.onAction(CreateSessionAction.DescriptionChanged(it)) },
                label = { Text(stringResource(R.string.session_description_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = form.venue,
                onValueChange = { viewModel.onAction(CreateSessionAction.VenueChanged(it)) },
                label = { Text(stringResource(R.string.session_venue_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = form.dateText,
                onValueChange = { viewModel.onAction(CreateSessionAction.DateChanged(it)) },
                label = { Text(stringResource(R.string.session_date_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("2026-05-15") },
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = form.startTime,
                onValueChange = { viewModel.onAction(CreateSessionAction.StartTimeChanged(it)) },
                label = { Text(stringResource(R.string.session_start_time_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = form.endTime,
                onValueChange = { viewModel.onAction(CreateSessionAction.EndTimeChanged(it)) },
                label = { Text(stringResource(R.string.session_end_time_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = form.maxStudents,
                onValueChange = { viewModel.onAction(CreateSessionAction.MaxStudentsChanged(it)) },
                label = { Text(stringResource(R.string.session_max_students_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            form.errorRes?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.onAction(CreateSessionAction.Submit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !form.isSubmitting,
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.session_create_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
