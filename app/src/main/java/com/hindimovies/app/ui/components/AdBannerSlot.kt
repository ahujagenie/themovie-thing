package com.hindimovies.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Pre-wired ad container slot.
 * In the MVP, this is dormant (collapses to 0 height).
 * When ready to monetize with Google AdMob banners, enable ADS_ENABLED
 * and insert the AdView composable here without touching screen code.
 */
object AdConfig {
    const val ADS_ENABLED = false
    const val SAMPLE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
}

@Composable
fun AdBannerSlot(
    modifier: Modifier = Modifier
) {
    if (AdConfig.ADS_ENABLED) {
        // Reserved space for Google Mobile Ads SDK AdView
        Box(modifier = modifier.fillMaxWidth())
    }
}
