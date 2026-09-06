package com.geotagcamera.geotagginglocationonphoto.ads

import android.app.Activity
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Real Unity Ads implementation of [RewardedAdManager] — Game ID and
 * placement come from [UnityAdsConfig]. SDK init happens once in
 * [com.geotagcamera.geotagginglocationonphoto.TraceLensApp.onCreate];
 * this class only loads + shows the "Rewarded_Android" placement on demand
 * (loading ahead of time, e.g. as soon as Settings opens, would cut the
 * wait before "Watch ad" — left as a follow-up, not required to work).
 *
 * [activity] must be the current foreground Activity — Unity Ads shows
 * itself as an overlay on it, so a stale/backgrounded Activity reference
 * will fail to show; pass it fresh from the composable's LocalContext each
 * time, don't cache it across recompositions.
 */
class UnityAdsManager(private val activity: Activity) : RewardedAdManager {

    suspend fun showInterstitial(): Boolean = suspendCancellableCoroutine { continuation ->
        UnityAds.load(UnityAdsConfig.INTERSTITIAL_PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                UnityAds.show(activity, placementId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String?) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                    override fun onUnityAdsShowStart(placementId: String) { }
                    override fun onUnityAdsShowClick(placementId: String) { }
                    override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                        if (continuation.isActive) continuation.resume(state == UnityAds.UnityAdsShowCompletionState.COMPLETED)
                    }
                })
            }
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String?) {
                if (continuation.isActive) continuation.resume(false)
            }
        })
    }

    override suspend fun showRewardedAd(): Boolean = suspendCancellableCoroutine { continuation ->
        val load = {
            UnityAds.load(UnityAdsConfig.REWARDED_PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                UnityAds.show(activity, placementId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String?) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                    override fun onUnityAdsShowStart(placementId: String) { /* no-op */ }
                    override fun onUnityAdsShowClick(placementId: String) { /* no-op */ }
                    override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                        // Only a full watch-through counts as a reward, same as the
                        // task's own intent — someone skipping early shouldn't unlock.
                        val rewarded = state == UnityAds.UnityAdsShowCompletionState.COMPLETED
                        if (continuation.isActive) continuation.resume(rewarded)
                    }
                })
            }
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String?) {
                if (continuation.isActive) continuation.resume(false)
            }
            })
        }
        kotlinx.coroutines.CoroutineScope(continuation.context).launch {
            val initialized = withTimeoutOrNull(10_000) {
                while (!UnityAds.isInitialized) delay(100)
                true
            } == true
            if (initialized && continuation.isActive) load()
            else if (continuation.isActive) continuation.resume(false)
        }
    }
}
