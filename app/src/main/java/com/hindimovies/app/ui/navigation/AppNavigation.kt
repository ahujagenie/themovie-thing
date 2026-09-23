package com.hindimovies.app.ui.navigation

import android.content.pm.ActivityInfo
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hindimovies.app.MoviesApplication
import com.hindimovies.app.ui.screens.detail.DetailScreen
import com.hindimovies.app.ui.screens.detail.DetailViewModel
import com.hindimovies.app.ui.screens.home.HomeScreen
import com.hindimovies.app.ui.screens.home.HomeViewModel
import com.hindimovies.app.ui.screens.player.PlayerScreen
import com.hindimovies.app.ui.screens.search.SearchScreen
import com.hindimovies.app.ui.screens.search.SearchViewModel
import com.hindimovies.app.ui.screens.watchlist.WatchlistScreen
import com.hindimovies.app.ui.screens.watchlist.WatchlistViewModel
import com.hindimovies.app.ui.theme.AccentRed
import com.hindimovies.app.ui.theme.AccentRedLight
import com.hindimovies.app.ui.theme.BackgroundDark
import com.hindimovies.app.ui.theme.SurfaceBorder
import com.hindimovies.app.ui.theme.SurfaceCard
import com.hindimovies.app.ui.theme.TextMuted
import com.hindimovies.app.ui.theme.TextSecondary
import com.hindimovies.app.util.findActivity

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val app = LocalContext.current.applicationContext as MoviesApplication
    val repository = app.movieRepository

    // Only show bottom navigation on top-level tabs
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Watchlist.route
    )

    // The player must be truly edge-to-edge: Scaffold's innerPadding carries the
    // system-bar insets (enableEdgeToEdge), which would inset the fullscreen
    // Box so "fullscreen" still renders as a small inset player. Note:
    // destination.route is the pattern ("player/{youtubeId}?..."), not values.
    val isPlayerRoute = currentRoute?.startsWith("player") == true

    val context = LocalContext.current
    LaunchedEffect(isPlayerRoute) {
        if (!isPlayerRoute) {
            val act = context.findActivity()
            try {
                if (act != null && act.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                val window = act?.window
                if (window != null) {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            } catch (_: Exception) { }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        contentWindowInsets = if (isPlayerRoute) WindowInsets(0, 0, 0, 0)
        else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = BackgroundDark,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = SurfaceBorder
                    )
                ) {
                    bottomNavScreens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                screen.icon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title.orEmpty(),
                                    fontSize = 11.sp
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentRedLight,
                                selectedTextColor = AccentRedLight,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = AccentRed.copy(alpha = 0.18f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = if (isPlayerRoute) Modifier.fillMaxSize()
            else Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(250)) +
                    slideInHorizontally(
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    ) { it / 8 }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) +
                    slideOutHorizontally(
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    ) { -it / 8 }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(250)) +
                    slideInHorizontally(
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    ) { -it / 8 }
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) +
                    slideOutHorizontally(
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    ) { it / 8 }
            }
        ) {
            // Home Tab
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(repository)
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onMovieClick = { movie ->
                        navController.navigate(Screen.Detail.createRoute(movie.id))
                    },
                    onWatchMovie = { movie ->
                        navController.navigate(Screen.Player.createRoute(movie.youtubeId, movie.title))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }

            // Search Tab
            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = viewModel(
                    factory = SearchViewModel.Factory(repository)
                )
                SearchScreen(
                    viewModel = searchViewModel,
                    onMovieClick = { movie ->
                        navController.navigate(Screen.Detail.createRoute(movie.id))
                    }
                )
            }

            // Watchlist Tab
            composable(Screen.Watchlist.route) {
                val watchlistViewModel: WatchlistViewModel = viewModel(
                    factory = WatchlistViewModel.Factory(repository)
                )
                WatchlistScreen(
                    viewModel = watchlistViewModel,
                    onMovieClick = { movie ->
                        navController.navigate(Screen.Detail.createRoute(movie.id))
                    }
                )
            }

            // Movie Detail Screen
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId").orEmpty()
                val detailViewModel: DetailViewModel = viewModel(
                    factory = DetailViewModel.Factory(movieId, repository)
                )
                DetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onWatchClick = { movie ->
                        navController.navigate(Screen.Player.createRoute(movie.youtubeId, movie.title))
                    },
                    onRelatedMovieClick = { relatedMovie ->
                        navController.navigate(Screen.Detail.createRoute(relatedMovie.id))
                    }
                )
            }

            // YouTube Player Screen — fade only (no slide): the destination
            // hosts AndroidView surfaces (YouTubePlayerView / WebView) that
            // must not be translated mid-transition.
            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("youtubeId") { type = NavType.StringType },
                    navArgument("movieTitle") {
                        type = NavType.StringType
                        defaultValue = "Movie"
                    }
                ),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
                popEnterTransition = { fadeIn(animationSpec = tween(250)) },
                popExitTransition = { fadeOut(animationSpec = tween(200)) }
            ) { backStackEntry ->
                val youtubeId = backStackEntry.arguments?.getString("youtubeId").orEmpty()
                val movieTitle = backStackEntry.arguments?.getString("movieTitle").orEmpty()
                PlayerScreen(
                    youtubeId = youtubeId,
                    movieTitle = android.net.Uri.decode(movieTitle),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
