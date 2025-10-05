package com.bravebrain

import android.app.Application
import com.google.firebase.FirebaseApp

class BraveBrainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firebase Manager
        FirebaseManager.getInstance().initialize(this)
    }
}
