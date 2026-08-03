package com.josem.monopulcro.ui

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.josem.monopulcro.R
import com.josem.monopulcro.ads.NativeAdLoader

/**
 * Anuncio nativo avanzado bajo las tareas diarias.
 * Si falla la carga, no ocupa espacio (política-friendly: no placeholder falso).
 */
@Composable
fun DailyTasksNativeAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        var disposed = false
        var loaded: NativeAd? = null
        NativeAdLoader.load(
            context = context,
            onLoaded = { ad ->
                if (disposed) {
                    ad.destroy()
                } else {
                    loaded = ad
                    nativeAd = ad
                }
            },
        )
        onDispose {
            disposed = true
            loaded?.destroy()
            loaded = null
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return

    AndroidView(
        factory = { ctx ->
            val adView = LayoutInflater.from(ctx)
                .inflate(R.layout.native_ad_tasks, null, false) as NativeAdView
            NativeAdLoader.bind(adView, ad)
            adView
        },
        update = { adView ->
            NativeAdLoader.bind(adView, ad)
        },
        modifier = modifier.fillMaxWidth(),
    )
}
