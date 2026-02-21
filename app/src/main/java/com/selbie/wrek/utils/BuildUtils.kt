package com.selbie.wrek.utils

import android.content.Context
import android.os.Build

object BuildUtils {
    fun getBuildString(context: Context) : String {
        val pm = context.packageManager
        val pi = pm.getPackageInfo(context.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pi.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pi.versionCode.toLong()
        }
        val versionNameBase = pi.versionName ?: ""

        return "$versionNameBase.$versionCode"
    }

}