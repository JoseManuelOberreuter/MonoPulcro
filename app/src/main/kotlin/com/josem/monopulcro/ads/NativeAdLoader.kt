package com.josem.monopulcro.ads

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.josem.monopulcro.R

/**
 * Carga anuncios nativos avanzados (unidad "Tareas diarias").
 * En debug usa el unit ID de prueba de Google.
 */
object NativeAdLoader {

    const val PRODUCTION_NATIVE_AD_UNIT_ID =
        "ca-app-pub-5537054947047840/2685525587"

    private const val TEST_NATIVE_AD_UNIT_ID =
        "ca-app-pub-3940256099942544/2247696110"

    fun adUnitId(context: Context): String {
        val debuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (debuggable) TEST_NATIVE_AD_UNIT_ID else PRODUCTION_NATIVE_AD_UNIT_ID
    }

    fun load(
        context: Context,
        onLoaded: (NativeAd) -> Unit,
        onFailed: ((LoadAdError) -> Unit)? = null,
    ): AdLoader {
        val adLoader = AdLoader.Builder(context, adUnitId(context))
            .forNativeAd { nativeAd -> onLoaded(nativeAd) }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    onFailed?.invoke(error)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
        return adLoader
    }

    /** Registra assets en el [NativeAdView] y asocia el [NativeAd]. */
    fun bind(adView: NativeAdView, ad: NativeAd) {
        val headline = adView.findViewById<TextView>(R.id.ad_headline)
        headline.text = ad.headline
        adView.headlineView = headline

        val body = adView.findViewById<TextView>(R.id.ad_body)
        if (ad.body.isNullOrBlank()) {
            body.visibility = View.GONE
        } else {
            body.visibility = View.VISIBLE
            body.text = ad.body
        }
        adView.bodyView = body

        val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
        val icon = ad.icon
        if (icon == null) {
            iconView.visibility = View.GONE
        } else {
            iconView.visibility = View.VISIBLE
            iconView.setImageDrawable(icon.drawable)
        }
        adView.iconView = iconView

        val cta = adView.findViewById<Button>(R.id.ad_call_to_action)
        if (ad.callToAction.isNullOrBlank()) {
            cta.visibility = View.GONE
        } else {
            cta.visibility = View.VISIBLE
            cta.text = ad.callToAction
        }
        adView.callToActionView = cta

        adView.setNativeAd(ad)
    }
}
