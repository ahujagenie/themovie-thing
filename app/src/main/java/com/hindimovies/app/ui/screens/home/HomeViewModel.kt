package com.hindimovies.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hindimovies.app.data.model.Movie
import com.hindimovies.app.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeUiState(
    val featuredMovies: List<Movie> = emptyList(),
    val featuredWatchlistIds: Set<String> = emptySet(),
    val categories: Map<String, List<Movie>> = emptyMap(),
    val isLoading: Boolean = true
)

class HomeViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            val featured = repository.getFeaturedMovies()
            val grouped = repository.getMoviesGroupedByCategory()

            _uiState.value = _uiState.value.copy(
                featuredMovies = featured,
                categories = grouped,
                isLoading = false
            )
        }

        viewModelScope.launch {
            repository.getWatchlist().collectLatest { watchlist ->
                _uiState.value = _uiState.value.copy(
                    featuredWatchlistIds = watchlist.map { it.id }.toSet()
                )
            }
        }
    }

    fun toggleFeaturedWatchlist(movie: Movie) {
        viewModelScope.launch {
            repository.toggleWatchlist(
                movie,
                _uiState.value.featuredWatchlistIds.contains(movie.id)
            )
        }
    }

    class Factory(private val repository: MovieRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
