package com.hindimovies.app.ui.screens.detail

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

data class DetailUiState(
    val movie: Movie? = null,
    val relatedMovies: List<Movie> = emptyList(),
    val isInWatchlist: Boolean = false,
    val isLoading: Boolean = true
)

class DetailViewModel(
    private val movieId: String,
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadMovieDetails()
    }

    private fun loadMovieDetails() {
        viewModelScope.launch {
            val movie = repository.getMovieById(movieId)
            if (movie != null) {
                val related = repository.getRelatedMovies(movie)
                _uiState.value = _uiState.value.copy(
                    movie = movie,
                    relatedMovies = related,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }

        viewModelScope.launch {
            repository.isMovieInWatchlist(movieId).collectLatest { inWatchlist ->
                _uiState.value = _uiState.value.copy(isInWatchlist = inWatchlist)
            }
        }
    }

    fun toggleWatchlist() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            repository.toggleWatchlist(movie, _uiState.value.isInWatchlist)
        }
    }

    class Factory(
        private val movieId: String,
        private val repository: MovieRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailViewModel(movieId, repository) as T
        }
    }
}
