package com.nimmaguru.app.core.common

import androidx.annotation.StringRes
import com.nimmaguru.app.R

/**
 * Centralised input validation. Used at every system-boundary input
 * (auth, onboarding, session creation, appreciation submission).
 *
 * Each validator returns a typed result so the UI can surface a
 * localised error string. R-KT-06.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(@StringRes val errorRes: Int) : ValidationResult
}

object Validators {

    /** Indian mobile (starts 6-9, 10 digits). */
    private val INDIAN_MOBILE_REGEX = Regex("^[6-9]\\d{9}$")

    fun validatePhone(rawPhone: String): ValidationResult =
        if (INDIAN_MOBILE_REGEX.matches(rawPhone)) ValidationResult.Valid
        else ValidationResult.Invalid(R.string.error_invalid_phone)

    fun isValidPhone(rawPhone: String): Boolean =
        INDIAN_MOBILE_REGEX.matches(rawPhone)

    fun validateOtp(otp: String): ValidationResult =
        if (otp.length == Constants.OTP_LENGTH && otp.all { it.isDigit() })
            ValidationResult.Valid
        else ValidationResult.Invalid(R.string.error_invalid_otp)

    fun validateName(name: String): ValidationResult {
        val trimmed = name.trim()
        return when {
            trimmed.length < Constants.NAME_MIN_LENGTH ->
                ValidationResult.Invalid(R.string.error_name_required)
            trimmed.length > Constants.NAME_MAX_LENGTH ->
                ValidationResult.Invalid(R.string.error_name_required)
            else -> ValidationResult.Valid
        }
    }

    fun validateVillage(village: String): ValidationResult {
        val trimmed = village.trim()
        return when {
            trimmed.length < Constants.VILLAGE_MIN_LENGTH ->
                ValidationResult.Invalid(R.string.error_village_required)
            trimmed.length > Constants.VILLAGE_MAX_LENGTH ->
                ValidationResult.Invalid(R.string.error_village_required)
            else -> ValidationResult.Valid
        }
    }

    fun validateBio(bio: String): ValidationResult {
        val trimmed = bio.trim()
        return when {
            trimmed.length < Constants.BIO_MIN_LENGTH ->
                ValidationResult.Invalid(R.string.error_bio_required)
            trimmed.length > Constants.BIO_MAX_LENGTH ->
                ValidationResult.Invalid(R.string.error_bio_required)
            else -> ValidationResult.Valid
        }
    }

    fun validateAppreciation(message: String): ValidationResult {
        val trimmed = message.trim()
        return when {
            trimmed.length < Constants.APPRECIATION_MIN_LENGTH ->
                ValidationResult.Invalid(R.string.error_message_required)
            trimmed.length > Constants.APPRECIATION_MAX_LENGTH ->
                ValidationResult.Invalid(R.string.error_message_required)
            else -> ValidationResult.Valid
        }
    }

    /**
     * Date in yyyy-mm-dd format. Returns Valid only if parseable AND in the future.
     */
    fun validateFutureDate(yyyyMmDd: String): ValidationResult {
        val parts = yyyyMmDd.split("-")
        if (parts.size != 3) return ValidationResult.Invalid(R.string.error_session_date_required)
        val year = parts[0].toIntOrNull() ?: return ValidationResult.Invalid(R.string.error_session_date_required)
        val month = parts[1].toIntOrNull() ?: return ValidationResult.Invalid(R.string.error_session_date_required)
        val day = parts[2].toIntOrNull() ?: return ValidationResult.Invalid(R.string.error_session_date_required)
        if (year !in 2024..2100 || month !in 1..12 || day !in 1..31) {
            return ValidationResult.Invalid(R.string.error_session_date_required)
        }
        val cal = java.util.Calendar.getInstance().apply {
            set(year, month - 1, day, 23, 59, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return if (cal.timeInMillis > System.currentTimeMillis()) ValidationResult.Valid
        else ValidationResult.Invalid(R.string.error_session_date_past)
    }
}
