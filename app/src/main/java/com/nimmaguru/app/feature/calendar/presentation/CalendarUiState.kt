package com.nimmaguru.app.feature.calendar.presentation

import androidx.annotation.StringRes
import com.nimmaguru.app.core.model.Session

/**
 * Stable internal day key — what we filter on. Display label comes
 * from a `@StringRes`. Using ints (Calendar.DAY_OF_WEEK) avoids the
 * locale issue from H18.
 */
enum class DayKey(val calendarValue: Int) {
    All(-1),
    Mon(java.util.Calendar.MONDAY),
    Tue(java.util.Calendar.TUESDAY),
    Wed(java.util.Calendar.WEDNESDAY),
    Thu(java.util.Calendar.THURSDAY),
    Fri(java.util.Calendar.FRIDAY),
    Sat(java.util.Calendar.SATURDAY),
    Sun(java.util.Calendar.SUNDAY),
}

sealed interface CalendarUiState {
    data object Loading : CalendarUiState
    data class Success(
        val sessions: List<Session>,
        val selectedDay: DayKey = DayKey.All,
        val currentUserId: String? = null,
        val isGuru: Boolean = false,
    ) : CalendarUiState
    data class Error(@StringRes val messageRes: Int) : CalendarUiState
}

sealed interface CalendarEvent {
    data class NavigateToSession(val sessionId: String, val guruId: String) : CalendarEvent
    data object NavigateToCreateSession : CalendarEvent
    data class ShowSnackbar(@StringRes val messageRes: Int) : CalendarEvent
}

sealed interface CalendarAction {
    data class DayFilterChanged(val day: DayKey) : CalendarAction
    data class SessionClicked(val sessionId: String, val guruId: String) : CalendarAction
    data class RsvpToggled(val sessionId: String) : CalendarAction
    data object CreateSessionClicked : CalendarAction
    data object Retry : CalendarAction
}
