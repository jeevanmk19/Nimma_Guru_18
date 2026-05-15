package com.nimmaguru.app

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Nimma Guru.
 */
@HiltAndroidApp
class NimmaGuruApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // App Check is disabled temporarily to fix "buffering" issues.
        // Re-enable this only after configuring tokens in Firebase Console.
        // installAppCheck()
    }

    private fun installAppCheck() {
        val factory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
    }
}
