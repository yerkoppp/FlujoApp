package dev.ycosorio.flujo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FlujoApplication : Application(){
    override fun onCreate() {
        super.onCreate()

        // ✅ Solo loguear en DEBUG
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}