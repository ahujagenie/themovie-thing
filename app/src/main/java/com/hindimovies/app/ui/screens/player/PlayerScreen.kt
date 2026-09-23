package com.hindimovies.app.ui.screens.player

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hindimovies.app.ui.theme.AccentEmerald
import com.hindimovies.app.ui.theme.AccentGold
import com.hindimovies.app.ui.theme.AccentRed
import com.hindimovies.app.ui.theme.SurfaceBorder
import com.hindimovies.app.ui.theme.SurfaceCard
import com.hindimovies.app.ui.theme.TextMuted
import com.hindimovies.app.ui.theme.TextPrimary
import com.hindimovies.app.ui.theme.TextSecondary
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun PlayerScreen(
    youtubeId: String,
    movieTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var hasPlaybackError by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isTimeout by rememberSaveable { mutableStateOf(false) }
    var useDirectEmbed by rememberSaveable { mutableStateOf(false) }
    // Plain holder (not State): assigned inside AndroidView factory during
    // composition, so it must not trigger recomposition.
    val nativeViewHolder = remember { object { var view: YouTubePlayerView? = null } }
    var youTubePlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
    var lastLoadedVideoId by remember { mutableStateOf<String?>(null) }
    var isOnline by remember { mutableStateOf(isDeviceOnline(context)) }

    // Live connectivity: flip to offline/online if the network drops or
    // returns mid-playback instead of only checking once at entry.
    DisposableEffect(context) {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (cm == null) {
            onDispose { }
        } else {
            val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    isOnline = true
                }

                override fun onLost(network: android.net.Network) {
                    isOnline = isDeviceOnline(context)
                }

                override fun onCapabilitiesChanged(
                    network: android.net.Network,
                    capabilities: android.net.NetworkCapabilities
                ) {
                    isOnline = capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            }
            try {
                cm.registerDefaultNetworkCallback(callback)
            } catch (_: Exception) { }
            onDispose {
                try {
                    cm.unregisterNetworkCallback(callback)
                } catch (_: Exception) { }
            }
        }
    }

    // Always resolve a *live* activity: the remembered one can go stale across
    // rotation/nav, and requesting orientation on a dead host silently does
    // nothing — leaving the player stuck in the small portrait strip.
    fun resolveActivity(): Activity? {
        val remembered = activity?.takeIf { !it.isFinishing && !it.isDestroyed }
        if (remembered != null) return remembered
        return context.findActivity()?.takeIf { !it.isFinishing && !it.isDestroyed }
    }

    val currentOnBack by rememberUpdatedState(onBackClick)
    fun exitPlayer() {
        try {
            resolveActivity()?.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (_: Exception) { }
        isFullscreen = false
        currentOnBack()
    }

    val toggleFullscreen = {
        val act = resolveActivity()
        if (act == null) {
            Log.w("PlayerScreen", "toggleFullscreen: no live activity, ignoring")
            Unit
        } else if (isFullscreen || isLandscape) {
            // Explicit exit: lock to portrait so the tap has a visible effect
            // even when the device is physically held in landscape.
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isFullscreen = false
        } else {
            // Fixed landscape lock — deterministic, no sensor dependency.
            // SENSOR_LANDSCAPE proved unreliable (ignored on some devices /
            // emulators while physical rotation still worked), leaving the
            // player stuck in the small portrait strip.
            Log.d("PlayerScreen", "toggleFullscreen: entering fullscreen on $act")
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            isFullscreen = true
        }
    }

    // First back press exits manual fullscreen; second (now portrait) pops.
    // Sensor rotation is intentionally NOT treated as fullscreen: the app is
    // manifest portrait-locked, so physical rotation never changes layout and
    // can never leak a landscape lock onto Home (the reported bug).
    BackHandler(enabled = isFullscreen) {
        val act = resolveActivity()
        act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        isFullscreen = false
    }

    DisposableEffect(Unit) {
        onDispose {
            // Restore the manifest portrait lock after a manual LANDSCAPE
            // fullscreen. Belt-and-braces: exitPlayer() already does this
            // before popping, this covers the system-back path.
            val act = resolveActivity()
            try {
                act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } catch (_: Exception) { }
            val window = act?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(isFullscreen, isLandscape) {
        Log.d("PlayerScreen", "fullscreen=$isFullscreen landscape=$isLandscape")
        val window = resolveActivity()?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen || isLandscape) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(youtubeId) {
        hasPlaybackError = false
        isTimeout = false
        isLoading = true
    }

    LaunchedEffect(youtubeId, useDirectEmbed) {
        // Web path resolves via onPageFinished; only the native bridge needs a watchdog.
        if (useDirectEmbed) return@LaunchedEffect
        isLoading = true
        isTimeout = false
        // Don't wipe a genuine embedding-restriction error when only the engine toggled.
        delay(8000)
        if (isLoading && !useDirectEmbed) {
            Log.w("PlayerScreen", "Native player slow (> 8s), showing manual fallback for: $youtubeId")
            isTimeout = true
        }
        delay(7000)
        if (isLoading && !useDirectEmbed && !hasPlaybackError) {
            Log.w("PlayerScreen", "Native player initialization took > 15s, auto-switching to Direct Web Player for: $youtubeId")
            useDirectEmbed = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            nativeViewHolder.view?.let { playerView ->
                try {
                    lifecycleOwner.lifecycle.removeObserver(playerView)
                } catch (_: Exception) { }
                try {
                    playerView.release()
                } catch (_: Exception) { }
            }
            nativeViewHolder.view = null
            youTubePlayerRef = null
            lastLoadedVideoId = null
        }
    }

    if (youtubeId.isBlank()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Invalid video",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "This movie has no playable YouTube id.",
                color = TextMuted,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { exitPlayer() },
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Go Back", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    if (!isOnline) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { exitPlayer() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceCard, CircleShape)
                        .border(1.dp, SurfaceBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = movieTitle,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            com.hindimovies.app.ui.components.NoInternetState(
                onRetry = {
                    isOnline = isDeviceOnline(context)
                }
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!isFullscreen && !isLandscape) {
            // Player Header Bar (Portrait Mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { exitPlayer() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceCard, CircleShape)
                        .border(1.dp, SurfaceBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = movieTitle,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = toggleFullscreen,
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceCard, CircleShape)
                        .border(1.dp, SurfaceBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Enter Fullscreen",
                        tint = TextPrimary
                    )
                }
            }
        }

        val boxModifier = if (isFullscreen || isLandscape) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        }

        Box(
            modifier = boxModifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (!hasPlaybackError) {
                if (useDirectEmbed) {
                    DirectYouTubeWebView(
                        youtubeId = youtubeId,
                        isFullscreen = isFullscreen,
                        modifier = Modifier.fillMaxSize(),
                        onLoaded = {
                            isLoading = false
                        },
                        onToggleFullscreen = toggleFullscreen
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            val safeNativeId = sanitizeYoutubeId(youtubeId)
                            Log.d("PlayerScreen", "Creating YouTubePlayerView for videoId: $safeNativeId")
                            val initialId = safeNativeId
                            YouTubePlayerView(ctx).apply {
                                nativeViewHolder.view = this
                                enableAutomaticInitialization = false

                                val iFramePlayerOptions = IFramePlayerOptions.Builder()
                                    .controls(1)
                                    .fullscreen(0)
                                    .build()

                                initialize(
                                    object : AbstractYouTubePlayerListener() {
                                        override fun onReady(youTubePlayer: YouTubePlayer) {
                                            Log.d("PlayerScreen", "YouTubePlayer onReady fired for: $initialId")
                                            youTubePlayerRef = youTubePlayer
                                            lastLoadedVideoId = initialId
                                            isLoading = false
                                            isTimeout = false
                                            try {
                                                youTubePlayer.loadVideo(initialId, 0f)
                                            } catch (e: Exception) {
                                                Log.e("PlayerScreen", "loadVideo failed, falling back to web", e)
                                                useDirectEmbed = true
                                            }
                                        }

                                        override fun onStateChange(
                                            youTubePlayer: YouTubePlayer,
                                            state: PlayerConstants.PlayerState
                                        ) {
                                            Log.d("PlayerScreen", "YouTubePlayer onStateChange: $state")
                                            if (state == PlayerConstants.PlayerState.PLAYING ||
                                                state == PlayerConstants.PlayerState.PAUSED ||
                                                state == PlayerConstants.PlayerState.VIDEO_CUED
                                            ) {
                                                isLoading = false
                                                isTimeout = false
                                            }
                                        }

                                        override fun onError(
                                            youTubePlayer: YouTubePlayer,
                                            error: PlayerConstants.PlayerError
                                        ) {
                                            if (error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER) {
                                                Log.e("PlayerScreen", "Embedding restricted by channel for: $initialId, showing fallback banner")
                                                isLoading = false
                                                hasPlaybackError = true
                                            } else {
                                                Log.e("PlayerScreen", "YouTubePlayer onError: $error, falling back to Direct Web Player")
                                                useDirectEmbed = true
                                            }
                                        }
                                    },
                                    false,
                                    iFramePlayerOptions
                                )

                                lifecycleOwner.lifecycle.addObserver(this)
                            }
                        },
                        update = {
                            val player = youTubePlayerRef
                            val safeId = sanitizeYoutubeId(youtubeId)
                            if (player != null && safeId.isNotBlank() && lastLoadedVideoId != safeId) {
                                Log.d("PlayerScreen", "youtubeId changed ${lastLoadedVideoId} -> $safeId, loading new video")
                                isLoading = true
                                hasPlaybackError = false
                                isTimeout = false
                                lastLoadedVideoId = safeId
                                try {
                                    player.loadVideo(safeId, 0f)
                                } catch (e: Exception) {
                                    Log.e("PlayerScreen", "loadVideo on update failed, falling back to web", e)
                                    useDirectEmbed = true
                                }
                            }
                        },
                        onRelease = { view ->
                            Log.d("PlayerScreen", "Releasing YouTubePlayerView")
                            try {
                                lifecycleOwner.lifecycle.removeObserver(view)
                            } catch (_: Exception) { }
                            try {
                                view.release()
                            } catch (_: Exception) { }
                            if (nativeViewHolder.view === view) {
                                nativeViewHolder.view = null
                            }
                            youTubePlayerRef = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentRed,
                            modifier = Modifier.size(38.dp)
                        )
                        if (isTimeout && !useDirectEmbed) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Loading is taking longer than usual",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    useDirectEmbed = true
                                    isLoading = true
                                    isTimeout = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Switch to Web Player", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                if (isFullscreen || isLandscape) {
                    IconButton(
                        onClick = toggleFullscreen,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .displayCutoutPadding()
                            .padding(start = 16.dp, top = 8.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .border(1.dp, SurfaceBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            } else {
                // In-player error banner
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceCard)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Embedding Restricted by Channel",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "The content owner requires watching on the YouTube app",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.youtube.com/watch?v=$youtubeId")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Watch Now on YouTube", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (!isFullscreen && !isLandscape) {
            // Compliance & Fallback Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Engine status — one quiet line. Fullscreen lives in the header;
                // the YouTube deep link below is the single primary action.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AccentEmerald, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (useDirectEmbed) "HD Web Player" else "Native Player",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            isLoading = true
                            isTimeout = false
                            hasPlaybackError = false
                            useDirectEmbed = !useDirectEmbed
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Switch player",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary 1-Tap Play Action (Bypasses Studio Embedding Restrictions)
                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/watch?v=$youtubeId")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Watch Full Movie in YouTube (1080p HD)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DirectYouTubeWebView(
    youtubeId: String,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier,
    onLoaded: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val activity = remember(context) { context.findActivity() }

    // Resolve a *live* activity on every call: the remembered one may be stale
    // (rotation, nav) or finishing. Never touch window/decor on a dead host.
    fun currentActivity(): Activity? {
        val remembered = activity?.takeIf { !it.isFinishing && !it.isDestroyed }
        if (remembered != null) return remembered
        return context.findActivity()?.takeIf { !it.isFinishing && !it.isDestroyed }
    }

    val webViewHolder = remember { object { var view: WebView? = null } }
    var lastLoadedId by remember { mutableStateOf<String?>(null) }
    // Plain holder for the HTML5 fullscreen view. State writes during
    // onDispose/onRelease (when we're leaving composition) are unsafe, so the
    // holder is the source of truth and `hasCustomView` only mirrors it for
    // the BackHandler while composed.
    val customViewHolder = remember { object { var view: View? = null; var callback: WebChromeClient.CustomViewCallback? = null } }
    var hasCustomView by remember { mutableStateOf(false) }
    var customViewTriggeredFullscreen by remember { mutableStateOf(false) }

    // Always read the *latest* fullscreen state/callback inside WebChromeClient
    // (the AndroidView factory captures once and would otherwise go stale).
    val currentIsFullscreen by rememberUpdatedState(isFullscreen)
    val currentOnToggle by rememberUpdatedState(onToggleFullscreen)

    // Snapshot refs BEFORE clearing so re-entrant calls (e.g. onHide triggered
    // by onCustomViewHidden) become no-ops instead of recursing.
    fun dismissCustomView() {
        val view = customViewHolder.view
        val cb = customViewHolder.callback
        customViewHolder.view = null
        customViewHolder.callback = null
        if (hasCustomView) hasCustomView = false
        if (view == null && cb == null) return
        try {
            (view?.parent as? ViewGroup)?.removeView(view)
        } catch (_: Exception) { }
        try {
            val decor = currentActivity()?.window?.decorView as? ViewGroup
            if (decor != null && view != null) {
                try { decor.removeView(view) } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
        try { cb?.onCustomViewHidden() } catch (_: Exception) { }
    }

    // Intercept back to dismiss custom video view before exiting the screen
    BackHandler(enabled = hasCustomView) {
        val wasTriggered = customViewTriggeredFullscreen
        customViewTriggeredFullscreen = false
        dismissCustomView()
        if (wasTriggered && currentIsFullscreen) {
            try { currentOnToggle() } catch (_: Exception) { }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Raw teardown: do NOT touch compose State here, we're leaving.
            val v = customViewHolder.view
            val cb = customViewHolder.callback
            customViewHolder.view = null
            customViewHolder.callback = null
            try { (v?.parent as? ViewGroup)?.removeView(v) } catch (_: Exception) { }
            try { cb?.onCustomViewHidden() } catch (_: Exception) { }
        }
    }

    // Pause WebView audio & video cleanly when the app goes to background
    // so playback stops immediately, but preserves the exact timestamp and buffer for UX.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    val pauseScript = """
                        (function() {
                            var iframes = document.querySelectorAll('iframe');
                            for (var i = 0; i < iframes.length; i++) {
                                iframes[i].contentWindow.postMessage(JSON.stringify({
                                    event: 'command',
                                    func: 'pauseVideo',
                                    args: ''
                                }), '*');
                            }
                            var vids = document.querySelectorAll('video');
                            for (var j = 0; j < vids.length; j++) {
                                vids[j].pause();
                            }
                        })();
                    """.trimIndent()

                    try {
                        webViewHolder.view?.evaluateJavascript(pauseScript, null)
                    } catch (_: Exception) { }

                    try {
                        webViewHolder.view?.onPause()
                    } catch (_: Exception) { }
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        webViewHolder.view?.onResume()
                    } catch (_: Exception) { }
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            try {
                lifecycle.removeObserver(observer)
            } catch (_: Exception) { }
        }
    }

    AndroidView(
        factory = { factoryContext ->
            val safeId = sanitizeYoutubeId(youtubeId)
            Log.d("PlayerScreen", "Creating DirectYouTubeWebView for videoId: $safeId")
            val initialId = safeId
            WebView(factoryContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                // Least privilege for a YouTube embed host: no local file/content access.
                settings.allowContentAccess = false
                settings.allowFileAccess = false
                try {
                    settings.safeBrowsingEnabled = true
                } catch (_: Exception) { }

                try {
                    android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                } catch (_: Exception) { }

                webChromeClient = object : WebChromeClient() {
                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (view == null) return
                        try {
                            // Resolve the activity from the view itself first: it is
                            // never stale, unlike anything captured at composition.
                            val act = view.context.findActivity() ?: currentActivity() ?: run {
                                Log.w("PlayerScreen", "onShowCustomView: no live activity, ignoring")
                                return
                            }
                            if (act.isFinishing || act.isDestroyed) {
                                Log.w("PlayerScreen", "onShowCustomView: activity finishing/destroyed, ignoring")
                                return
                            }
                            val decor = act.window?.decorView as? ViewGroup ?: run {
                                Log.w("PlayerScreen", "onShowCustomView: no decor view, ignoring")
                                return
                            }
                            // Same view already attached: refresh callback, don't re-add
                            // (re-adding throws IllegalStateException).
                            if (customViewHolder.view === view && view.parent === decor) {
                                customViewHolder.callback = callback
                                return
                            }
                            dismissCustomView()
                            // The incoming view may still be attached to a stale
                            // parent (rotation / previous decor): detach first.
                            try {
                                (view.parent as? ViewGroup)?.removeView(view)
                            } catch (e: Exception) {
                                Log.w("PlayerScreen", "onShowCustomView: detach failed, ignoring: ${e.message}")
                                return
                            }
                            if (view.parent != null) {
                                Log.w("PlayerScreen", "onShowCustomView: view still has parent, ignoring")
                                return
                            }
                            decor.addView(
                                view,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                            customViewHolder.view = view
                            customViewHolder.callback = callback
                            hasCustomView = true
                            if (!currentIsFullscreen) {
                                customViewTriggeredFullscreen = true
                                try { currentOnToggle() } catch (_: Exception) { }
                            } else {
                                customViewTriggeredFullscreen = false
                            }
                        } catch (e: Exception) {
                            Log.e("PlayerScreen", "onShowCustomView failed, ignoring", e)
                        }
                    }

                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                        Log.d(
                            "PlayerScreen",
                            "WebView console [${message?.messageLevel()}]: ${message?.message()} " +
                                "(${message?.sourceId()}:${message?.lineNumber()})"
                        )
                        return super.onConsoleMessage(message)
                    }

                    override fun onHideCustomView() {
                        try {
                            val wasTriggered = customViewTriggeredFullscreen
                            customViewTriggeredFullscreen = false
                            dismissCustomView()
                            if (wasTriggered && currentIsFullscreen) {
                                try { currentOnToggle() } catch (_: Exception) { }
                            }
                        } catch (e: Exception) {
                            Log.e("PlayerScreen", "onHideCustomView failed, ignoring", e)
                        }
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        // Keep the nocookie embed inline; open anything else externally
                        // so taps can't trap the user inside the small player frame.
                        if (url.contains("youtube-nocookie.com/embed") || url == "about:blank" || url.isBlank()) {
                            return false
                        }
                        return try {
                            view?.context?.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            )
                            true
                        } catch (_: Exception) {
                            false
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            Log.e(
                                "PlayerScreen",
                                "WebView main-frame error for: $initialId " +
                                    "code=${error?.errorCode} desc=${error?.description}"
                            )
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        errorResponse: android.webkit.WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request?.isForMainFrame == true) {
                            Log.e(
                                "PlayerScreen",
                                "WebView main-frame HTTP error for: $initialId " +
                                    "status=${errorResponse?.statusCode}"
                            )
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("PlayerScreen", "DirectYouTubeWebView onPageFinished for: $initialId url=$url")
                        onLoaded()
                    }
                }
                webViewHolder.view = this
                lastLoadedId = initialId
                loadDataWithBaseURL(
                    "https://www.youtube-nocookie.com",
                    buildYouTubeEmbedHtml(initialId),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { view ->
            val safeId = sanitizeYoutubeId(youtubeId)
            if (safeId.isBlank()) return@AndroidView
            if (lastLoadedId != safeId) {
                Log.d("PlayerScreen", "WebView youtubeId changed $lastLoadedId -> $safeId, reloading")
                lastLoadedId = safeId
                try {
                    view.stopLoading()
                } catch (_: Exception) { }
                try {
                    view.loadDataWithBaseURL(
                        "https://www.youtube-nocookie.com",
                        buildYouTubeEmbedHtml(safeId),
                        "text/html",
                        "UTF-8",
                        null
                    )
                } catch (e: Exception) {
                    Log.e("PlayerScreen", "WebView reload failed, ignoring", e)
                }
            }
        },
        onRelease = { view ->
            Log.d("PlayerScreen", "Destroying DirectYouTubeWebView")
            // Raw fullscreen teardown (no compose State writes: we're releasing).
            val v = customViewHolder.view
            val cb = customViewHolder.callback
            customViewHolder.view = null
            customViewHolder.callback = null
            try { (v?.parent as? ViewGroup)?.removeView(v) } catch (_: Exception) { }
            try { cb?.onCustomViewHidden() } catch (_: Exception) { }
            // Detach the WebView itself from any parent before destroy; a
            // WebView with a parent throws on destroy on some OEM builds.
            try { (view.parent as? ViewGroup)?.removeView(view) } catch (_: Exception) { }
            try {
                view.stopLoading()
            } catch (_: Exception) { }
            try {
                view.loadUrl("about:blank")
            } catch (_: Exception) { }
            try {
                view.removeAllViews()
            } catch (_: Exception) { }
            try {
                view.destroy()
            } catch (_: Exception) { }
            if (webViewHolder.view === view) {
                webViewHolder.view = null
            }
        },
        modifier = modifier
    )
}

private fun buildYouTubeEmbedHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                    background-color: #000000;
                }
                html, body {
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                    background-color: #000000;
                }
                .video-container {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                }
                iframe {
                    width: 100%;
                    height: 100%;
                    border: 0;
                }
            </style>
        </head>
        <body>
            <div class="video-container">
                <iframe 
                    id="youtube-player"
                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&fs=1&rel=0&modestbranding=1&enablejsapi=1" 
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                    allowfullscreen>
                </iframe>
            </div>
        </body>
        </html>
    """.trimIndent()
}

private fun sanitizeYoutubeId(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    // Plain id fast-path: YouTube ids are [A-Za-z0-9_-].
    if (trimmed.matches(Regex("[A-Za-z0-9_-]{1,64}"))) return trimmed
    // Accept full watch / youtu.be / embed URLs by extracting the id so a bad
    // catalog entry can't produce an illegal embed/loadVideo argument.
    return try {
        val uri = Uri.parse(trimmed)
        uri.getQueryParameter("v")?.trim()?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,64}")) }
            ?: uri.pathSegments.lastOrNull()?.trim()?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,64}")) }
            ?: trimmed
    } catch (_: Exception) {
        trimmed
    }
}

private fun isDeviceOnline(context: android.content.Context): Boolean {
    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
    val network = connectivityManager?.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun android.content.Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
