package com.hindimovies.app.data.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class MovieCatalog(
    val config: CatalogConfig = CatalogConfig(),
    val movies: List<Movie> = emptyList()
)

@Serializable
data class CatalogConfig(
    val featuredMovieId: String? = null,
    val featuredMovieIds: List<String> = emptyList(),
    val categoryOrder: List<String> = emptyList()
)

@Serializable
data class Movie(
    val id: String,
    val title: String,
    val category: String,
    val channel: String,
    val youtubeId: String,
    val starring: String,
    val year: Int,
    val duration: String,
    val rating: String,
    val posterUrl: String? = null,
    val description: String,
    val isFeatured: Boolean = false,
    val embeddingVerified: Boolean = true
) {
    /**
     * Resolves the poster image with a bulletproof fallback:
     * 1. Custom theatrical poster URL (if provided in movies.json)
     * 2. YouTube auto-generated thumbnail (hqdefault is guaranteed to exist for all videos)
     */
    val resolvedPosterUrl: String
        get() = posterUrl?.takeIf { it.isNotBlank() } ?: youtubeThumbnailUrl

    /**
     * Standard YouTube thumbnail (hqdefault 480x360) guaranteed to exist for any YouTube video.
     */
    val youtubeThumbnailUrl: String
        get() = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"

    /**
     * Optional YouTube high-resolution thumbnail (maxresdefault 1280x720).
     */
    val youtubeMaxResThumbnailUrl: String
        get() = "https://img.youtube.com/vi/$youtubeId/maxresdefault.jpg"
}

/**
 * Single source of truth for in-memory movie search.
 *
 * Used by both [com.hindimovies.app.ui.screens.search.SearchViewModel]
 * (debounced, cached list) and
 * [com.hindimovies.app.data.repository.MovieRepository.searchMovies]
 * (one-shot). Single pass over the list, tokenized + ranked so title
 * hits sort above starring/channel/category hits.
 */
object MovieSearch {

    const val ALL_FILTER = "All"

    // Exact catalog values — substring matching broke when labels diverged
    // (e.g. chip "90s Thrillers" never matched "⚡ 90s Action & Thrillers").
    private const val SOUTH_ACTION_CATEGORY = "🔥 South Action (Hindi Dubbed)"
    private const val COMEDY_CATEGORY = "😂 Bollywood Comedy Dhamaka"
    private const val NINETIES_CATEGORY = "⚡ 90s Action & Thrillers"

    data class QuickFilter(
        val label: String,
        val matches: (Movie) -> Boolean
    )

    val QUICK_FILTERS = listOf(
        QuickFilter(ALL_FILTER) { true },
        QuickFilter("South Action") { it.category == SOUTH_ACTION_CATEGORY },
        QuickFilter("Comedy") { it.category == COMEDY_CATEGORY },
        QuickFilter("Rajshri") { it.channel.equals("Rajshri", ignoreCase = true) },
        // Covers "Goldmines", "Goldmines Movies", "Goldmines Housefull", ...
        QuickFilter("Goldmines") { it.channel.startsWith("Goldmines", ignoreCase = true) },
        QuickFilter("90s Thrillers") { it.category == NINETIES_CATEGORY }
    )

    val QUICK_FILTER_LABELS: List<String> = QUICK_FILTERS.map { it.label }

    private val filterByLabel: Map<String, (Movie) -> Boolean> =
        QUICK_FILTERS.associate { it.label to it.matches }

    // Queries shorter than this return the base list unfiltered — single-char
    // queries match almost everything and are pure typing transients anyway.
    private const val MIN_QUERY_LENGTH = 2

    // Tokens shorter than this are ignored for partial matching ("a", "I").
    private const val MIN_TOKEN_LENGTH = 2

    fun filter(movies: List<Movie>, rawQuery: String, filter: String = ALL_FILTER): List<Movie> {
        val base = when (val predicate = filterByLabel[filter]) {
            null -> movies.filter {
                // Back-compat for unknown filter strings: old substring behavior.
                it.category.contains(filter, ignoreCase = true) ||
                    it.channel.contains(filter, ignoreCase = true)
            }
            else -> if (filter == ALL_FILTER) movies else movies.filter(predicate)
        }

        val q = rawQuery.trim().lowercase(Locale.ROOT)
        if (q.length < MIN_QUERY_LENGTH) return base
        val tokens = q.split(WHITESPACE).filter { it.length >= MIN_TOKEN_LENGTH }
        // Handles "shah rukh" vs "shahrukh" style queries.
        val qSpaceless = q.replace(WHITESPACE, "")

        return base.mapNotNull { movie ->
            val score = score(movie, q, tokens, qSpaceless)
            if (score > 0) movie to score else null
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private val WHITESPACE = "\\s+".toRegex()

    /**
     * Returns 0 when there is no title/starring/channel/category/year hit.
     * Description can only add a bonus on top of a real hit — otherwise
     * stopwords ("the", "and") match nearly every description and return
     * the whole catalog.
     */
    private fun score(movie: Movie, q: String, tokens: List<String>, qSpaceless: String): Int {
        var s = 0

        if (movie.title.contains(q, ignoreCase = true)) {
            s += 100
            if (movie.title.startsWith(q, ignoreCase = true)) s += 30
        } else {
            // Partial token matches still rank, exact-phrase matches rank higher.
            for (t in tokens) {
                if (movie.title.contains(t, ignoreCase = true)) s += 20
            }
        }
        // Spaceless fallback for compound names ("shahrukh" ~ "shah rukh").
        if (s == 0 && qSpaceless.length >= 4 &&
            movie.title.replace(WHITESPACE, "").contains(qSpaceless, ignoreCase = true)
        ) {
            s += 60
        }

        if (movie.starring.contains(q, ignoreCase = true)) {
            s += 40
        } else {
            for (t in tokens) {
                if (movie.starring.contains(t, ignoreCase = true)) s += 10
            }
            if (qSpaceless.length >= 4 &&
                movie.starring.replace(WHITESPACE, "").contains(qSpaceless, ignoreCase = true)
            ) {
                s += 25
            }
        }

        if (movie.channel.contains(q, ignoreCase = true)) s += 15
        if (movie.category.contains(q, ignoreCase = true)) s += 15
        if (movie.year.toString() == q) s += 25

        if (s == 0) return 0
        if (movie.description.contains(q, ignoreCase = true)) s += 5
        return s
    }
}
