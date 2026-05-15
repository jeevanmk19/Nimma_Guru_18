package com.nimmaguru.app.feature.onboarding.presentation

import androidx.annotation.StringRes

/**
 * Guru Onboarding state — 4-step wizard.
 * From idea-screens.md Screen 9.
 *
 * R-KT-06: Sealed hierarchy for UI state.
 */
data class OnboardingFormState(
    val currentStep: Int = 1,
    val totalSteps: Int = 4,
    // Step 1 — Basic Info
    val photoUri: String? = null,
    /** When the user picks a photo we encode bytes to base64 once and cache it here. */
    val photoDataUrl: String? = null,
    val nameEn: String = "",
    val nameKn: String = "",
    val village: String = "",
    val district: String = "",
    // Step 2 — Skills
    val selectedSkills: List<String> = emptyList(),
    // Step 3 — Availability
    val availability: Map<String, String> = emptyMap(),
    // Step 4 — Bio
    val bioEn: String = "",
    val bioKn: String = "",
    val isBioGenerating: Boolean = false,
    // Overall
    val isSubmitting: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState
    data class Form(val formState: OnboardingFormState) : OnboardingUiState
    data object Published : OnboardingUiState
    data class Error(@StringRes val messageRes: Int) : OnboardingUiState
}

sealed interface OnboardingEvent {
    data object NavigateToHome : OnboardingEvent
    data class ShowSnackbar(@StringRes val messageRes: Int) : OnboardingEvent
}

sealed interface OnboardingAction {
    // Navigation
    data object NextStep : OnboardingAction
    data object PreviousStep : OnboardingAction

    // Step 1 — Basic Info
    data class NameEnChanged(val name: String) : OnboardingAction
    data class NameKnChanged(val name: String) : OnboardingAction
    data class VillageChanged(val village: String) : OnboardingAction
    data class DistrictChanged(val district: String) : OnboardingAction
    /** [photoDataUrl] is a `data:image/jpeg;base64,...` string ready to persist. */
    data class PhotoSelected(val photoUri: String, val photoDataUrl: String) : OnboardingAction

    // Step 2 — Skills
    data class SkillToggled(val skill: String) : OnboardingAction

    // Step 3 — Availability
    data class AvailabilitySet(val day: String, val timeSlot: String) : OnboardingAction
    data class AvailabilityRemoved(val day: String) : OnboardingAction

    // Step 4 — Bio
    data object GenerateBio : OnboardingAction
    data class BioEdited(val bio: String) : OnboardingAction
    data class BioKnEdited(val bio: String) : OnboardingAction

    // Final
    data object PublishProfile : OnboardingAction
}
