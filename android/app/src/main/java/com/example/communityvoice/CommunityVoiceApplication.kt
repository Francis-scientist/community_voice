package com.example.communityvoice

import android.app.Application
import com.example.communityvoice.data.ServiceLocator

/**
 * Android creates this once, before anything else in the app.
 *
 * Its only job is to hand ServiceLocator an application Context so the session
 * store can read the saved token from disk. Registered in AndroidManifest.xml
 * with android:name=".CommunityVoiceApplication".
 */
class CommunityVoiceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
