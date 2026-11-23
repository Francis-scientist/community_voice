package com.example.communityvoice.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communityvoice.data.ApiErrorMapper
import com.example.communityvoice.data.CommunityStats
import com.example.communityvoice.data.ServiceLocator
import com.example.communityvoice.data.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val stats: CommunityStats? = null,
    val errorMessage: String? = null
)

/**
 * Loads the community's headline numbers.
 *
 * All the arithmetic happens on the server (see StatsService), so this is a
 * single request rather than the app downloading every issue and adding them
 * up - which matters on a slow connection and means everybody sees the same
 * figures.
 */
class InsightsViewModel(
    private val statsRepository: StatsRepository = ServiceLocator.statsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InsightsUiState())
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() = fetch(refreshing = false)

    fun refresh() = fetch(refreshing = true)

    private fun fetch(refreshing: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = !refreshing && it.stats == null,
                    isRefreshing = refreshing,
                    errorMessage = null
                )
            }
            runCatching { statsRepository.loadStats() }
                .onSuccess { stats ->
                    _state.update {
                        it.copy(isLoading = false, isRefreshing = false, stats = stats)
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = ApiErrorMapper.toUserMessage(throwable)
                        )
                    }
                }
        }
    }
}
