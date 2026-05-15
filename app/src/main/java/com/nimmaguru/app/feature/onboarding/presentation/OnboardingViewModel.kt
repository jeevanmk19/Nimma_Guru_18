package com.nimmaguru.app.feature.onboarding.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.nimmaguru.app.R
import com.nimmaguru.app.core.common.ValidationResult
import com.nimmaguru.app.core.common.Validators
import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.auth.domain.repository.UserRepository
import com.nimmaguru.app.feature.profile.domain.repository.GuruRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val guruRepository: GuruRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(
        OnboardingUiState.Form(OnboardingFormState())
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    fun onAction(action: OnboardingAction) {
        val currentState = _uiState.value
        if (currentState !is OnboardingUiState.Form) return

        when (action) {
            is OnboardingAction.NextStep -> {
                if (validateCurrentStep(currentState.formState)) {
                    updateForm { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(4), errorRes = null) }
                }
            }
            is OnboardingAction.PreviousStep -> updateForm {
                it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1), errorRes = null)
            }

            // Step 1
            is OnboardingAction.NameEnChanged -> updateForm { it.copy(nameEn = action.name, errorRes = null) }
            is OnboardingAction.NameKnChanged -> updateForm { it.copy(nameKn = action.name, errorRes = null) }
            is OnboardingAction.VillageChanged -> updateForm { it.copy(village = action.village, errorRes = null) }
            is OnboardingAction.DistrictChanged -> updateForm { it.copy(district = action.district, errorRes = null) }
            is OnboardingAction.PhotoSelected -> updateForm {
                it.copy(photoUri = action.photoUri, photoDataUrl = action.photoDataUrl)
            }

            // Step 2
            is OnboardingAction.SkillToggled -> updateForm { form ->
                val skills = form.selectedSkills.toMutableList()
                if (skills.contains(action.skill)) skills.remove(action.skill) else skills.add(action.skill)
                form.copy(selectedSkills = skills)
            }

            // Step 3
            is OnboardingAction.AvailabilitySet -> updateForm { form ->
                val avail = form.availability.toMutableMap()
                avail[action.day] = action.timeSlot
                form.copy(availability = avail)
            }
            is OnboardingAction.AvailabilityRemoved -> updateForm { form ->
                val avail = form.availability.toMutableMap()
                avail.remove(action.day)
                form.copy(availability = avail)
            }

            // Step 4
            is OnboardingAction.GenerateBio -> generateBio(currentState.formState)
            is OnboardingAction.BioEdited -> updateForm { it.copy(bioEn = action.bio, errorRes = null) }
            is OnboardingAction.BioKnEdited -> updateForm { it.copy(bioKn = action.bio, errorRes = null) }

            // Final
            is OnboardingAction.PublishProfile -> publishProfile(currentState.formState)
        }
    }

    private fun validateCurrentStep(form: OnboardingFormState): Boolean = when (form.currentStep) {
        1 -> {
            when {
                Validators.validateName(form.nameEn) is ValidationResult.Invalid -> {
                    updateForm { it.copy(errorRes = R.string.error_name_required) }; false
                }
                Validators.validateVillage(form.village) is ValidationResult.Invalid -> {
                    updateForm { it.copy(errorRes = R.string.error_village_required) }; false
                }
                else -> true
            }
        }
        2 -> if (form.selectedSkills.isEmpty()) {
            updateForm { it.copy(errorRes = R.string.error_select_skill) }; false
        } else true
        3 -> if (form.availability.isEmpty()) {
            updateForm { it.copy(errorRes = R.string.error_set_availability) }; false
        } else true
        4 -> {
            if (form.bioEn.isBlank() && form.bioKn.isBlank()) {
                updateForm { it.copy(errorRes = R.string.error_bio_required) }; false
            } else true
        }
        else -> true
    }

    private fun generateBio(form: OnboardingFormState) {
        updateForm { it.copy(isBioGenerating = true, errorRes = null) }

        viewModelScope.launch {
            val skills = form.selectedSkills.joinToString(", ").ifBlank { "various subjects" }
            val location = listOf(form.village, form.district).filter { it.isNotBlank() }.joinToString(", ")
            val name = form.nameEn.ifBlank { "the volunteer" }

            val prompt = """
                Write a warm, humble 3-sentence biography for a retired professional
                volunteering on a community education platform in rural Karnataka.
                Name: $name. Location: $location. Skills: $skills.

                Output BOTH languages, each on its own line, exactly in this format
                (no extra text, no quotes, no numbering, no headers):
                EN: <three-sentence English biography>
                KN: <three-sentence Kannada (kn-IN) biography>
            """.trimIndent()

            val (en, kn) = try {
                val response = withTimeoutOrNull(8000) {
                    generativeModel.generateContent(prompt).text
                }.orEmpty()
                
                if (response.isBlank()) {
                    deterministicFallback(name, form.village, skills)
                } else {
                    parseBilingualBio(response, fallbackName = name, fallbackVillage = form.village, fallbackSkills = skills)
                }
            } catch (e: Exception) {
                Log.e("OnboardingVM", "Gemini error", e)
                deterministicFallback(name, form.village, skills)
            }
            updateForm { it.copy(bioEn = en, bioKn = kn, isBioGenerating = false) }
        }
    }

    private fun parseBilingualBio(
        response: String,
        fallbackName: String,
        fallbackVillage: String,
        fallbackSkills: String,
    ): Pair<String, String> {
        val enLine = response.lineSequence().firstOrNull { it.trimStart().startsWith("EN:", ignoreCase = true) }
            ?.substringAfter(":")?.trim().orEmpty()
        val knLine = response.lineSequence().firstOrNull { it.trimStart().startsWith("KN:", ignoreCase = true) }
            ?.substringAfter(":")?.trim().orEmpty()

        val en = enLine.ifBlank { response.trim() }
        val kn = knLine.ifBlank { deterministicFallback(fallbackName, fallbackVillage, fallbackSkills).second }
        return en to kn
    }

    private fun deterministicFallback(
        name: String,
        village: String,
        skills: String,
    ): Pair<String, String> {
        val en = "$name is a dedicated volunteer from $village. Skilled in $skills. Available to mentor students and share knowledge with the community."
        val kn = "$name ರವರು $village ಯಿಂದ ಬಂದ ಸಮರ್ಪಿತ ಸ್ವಯಂಸೇವಕ. $skills ನಲ್ಲಿ ಪರಿಣತರು. ವಿದ್ಯಾರ್ಥಿಗಳಿಗೆ ಮಾರ್ಗದರ್ಶನ ನೀಡಲು ಮತ್ತು ಸಮುದಾಯದೊಂದಿಗೆ ಜ್ಞಾನವನ್ನು ಹಂಚಿಕೊಳ್ಳಲು ಲಭ್ಯವಿದ್ದಾರೆ."
        return en to kn
    }

    private fun publishProfile(form: OnboardingFormState) {
        if (!validateCurrentStep(form)) return
        updateForm { it.copy(isSubmitting = true, errorRes = null) }

        viewModelScope.launch {
            try {
                Log.d("OnboardingVM", "Starting profile publication...")
                
                // Increased to 45s for large base64 data payloads
                val result = withTimeoutOrNull(45000) {
                    val userId = authRepository.currentUserId ?: throw Exception("Not logged in")

                    val guru = Guru(
                        id = userId,
                        nameEn = form.nameEn.trim(),
                        nameKn = form.nameKn.trim(),
                        village = form.village.trim(),
                        district = form.district.trim(),
                        bioEn = form.bioEn.trim(),
                        bioKn = form.bioKn.trim().ifBlank { form.bioEn.trim() },
                        skills = form.selectedSkills,
                        availability = form.availability,
                        photoUrl = form.photoDataUrl ?: form.photoUri ?: "",
                        isPublic = true,
                        updatedAt = System.currentTimeMillis()
                    )

                    Log.d("OnboardingVM", "Saving guru doc...")
                    val createResult = guruRepository.createOrUpdateGuru(guru)
                    if (createResult.isFailure) throw createResult.exceptionOrNull() ?: Exception("Save failed")

                    Log.d("OnboardingVM", "Upgrading user role...")
                    val upgradeResult = userRepository.upgradeToGuru(userId)
                    if (upgradeResult.isFailure) throw upgradeResult.exceptionOrNull() ?: Exception("Upgrade failed")
                    
                    true
                }

                if (result == true) {
                    Log.d("OnboardingVM", "Onboarding success!")
                    _uiState.value = OnboardingUiState.Published
                    _events.emit(OnboardingEvent.ShowSnackbar(R.string.profile_published))
                    _events.emit(OnboardingEvent.NavigateToHome)
                } else {
                    Log.e("OnboardingVM", "Publication timed out")
                    updateForm { it.copy(errorRes = R.string.error_save_profile, isSubmitting = false) }
                    _events.emit(OnboardingEvent.ShowSnackbar(R.string.error_save_profile))
                }
            } catch (e: Exception) {
                Log.e("OnboardingVM", "Publication error", e)
                updateForm { it.copy(errorRes = R.string.error_save_profile, isSubmitting = false) }
                _events.emit(OnboardingEvent.ShowSnackbar(R.string.error_save_profile))
            }
        }
    }

    private fun updateForm(transform: (OnboardingFormState) -> OnboardingFormState) {
        _uiState.update { current ->
            if (current is OnboardingUiState.Form) {
                OnboardingUiState.Form(transform(current.formState))
            } else current
        }
    }
}
