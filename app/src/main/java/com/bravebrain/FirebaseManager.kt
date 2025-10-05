package com.bravebrain

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * A centralized Firebase manager that provides access to all Firebase services
 */
class FirebaseManager private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: FirebaseManager? = null

        fun getInstance(): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseManager().also { INSTANCE = it }
            }
        }
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    fun getAuth(): FirebaseAuth = auth
    fun getFirestore(): FirebaseFirestore = firestore
    fun getStorage(): FirebaseStorage = storage

    /**
     * Initialize Firebase with context (typically called in Application class)
     */
    fun initialize(context: Context) {
        // Firebase is automatically initialized when getInstance() is called
        // This method can be used for any additional setup if needed
    }

    /**
     * Check if user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Get current user ID if authenticated
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        auth.signOut()
    }
}
