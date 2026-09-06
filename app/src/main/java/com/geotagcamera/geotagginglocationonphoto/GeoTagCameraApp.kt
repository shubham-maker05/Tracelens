package com.geotagcamera.geotagginglocationonphoto

import android.app.Application
import com.geotagcamera.geotagginglocationonphoto.ads.UnityAdsConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds

class TraceLensApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Unity Ads SDK init — Game ID lives in UnityAdsConfig. testMode is on by
        // default (see that file) so you see Unity's own test creative first;
        // flip it off there once you're ready to serve real ad fill.
        UnityAds.initialize(this, UnityAdsConfig.GAME_ID, UnityAdsConfig.TEST_MODE, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() { /* ready — first load happens on demand when the user taps "Watch ad" */ }
            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) { /* ads simply won't be available this session; the unlock button will show its own error */ }
        })
    }
}
