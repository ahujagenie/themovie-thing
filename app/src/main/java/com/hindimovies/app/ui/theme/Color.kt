package com.hindimovies.app.ui.theme

import androidx.compose.ui.graphics.Color

// Cinematic Obsidian Dark Palette
val BackgroundDark = Color(0xFF0C0D0E)
val SurfaceDark = Color(0xFF141619)
val SurfaceCard = Color(0xFF1A1D21)
val SurfaceBorder = Color(0xFF262A30)

// Accents
val AccentRed = Color(0xFFE50914)
val AccentRedDark = Color(0xFFB80710)
// Brightened red for small text/icons on dark surfaces: pure AccentRed on
// BackgroundDark is 4.06:1 (fails WCAG AA 4.5:1); this variant is ~5.3:1.
val AccentRedLight = Color(0xFFFF3B46)
val AccentGold = Color(0xFFFFB800)
val AccentEmerald = Color(0xFF10B981)

// Typography & Neutrals
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
// Lightened for WCAG AA: 6.28:1 on BackgroundDark (was #64748B at 4.09:1,
// failing for the 9-12sp metadata this color carries across cards and rows).
val TextMuted = Color(0xFF8494A7)
