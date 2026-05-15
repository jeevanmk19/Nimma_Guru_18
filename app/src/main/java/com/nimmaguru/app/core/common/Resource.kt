package com.nimmaguru.app.core.common

/**
 * Sealed class representing the state of a data operation.
 * Used across all layers for consistent state management.
 *
 * R-KT-06: Use sealed hierarchy for UI state.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>
}
