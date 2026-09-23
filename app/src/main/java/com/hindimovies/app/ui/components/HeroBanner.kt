package com.hindimovies.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hindimovies.app.data.model.Movie
import com.hindimovies.app.ui.theme.AccentEmerald
import com.hindimovies.app.ui.theme.AccentGold
import com.hindimovies.app.ui.theme.AccentRed
import com.hindimovies.app.ui.theme.BackgroundDark
import com.hindimovies.app.ui.theme.DisplayFontFamily
import com.hindimovies.app.ui.theme.SurfaceBorder
import com.hindimovies.app.ui.theme.SurfaceDark
import com.hindimovies.app.ui.theme.TextPrimary
import com.hindimovies.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HeroBanner(
    movie: Movie,
    onWatchClick: (Movie) -> Unit,
    onDetailClick: (Movie) -> Unit,
    isInWatchlist: Boolean,
    onToggleWatchlist: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String = "FEATURED #1"
) {
    val heroInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .pressScale(heroInteraction, pressedScale = 0.98f)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = heroInteraction,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = { onDetailClick(movie) }
            )
    ) {
        // High-Resolution Backdrop or Theatrical Poster
        MoviePosterImage(
            movie = movie,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Fade to Dark
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BackgroundDark.copy(alpha = 0.5f),
                            BackgroundDark.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Foreground Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            // Badges Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .background(AccentRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.height(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = movie.rating,
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${movie.duration} • ${movie.channel}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Movie Title (display face — condensed caps fit long titles on one line)
            Text(
                text = movie.title,
                color = TextPrimary,
                fontFamily = DisplayFontFamily,
                fontSize = 26.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Starring
            if (movie.starring.isNotBlank()) {
                Text(
                    text = movie.starring,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CinematicWatchButton(
                    onClick = { onWatchClick(movie) },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedButton(
                    onClick = { onToggleWatchlist(movie) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceDark.copy(alpha = 0.85f),
                        contentColor = if (isInWatchlist) AccentEmerald else TextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(
                            listOf(SurfaceBorder, SurfaceBorder)
                        )
                    )
                ) {
                    AnimatedContent(
                        targetState = isInWatchlist,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(150)) +
                                scaleIn(
                                    animationSpec = tween(150),
                                    initialScale = 0.96f
                                )) togetherWith fadeOut(animationSpec = tween(150))
                        },
                        label = "HeroWatchlistToggle"
                    ) { isSaved ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                tint = if (isSaved) AccentEmerald else TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSaved) "Saved" else "Watchlist",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Swipeable hero carousel merchandised from `config.featuredMovieIds` in movies.json.
 *
 * - 1 movie: renders the plain [HeroBanner], no pager or dots.
 * - 2+ movies: auto-advances every 5s (pauses while the user is dragging),
 *   with tappable dot indicators. Card design is the same HeroBanner,
 *   badged FEATURED #1..N in carousel order.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    movies: List<Movie>,
    watchlistIds: Set<String>,
    onWatchClick: (Movie) -> Unit,
    onDetailClick: (Movie) -> Unit,
    onToggleWatchlist: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return
    if (movies.size == 1) {
        val single = movies.first()
        HeroBanner(
            movie = single,
            onWatchClick = onWatchClick,
            onDetailClick = onDetailClick,
            isInWatchlist = watchlistIds.contains(single.id),
            onToggleWatchlist = onToggleWatchlist,
            modifier = modifier
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { movies.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(movies.map { it.id }) {
        while (true) {
            delay(5000)
            if (pagerState.isScrollInProgress) continue
            val next = (pagerState.currentPage + 1) % movies.size
            try {
                pagerState.animateScrollToPage(next)
            } catch (_: Exception) {
                break
            }
        }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val movie = movies[page]
            HeroBanner(
                movie = movie,
                onWatchClick = onWatchClick,
                onDetailClick = onDetailClick,
                isInWatchlist = watchlistIds.contains(movie.id),
                onToggleWatchlist = onToggleWatchlist,
                badgeText = "FEATURED #${page + 1}"
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            movies.indices.forEach { index ->
                val selected = index == pagerState.currentPage
                val dotScale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.75f,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "heroDotScale"
                )
                val dotAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.35f,
                    animationSpec = tween(250),
                    label = "heroDotAlpha"
                )
                // 32dp hit target (up from a 6-8dp dot) with a TalkBack label.
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Go to featured ${index + 1}"
                        ) {
                            scope.launch {
                                try {
                                    pagerState.animateScrollToPage(index)
                                } catch (_: Exception) { }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer {
                                scaleX = dotScale
                                scaleY = dotScale
                                alpha = dotAlpha
                            }
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}
