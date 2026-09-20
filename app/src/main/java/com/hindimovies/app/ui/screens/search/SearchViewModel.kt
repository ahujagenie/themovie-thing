package com.hindimovies.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hindimovies.app.data.model.Movie
import com.hindimovies.app.data.model.MovieSearch
import com.hindimovies.app.data.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedFilter: String = MovieSearch.ALL_FILTER,
    val results: List<Movie> = emptyList(),
    val availableFilters: List<String> = MovieSearch.QUICK_FILTER_LABELS,
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val error: String? = null
)

class SearchViewModel(private val repository: MovieRepository) : ViewModel() {

    // Immediate keystroke state — drives the TextField with no lag.
    private val queryInput = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(MovieSearch.ALL_FILTER)
    private val allMovies = MutableStateFlow<List<Movie>>(emptyList())
    private val isLoading = MutableStateFlow(true)
    private val loadError = MutableStateFlow<String?>(null)

    // Debounced query — drives the expensive filter. Typing "shahrukh"
    // triggers 1 filter pass instead of 8.
    @OptIn(FlowPreview::class)
    private val debouncedQuery = queryInput
        .debounce(300)
        .map { it.trim() }
        .distinctUntilChanged()

    // Heavy work lives ONLY here: re-runs on debounced query / filter /
    // catalog changes. Keystrokes (queryInput) do not touch this flow,
    // so typing never runs MovieSearch.filter.
    private val filteredResults: StateFlow<List<Movie>> =
        combine(debouncedQuery, selectedFilter, allMovies) { query, filter, all ->
            MovieSearch.filter(all, query, filter)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // Cheap string copies only — safe to run on Main on every keystroke.
    val uiState: StateFlow<SearchUiState> = combine(
        combine(queryInput, debouncedQuery, selectedFilter) { immediate, debounced, filter ->
            Triple(immediate, debounced, filter)
        },
        combine(filteredResults, isLoading, loadError) { results, loading, error ->
            Triple(results, loading, error)
        }
    ) { text, data ->
        val (immediate, debounced, filter) = text
        val (results, loading, error) = data
        SearchUiState(
            query = immediate,
            selectedFilter = filter,
            results = results,
            isLoading = loading,
            // True while the user typed ahead of the debounce window.
            isSearching = immediate.trim() != debounced,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState()
    )

    init {
        loadCatalog()
    }

    fun onQueryChange(query: String) {
        queryInput.value = query
    }

    fun onFilterSelect(filter: String) {
        selectedFilter.value = filter
    }

    fun retry() {
        loadCatalog()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            isLoading.value = true
            loadError.value = null
            try {
                allMovies.value = repository.getAllMovies()
            } catch (e: Exception) {
                loadError.value = "Couldn't load movies. Check your connection and try again."
            } finally {
                isLoading.value = false
            }
        }
    }

    class Factory(private val repository: MovieRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(repository) as T
        }
    }
}
