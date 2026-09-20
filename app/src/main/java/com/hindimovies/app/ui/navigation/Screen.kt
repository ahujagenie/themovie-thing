package com.hindimovies.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Watchlist : Screen("watchlist", "Watchlist", Icons.Default.Bookmark)

    object Detail : Screen("detail/{movieId}") {
        fun createRoute(movieId: String) = "detail/$movieId"
    }

    object Player : Screen("player/{youtubeId}?title={movieTitle}") {
        fun createRoute(youtubeId: String, movieTitle: String) =
            "player/$youtubeId?title=${android.net.Uri.encode(movieTitle)}"
    }
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Watchlist
)
