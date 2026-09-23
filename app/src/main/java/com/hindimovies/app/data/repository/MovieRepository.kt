package com.hindimovies.app.data.repository

import android.content.Context
import android.util.Log
import com.hindimovies.app.data.local.AppDatabase
import com.hindimovies.app.data.local.WatchlistEntity
import com.hindimovies.app.data.model.Movie
import com.hindimovies.app.data.model.MovieCatalog
import com.hindimovies.app.data.model.MovieSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MovieRepository(private val context: Context) {

    companion object {
        private const val TAG = "MovieRepository"

        /**
         * Remote catalog hosted on GitHub. Editing movies.json in the repo
         * updates the app's catalog — no Play Store release needed.
         * If the repo's default branch is ever renamed, update this URL.
         */
        private const val REMOTE_CATALOG_URL =
            "https://raw.githubusercontent.com/ahujagenie/themovie-thing/main/movies.json"

        private const val CACHE_FILE_NAME = "movies_catalog.json"
        private const val CONNECT_TIMEOUT_MS = 6000
        private const val READ_TIMEOUT_MS = 8000
    }

    private val database = AppDatabase.getDatabase(context)
    private val watchlistDao = database.watchlistDao()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val catalogMutex = Mutex()
    @Volatile
    private var cachedCatalog: MovieCatalog? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Fast Stale-While-Revalidate catalog loading:
     * 1. Returns in-memory cache if already resolved.
     * 2. Immediately loads disk cache or bundled assets/movies.json (< 10ms)
     *    so the home screen renders instantly without blocking on network.
     * 3. Asynchronously checks GitHub in the background for remote catalog
     *    updates and silently updates the disk cache for future launches.
     */
    suspend fun getCatalog(): MovieCatalog = withContext(Dispatchers.IO) {
        cachedCatalog?.let { return@withContext it }

        catalogMutex.withLock {
            cachedCatalog?.let { return@withLock it }

            // Immediate local load: last saved disk cache or bundled catalog
            val local = readDiskCache() ?: readBundledCatalog()
            cachedCatalog = local

            // Asynchronous background check for remote updates
            repositoryScope.launch {
                try {
                    fetchRemoteCatalog()?.let { fresh ->
                        catalogMutex.withLock {
                            cachedCatalog = fresh
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Background remote catalog fetch failed", e)
                }
            }

            local
        }
    }

    /** Clears the in-memory catalog so the next [getCatalog] re-resolves it. */
    fun refreshCatalog() {
        cachedCatalog = null
    }

    private fun fetchRemoteCatalog(): MovieCatalog? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(REMOTE_CATALOG_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Remote catalog HTTP ${connection.responseCode}, using local copy")
                return null
            }
            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val parsed = json.decodeFromString<MovieCatalog>(jsonString)
            if (parsed.movies.isEmpty()) {
                Log.w(TAG, "Remote catalog has no movies, ignoring (keeping local copy)")
                return null
            }
            writeDiskCache(jsonString)
            Log.i(TAG, "Loaded ${parsed.movies.size} movies from remote catalog")
            parsed
        } catch (e: Exception) {
            Log.w(TAG, "Remote catalog fetch failed, using local copy", e)
            null
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) { }
        }
    }

    private fun cacheFile(): File = File(context.filesDir, CACHE_FILE_NAME)

    private fun writeDiskCache(jsonString: String) {
        try {
            cacheFile().writeText(jsonString)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write catalog disk cache", e)
        }
    }

    private fun readDiskCache(): MovieCatalog? {
        return try {
            val file = cacheFile()
            if (!file.exists()) return null
            json.decodeFromString<MovieCatalog>(file.readText())
        } catch (e: Exception) {
            Log.w(TAG, "Disk catalog cache unreadable, ignoring", e)
            null
        }
    }

    private fun readBundledCatalog(): MovieCatalog {
        return try {
            val jsonString = context.assets.open("movies.json").bufferedReader().use { it.readText() }
            json.decodeFromString<MovieCatalog>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Bundled catalog unreadable", e)
            MovieCatalog()
        }
    }

    suspend fun getAllMovies(): List<Movie> {
        return getCatalog().movies
    }

    suspend fun getFeaturedMovie(): Movie? {
        return getFeaturedMovies(limit = 1).firstOrNull()
    }

    /**
     * Carousel source of truth for the hero banner.
     *
     * Priority:
     * 1. `config.featuredMovieIds` in order (JSON merchandising, no release needed).
     * 2. Legacy single `config.featuredMovieId`.
     * 3. Movies flagged `isFeatured`.
     * 4. First movies in the catalog.
     */
    suspend fun getFeaturedMovies(limit: Int = 5): List<Movie> {
        val catalog = getCatalog()
        val configuredIds = catalog.config.featuredMovieIds.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(catalog.config.featuredMovieId)
        if (configuredIds.isNotEmpty()) {
            val byId = catalog.movies.associateBy { it.id }
            val ordered = configuredIds.mapNotNull { byId[it] }.take(limit)
            if (ordered.isNotEmpty()) return ordered
        }
        val flagged = catalog.movies.filter { it.isFeatured }.take(limit)
        if (flagged.isNotEmpty()) return flagged
        return catalog.movies.take(limit)
    }

    /**
     * Groups movies by category, respecting the curated category order from config.
     */
    suspend fun getMoviesGroupedByCategory(): Map<String, List<Movie>> {
        val catalog = getCatalog()
        val allMovies = catalog.movies
        val configuredOrder = catalog.config.categoryOrder

        val grouped = allMovies.groupBy { it.category }

        // LinkedHashMap preserving the configured category order first
        val result = LinkedHashMap<String, List<Movie>>()
        for (category in configuredOrder) {
            grouped[category]?.let { movies ->
                result[category] = movies
            }
        }
        // Add any categories not explicitly listed in config
        for ((cat, movies) in grouped) {
            if (!result.containsKey(cat)) {
                result[cat] = movies
            }
        }

        return result
    }

    suspend fun getMovieById(movieId: String): Movie? {
        return getAllMovies().find { it.id == movieId }
    }

    suspend fun getRelatedMovies(movie: Movie, limit: Int = 4): List<Movie> {
        return getAllMovies()
            .filter { it.id != movie.id && (it.category == movie.category || it.channel == movie.channel) }
            .take(limit)
    }

    suspend fun searchMovies(query: String): List<Movie> {
        if (query.isBlank()) return emptyList()
        val movies = getAllMovies()
        // Ranking is CPU work — keep it off the caller's thread.
        return withContext(Dispatchers.Default) {
            // Single source of truth — same ranked predicate as SearchViewModel.
            MovieSearch.filter(movies, query)
        }
    }

    // Watchlist Room DB interactions
    fun getWatchlist(): Flow<List<Movie>> {
        return watchlistDao.getAllWatchlist().map { entities ->
            entities.map { entity ->
                Movie(
                    id = entity.id,
                    title = entity.title,
                    category = entity.category,
                    channel = entity.channel,
                    youtubeId = entity.youtubeId,
                    starring = entity.starring,
                    year = entity.year,
                    duration = entity.duration,
                    rating = entity.rating,
                    posterUrl = entity.posterUrl,
                    description = entity.description
                )
            }
        }
    }

    fun isMovieInWatchlist(movieId: String): Flow<Boolean> {
        return watchlistDao.isInWatchlist(movieId)
    }

    suspend fun toggleWatchlist(movie: Movie, isInWatchlist: Boolean) {
        if (isInWatchlist) {
            watchlistDao.removeFromWatchlist(movie.id)
        } else {
            watchlistDao.addToWatchlist(
                WatchlistEntity(
                    id = movie.id,
                    title = movie.title,
                    category = movie.category,
                    channel = movie.channel,
                    youtubeId = movie.youtubeId,
                    posterUrl = movie.resolvedPosterUrl,
                    year = movie.year,
                    duration = movie.duration,
                    rating = movie.rating,
                    starring = movie.starring,
                    description = movie.description
                )
            )
        }
    }
}
