package com.hindimovies.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.hindimovies.app.R
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.hindimovies.app.data.model.Movie
import com.hindimovies.app.ui.theme.AccentRed
import com.hindimovies.app.ui.theme.BackgroundDark
import com.hindimovies.app.ui.theme.SurfaceCard
import com.hindimovies.app.ui.theme.TextMuted
import com.hindimovies.app.ui.theme.TextPrimary

/**
 * Resilient poster image component with automatic self-healing fallback:
 * 1. Attempts the custom theatrical poster URL if provided in catalog.
 * 2. Automatically falls back to the guaranteed YouTube thumbnail if the primary fails.
 * 3. Shows an elegant cinematic placeholder card if both fail or the device is offline.
 */
@Composable
fun MoviePosterImage(
    movie: Movie,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val primaryUrl = movie.posterUrl?.takeIf { it.isNotBlank() }
    val fallbackUrl = movie.youtubeThumbnailUrl

    // 0 = primaryUrl (if available), 1 = fallbackUrl, 2 = all failed
    var currentTier by remember(movie.id) {
        mutableIntStateOf(if (primaryUrl != null) 0 else 1)
    }

    val activeUrl = when (currentTier) {
        0 -> primaryUrl ?: fallbackUrl
        1 -> fallbackUrl
        else -> null
    }

    if (activeUrl != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(activeUrl)
                .crossfade(true)
                .build(),
            contentDescription = movie.title,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = AccentRed.copy(alpha = 0.5f),
                        strokeWidth = 2.dp
                    )
                }
            },
            onError = {
                // Advance to next tier on failure
                if (currentTier == 0) {
                    currentTier = 1
                } else if (currentTier == 1) {
                    currentTier = 2
                }
            }
        )
    } else {
        // Offline / Error Theatrical Fallback UI
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.poster_placeholder),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Title overlay at the bottom with dark gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = movie.title,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
