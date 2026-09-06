package com.geotagcamera.geotagginglocationonphoto.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * `LocalContext.current` inside Compose is sometimes a `ContextWrapper`
 * around the Activity rather than the Activity itself — a direct
 * `as Activity` cast can crash. This walks the wrapper chain to find it,
 * which `UnityAdsManager` needs since Unity Ads shows itself on an Activity.
 */
fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity found in this Context chain — UnityAdsManager needs a real Activity to show ads on.")
}
