package com.nimmaguru.app.feature.appreciation.presentation

import androidx.annotation.StringRes

sealed interface AppreciationUiState {
    data object Loading : AppreciationUiState
    data class Form(
        val message: String = "",
        val rating: Int = 0,
        val isSubmitting: Boolean = false,
        @StringRes val errorRes: Int? = null,
    ) : AppreciationUiState
    data object Submitted : AppreciationUiState
    data class Error(@StringRes val messageRes: Int) : AppreciationUiState
}

sealed interface AppreciationEvent {
    data object NavigateBack : AppreciationEvent
    data class ShowSnackbar(@StringRes val messageRes: Int) : AppreciationEvent
}

sealed interface AppreciationAction {
    data class MessageChanged(val message: String) : AppreciationAction
    data class RatingChanged(val rating: Int) : AppreciationAction
    data object Submit : AppreciationAction
}
