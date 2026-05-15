package com.nimmaguru.app.feature.onboarding.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R
import com.nimmaguru.app.core.common.ImageCompressor
import com.nimmaguru.app.core.ui.components.GuruAvatar
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme
import kotlinx.coroutines.launch

/**
 * Skill option keyed to its `@StringRes` label so the chip text
 * follows the active locale.
 */
private data class SkillOption(val key: String, val labelRes: Int)

private val SKILL_OPTIONS = listOf(
    SkillOption("Math", R.string.skill_math),
    SkillOption("Science", R.string.skill_science),
    SkillOption("English", R.string.skill_english),
    SkillOption("Kannada", R.string.skill_kannada),
    SkillOption("Physics", R.string.skill_physics),
    SkillOption("History", R.string.skill_history),
    SkillOption("Carpentry", R.string.skill_carpentry),
    SkillOption("Music", R.string.skill_music),
    SkillOption("Art", R.string.skill_art),
    SkillOption("Chess", R.string.skill_chess),
    SkillOption("Yoga", R.string.skill_yoga),
    SkillOption("Cooking", R.string.skill_cooking),
)

private data class DayOption(val key: String, val labelRes: Int)

private val DAY_OPTIONS = listOf(
    DayOption("Mon", R.string.day_mon),
    DayOption("Tue", R.string.day_tue),
    DayOption("Wed", R.string.day_wed),
    DayOption("Thu", R.string.day_thu),
    DayOption("Fri", R.string.day_fri),
    DayOption("Sat", R.string.day_sat),
    DayOption("Sun", R.string.day_sun),
)

private data class TimeSlotOption(val key: String, val labelRes: Int)

private val TIME_SLOT_OPTIONS = listOf(
    TimeSlotOption("9:00-11:00", R.string.time_slot_morning_early),
    TimeSlotOption("11:00-13:00", R.string.time_slot_morning_late),
    TimeSlotOption("14:00-16:00", R.string.time_slot_afternoon),
    TimeSlotOption("16:00-18:00", R.string.time_slot_evening),
)

@Composable
fun GuruOnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Image picker — Photo Picker API
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val dataUrl = ImageCompressor.toBase64DataUrl(context, uri)
                if (dataUrl != null) {
                    viewModel.onAction(OnboardingAction.PhotoSelected(uri.toString(), dataUrl))
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.error_image_too_large))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingEvent.NavigateToHome -> onNavigateToHome()
                is OnboardingEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    when (val state = uiState) {
        is OnboardingUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is OnboardingUiState.Form -> {
            OnboardingContent(
                modifier = modifier,
                formState = state.formState,
                onAction = viewModel::onAction,
                onPickPhoto = {
                    photoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onBack = onBack,
                snackbarHostState = snackbarHostState,
            )
        }
        is OnboardingUiState.Published -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.profile_published),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator()
                }
            }
        }
        is OnboardingUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(state.messageRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onBack) {
                        Text(text = stringResource(R.string.back_button))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingContent(
    modifier: Modifier = Modifier,
    formState: OnboardingFormState = OnboardingFormState(),
    onAction: (OnboardingAction) -> Unit = {},
    onPickPhoto: () -> Unit = {},
    onBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.create_guru_profile),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (formState.currentStep > 1) onAction(OnboardingAction.PreviousStep)
                        else onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_navigation_desc),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
        ) {
            LinearProgressIndicator(
                progress = { formState.currentStep / formState.totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.step_of, formState.currentStep, formState.totalSteps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            formState.errorRes?.let { errorRes ->
                Text(
                    text = stringResource(errorRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            AnimatedContent(
                targetState = formState.currentStep,
                modifier = Modifier.weight(1f),
                label = "step_transition",
            ) { step ->
                when (step) {
                    1 -> Step1BasicInfo(
                        formState = formState,
                        onAction = onAction,
                        onPickPhoto = onPickPhoto,
                    )
                    2 -> Step2Skills(formState = formState, onAction = onAction)
                    3 -> Step3Availability(formState = formState, onAction = onAction)
                    4 -> Step4Bio(formState = formState, onAction = onAction)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (formState.currentStep < 4) {
                Button(
                    onClick = { onAction(OnboardingAction.NextStep) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = stringResource(R.string.next_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Button(
                    onClick = { onAction(OnboardingAction.PublishProfile) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !formState.isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.publish_profile),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ═══════════════════════════════════
//  STEP 1 — Basic Info
// ═══════════════════════════════════

@Composable
fun Step1BasicInfo(
    formState: OnboardingFormState,
    onAction: (OnboardingAction) -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        // Photo: shows current avatar (or initials placeholder) and triggers picker.
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val photoSource = formState.photoDataUrl ?: formState.photoUri.orEmpty()
            if (photoSource.isNotBlank()) {
                GuruAvatar(
                    name = formState.nameEn.ifBlank { "?" },
                    photoUrl = photoSource,
                    size = 120.dp,
                    contentDescription = stringResource(R.string.cd_add_photo),
                )
            } else {
                OutlinedButton(
                    onClick = onPickPhoto,
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = stringResource(R.string.cd_add_photo),
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = stringResource(R.string.add_photo),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        if (formState.photoDataUrl != null || !formState.photoUri.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onPickPhoto,
                    modifier = Modifier.align(Alignment.Center).height(48.dp),
                ) {
                    Text(stringResource(R.string.add_photo))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = formState.nameEn,
            onValueChange = { onAction(OnboardingAction.NameEnChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.name_english)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formState.nameKn,
            onValueChange = { onAction(OnboardingAction.NameKnChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.name_kannada)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formState.village,
            onValueChange = { onAction(OnboardingAction.VillageChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.village_label)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formState.district,
            onValueChange = { onAction(OnboardingAction.DistrictChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.district_label)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

// ═══════════════════════════════════
//  STEP 2 — Select Skills
// ═══════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step2Skills(
    formState: OnboardingFormState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.select_skills),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SKILL_OPTIONS.forEach { skill ->
                val isSelected = formState.selectedSkills.contains(skill.key)
                FilterChip(
                    selected = isSelected,
                    onClick = { onAction(OnboardingAction.SkillToggled(skill.key)) },
                    label = {
                        Text(
                            text = stringResource(skill.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    modifier = Modifier.height(48.dp), // R-COMP-03
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

// ═══════════════════════════════════
//  STEP 3 — Set Availability
// ═══════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3Availability(
    formState: OnboardingFormState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.select_availability),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        DAY_OPTIONS.forEach { day ->
            Text(
                text = stringResource(day.labelRes),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TIME_SLOT_OPTIONS.forEach { slot ->
                    val isSelected = formState.availability[day.key] == slot.key
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) onAction(OnboardingAction.AvailabilityRemoved(day.key))
                            else onAction(OnboardingAction.AvailabilitySet(day.key, slot.key))
                        },
                        label = {
                            Text(
                                text = stringResource(slot.labelRes),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        modifier = Modifier.height(48.dp), // R-COMP-03
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ═══════════════════════════════════
//  STEP 4 — AI Bio + Review
// ═══════════════════════════════════

@Composable
fun Step4Bio(
    formState: OnboardingFormState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.ai_bio_title),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onAction(OnboardingAction.GenerateBio) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            enabled = !formState.isBioGenerating,
        ) {
            if (formState.isBioGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (formState.bioEn.isBlank()) stringResource(R.string.generate_bio)
                    else stringResource(R.string.regenerate_bio),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formState.bioEn,
            onValueChange = { onAction(OnboardingAction.BioEdited(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            label = { Text(stringResource(R.string.name_english)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = formState.bioKn,
            onValueChange = { onAction(OnboardingAction.BioKnEdited(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            label = { Text(stringResource(R.string.name_kannada)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.review_publish),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (formState.nameEn.isNotBlank()) {
            Text(
                text = stringResource(R.string.review_name, formState.nameEn),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (formState.village.isNotBlank()) {
            Text(
                text = stringResource(R.string.review_village, formState.village),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        // Pre-resolve labels at composable scope so non-composable lambdas (map/joinToString) can use them.
        val skillLabelMap = SKILL_OPTIONS.associate { it.key to stringResource(it.labelRes) }
        val dayLabelMap = DAY_OPTIONS.associate { it.key to stringResource(it.labelRes) }
        val timeSlotLabelMap = TIME_SLOT_OPTIONS.associate { it.key to stringResource(it.labelRes) }

        if (formState.selectedSkills.isNotEmpty()) {
            val skillLabels = formState.selectedSkills.map { key ->
                skillLabelMap[key] ?: key
            }
            Text(
                text = stringResource(R.string.review_skills, skillLabels.joinToString(", ")),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (formState.availability.isNotEmpty()) {
            val avail = formState.availability.entries.joinToString(", ") { (dayKey, slotKey) ->
                val dayLabel = dayLabelMap[dayKey] ?: dayKey
                val slotLabel = timeSlotLabelMap[slotKey] ?: slotKey
                "$dayLabel: $slotLabel"
            }
            Text(
                text = stringResource(R.string.review_availability, avail),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Step1() {
    NimmaGuruTheme {
        OnboardingContent(formState = OnboardingFormState(currentStep = 1))
    }
}
