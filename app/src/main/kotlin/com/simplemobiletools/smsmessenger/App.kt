package com.simplemobiletools.smsmessenger

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import com.simplemobiletools.commons.extensions.checkUseEnglish

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        checkUseEnglish()
        // provide custom configuration
        val myConfig = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components { add(GifDecoder.Factory()) }
                .build()
        )

        WorkManager.initialize(this, myConfig)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}

