package com.nimmaguru.app.feature.discover.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.feature.discover.domain.usecase.SearchGurusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val searchGurusUseCase: SearchGurusUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DiscoverEvent>()
    val events: SharedFlow<DiscoverEvent> = _events.asSharedFlow()

    private var searchJob: Job? = null

    init {
        loadGurus()
    }

    fun onAction(action: DiscoverAction) {
        when (action) {
            is DiscoverAction.QueryChanged -> {
                updateSuccessState { it.copy(query = action.query, isSearching = true) }
                debouncedSearch()
            }
            is DiscoverAction.SkillToggled -> {
                updateSuccessState { state ->
                    val skills = state.selectedSkills.toMutableList()
                    if (skills.contains(action.skill)) skills.remove(action.skill)
                    else skills.add(action.skill)
                    state.copy(selectedSkills = skills, isSearching = true)
                }
                debouncedSearch()
            }
            is DiscoverAction.ClearQuery -> {
                updateSuccessState { it.copy(query = "", isSearching = true) }
                debouncedSearch()
            }
            is DiscoverAction.ClearFilters -> {
                updateSuccessState {
                    it.copy(query = "", selectedSkills = emptyList(), isSearching = true)
                }
                debouncedSearch()
            }
            is DiscoverAction.GuruClicked -> {
                viewModelScope.launch {
                    _events.emit(DiscoverEvent.NavigateToGuruDetail(action.guruId))
                }
            }
            is DiscoverAction.Retry -> loadGurus()
        }
    }

    private fun loadGurus() {
        viewModelScope.launch {
            _uiState.value = DiscoverUiState.Loading
            searchGurusUseCase()
                .onSuccess { gurus ->
                    _uiState.value = DiscoverUiState.Success(gurus = gurus)
                }
                .onFailure {
                    _uiState.value = DiscoverUiState.Error(R.string.error_load_home)
                }
        }
    }

    private fun debouncedSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(Constants.SEARCH_DEBOUNCE_MS)
            val currentState = _uiState.value
            if (currentState is DiscoverUiState.Success) {
                searchGurusUseCase(
                    query = currentState.query,
                    skills = currentState.selectedSkills,
                )
                    .onSuccess { gurus ->
                        updateSuccessState { it.copy(gurus = gurus, isSearching = false) }
                    }
                    .onFailure {
                        updateSuccessState { it.copy(isSearching = false) }
                    }
            }
        }
    }

    private fun updateSuccessState(transform: (DiscoverUiState.Success) -> DiscoverUiState.Success) {
        _uiState.update { current ->
            if (current is DiscoverUiState.Success) transform(current)
            else current
        }
    }
}
