package com.geotagcamera.geotagginglocationonphoto.ads

/**
 * Your Unity LevelPlay / Unity Ads credentials, in one place.
 *
 * Debug builds use Unity's clearly-labelled test creative so the full
 * "watch ad -> unlock" flow can be exercised without real ad fill. Release
 * builds use real inventory.
 */
object UnityAdsConfig {
    const val GAME_ID = "800368416"
    const val REWARDED_PLACEMENT_ID = "Rewarded_Android"
    const val INTERSTITIAL_PLACEMENT_ID = "Interstitial_Android"
    const val TEST_MODE = false
}
