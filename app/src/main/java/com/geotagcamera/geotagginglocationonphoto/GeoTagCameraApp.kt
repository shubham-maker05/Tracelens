package com.geotagcamera.geotagginglocationonphoto

import android.app.Application
import android.util.Log
import com.geotagcamera.geotagginglocationonphoto.ads.UnityAdsConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds

class TraceLensApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug builds use Unity's test mode; release builds use real ad fill.
        UnityAds.initialize(this, UnityAdsConfig.GAME_ID, UnityAdsConfig.TEST_MODE, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d("UnityAdsInit", "Unity Ads initialized successfully")
            }
            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                Log.e("UnityAdsInit", "Unity Ads init failed: $error - $message")
            }
        })
    }
}
