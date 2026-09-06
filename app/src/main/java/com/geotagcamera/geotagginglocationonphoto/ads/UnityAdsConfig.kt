package com.geotagcamera.geotagginglocationonphoto.ads

/**
 * Your Unity LevelPlay / Unity Ads credentials, in one place.
 *
 * TEST_MODE = true shows Unity's own test creative (a clearly-labelled
 * placeholder ad) instead of real fill, so you can verify the whole
 * "watch ad -> unlock" flow without spending real ad budget or worrying
 * about fill rate while you're still testing. Set it to false only once
 * you're ready to ship — Unity will reject/flag apps that submit to the
 * Play Store with test mode left on.
 */
object UnityAdsConfig {
    const val GAME_ID = "800368368"
    const val REWARDED_PLACEMENT_ID = "Rewarded_Android"
    const val TEST_MODE = true
}
