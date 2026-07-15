package com.josem.monopulcro.ads

import android.app.Activity
import android.content.pm.ApplicationInfo
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AdLoadState {
    data object Idle : AdLoadState
    data object Loading : AdLoadState
    data object Ready : AdLoadState
    data class Failed(val message: String) : AdLoadState
}

class RewardedAdManager(private val activity: Activity) {

    private var rewardedAd: RewardedAd? = null

    private val _adState = MutableStateFlow<AdLoadState>(AdLoadState.Idle)
    val adState: StateFlow<AdLoadState> = _adState.asStateFlow()

    private val adUnitId: String
        get() {
            val debuggable =
                (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            return if (debuggable) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID
        }

    fun preload() {
        if (rewardedAd != null || _adState.value == AdLoadState.Loading) return
        _adState.value = AdLoadState.Loading

        RewardedAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _adState.value = AdLoadState.Ready
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _adState.value = AdLoadState.Failed(error.message)
                }
            }
        )
    }

    fun show(
        onEarned: () -> Unit,
        onDismissed: () -> Unit,
        onFailedToShow: () -> Unit,
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onFailedToShow()
            preload()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _adState.value = AdLoadState.Idle
                onDismissed()
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _adState.value = AdLoadState.Failed(error.message)
                onFailedToShow()
                preload()
            }
        }

        ad.show(activity) {
            onEarned()
        }
    }

    companion object {
        const val PRODUCTION_REWARDED_AD_UNIT_ID =
            "ca-app-pub-5537054947047840/9303678905"
        private const val TEST_REWARDED_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/5224354917"
    }
}
