package com.nimmaguru.app.feature.discover.presentation

import androidx.annotation.StringRes
import com.nimmaguru.app.core.model.Guru

/**
 * UI state for Discover / Find Guru screen.
 * R-KT-06: Sealed hierarchy.
 */
sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Success(
        val gurus: List<Guru>,
        val query: String = "",
        val selectedSkills: List<String> = emptyList(),
        val isSearching: Boolean = false,
    ) : DiscoverUiState
    data class Error(@StringRes val messageRes: Int) : DiscoverUiState
}

sealed interface DiscoverEvent {
    data class NavigateToGuruDetail(val guruId: String) : DiscoverEvent
}

sealed interface DiscoverAction {
    data class QueryChanged(val query: String) : DiscoverAction
    data class SkillToggled(val skill: String) : DiscoverAction
    data object ClearQuery : DiscoverAction
    data object ClearFilters : DiscoverAction
    data class GuruClicked(val guruId: String) : DiscoverAction
    data object Retry : DiscoverAction
}
