package com.kerimovscreations.naturun.application

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.kerimovscreations.naturun.services.DriverService

class GlobalApplication : MultiDexApplication() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        base?.let {
            DriverService.instance.setContext(it)
        }
    }
}