package com.geotagcamera.geotagginglocationonphoto.ads

import kotlinx.coroutines.delay

/**
 * Seam for a rewarded video ad provider. [MockRewardedAdManager] below is a
 * SIMULATED ad (a countdown, no network, no SDK) so the "watch ad to unlock"
 * flow works end-to-end right now, in Codespaces, with zero third-party
 * credentials. Swap it for real Unity Ads once you have a Game ID:
 *
 * 1. `implementation("com.unity3d.ads:unity-ads:4.+")` in app/build.gradle.kts.
 * 2. In your Application class: `UnityAds.initialize(context, "YOUR_UNITY_GAME_ID", testMode, listener)`.
 * 3. Implement this interface with a class that calls
 *    `UnityAds.load(placementId, loadListener)` then, on load success,
 *    `UnityAds.show(activity, placementId, showListener)`, resuming the
 *    suspend function with `true` from `onUnityAdsShowComplete` (state ==
 *    COMPLETED) and `false` for SKIPPED/error — the rest of the app (the
 *    lock screen, the unlock state) needs no other changes.
 */
interface RewardedAdManager {
    /** Suspends until the ad finishes; true only if it played to completion (a real reward). */
    suspend fun showRewardedAd(): Boolean
}

/** Simulated 4-second "ad" — always rewards. Replace with a real Unity Ads implementation before release. */
class MockRewardedAdManager : RewardedAdManager {
    override suspend fun showRewardedAd(): Boolean {
        delay(4_000L)
        return true
    }
}
