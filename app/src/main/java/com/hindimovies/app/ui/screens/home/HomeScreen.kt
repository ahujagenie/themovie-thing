package com.hindimovies.app.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.hindimovies.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hindimovies.app.data.model.Movie
import com.hindimovies.app.ui.components.AdBannerSlot
import com.hindimovies.app.ui.components.GenreRow
import com.hindimovies.app.ui.components.HeroCarousel
import com.hindimovies.app.ui.theme.AccentEmerald
import com.hindimovies.app.ui.theme.AccentRed
import com.hindimovies.app.ui.theme.BackgroundDark
import com.hindimovies.app.ui.theme.SurfaceBorder
import com.hindimovies.app.ui.theme.SurfaceCard
import com.hindimovies.app.ui.theme.TextMuted
import com.hindimovies.app.ui.theme.TextPrimary
import com.hindimovies.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit,
    onWatchMovie: (Movie) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Loading Hindi Cinema Zone",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator(
                    color = AccentRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading Blockbusters...",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Hindi Cinema Zone Logo",
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Hindi Cinema Zone",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showDisclaimerDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceCard, CircleShape)
                        .border(1.dp, SurfaceBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About & Disclaimer",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceCard, CircleShape)
                        .border(1.dp, SurfaceBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Hero Featured Carousel (merchandised via config.featuredMovieIds)
        if (uiState.featuredMovies.isNotEmpty()) {
            item {
                HeroCarousel(
                    movies = uiState.featuredMovies,
                    watchlistIds = uiState.featuredWatchlistIds,
                    onWatchClick = onWatchMovie,
                    onDetailClick = onMovieClick,
                    onToggleWatchlist = { viewModel.toggleFeaturedWatchlist(it) }
                )
            }
        }

        // Official Channel Attribution Info Banner (Play Store Transparency)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(SurfaceCard.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AccentEmerald, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Official YouTube Streams",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Goldmines • Shemaroo • Rajshri • Ultra • Ishtar",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Category Rows
        uiState.categories.forEach { (categoryName, movies) ->
            item(key = categoryName) {
                GenreRow(
                    categoryTitle = categoryName,
                    movies = movies,
                    onMovieClick = onMovieClick,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }

        // Ad Banner Slot Placeholder
        item {
            AdBannerSlot(modifier = Modifier.padding(top = 16.dp))
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Legal Disclaimer Dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            containerColor = SurfaceCard,
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            },
            title = {
                Text(
                    text = "About & Legal Disclaimer",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Hindi Cinema Movie Zone is a curated discovery platform. We do not host, store, or upload any video files on our own servers.\n\nAll movies in this catalog are legally embedded and streamed directly from official, publicly accessible YouTube channels (e.g. Goldmines, Shemaroo, Ultra, Rajshri) via the official YouTube Player API in strict compliance with YouTube Terms of Service.\n\nAll copyrights, trademarks, and intellectual property belong to their respective studio owners.\n\nTo request removal of any video link from this catalog, please contact: contact@backlinks.fyi",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDisclaimerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Understood", color = Color.White)
                }
            }
        )
    }
}
