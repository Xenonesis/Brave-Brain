package com.bravebrain

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File

class FirebaseStorageManager(private val context: Context) {
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private fun getUserStorageReference(): StorageReference? {
        val userId = auth.currentUser?.uid
        return if (userId != null) {
            storage.reference.child("users").child(userId)
        } else {
            Log.e("FirebaseStorageManager", "User not authenticated")
            null
        }
    }
    
    /**
     * Upload a file to Firebase Storage
     * @param fileUri The URI of the file to upload
     * @param folderName The folder in the user's storage to upload to
     * @param fileName The name to give the file in storage
     * @return The download URL of the uploaded file, or null if upload failed
     */
    suspend fun uploadFile(
        fileUri: Uri,
        folderName: String,
        fileName: String
    ): Result<String> {
        return try {
            val userRef = getUserStorageReference()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val fileRef = userRef.child(folderName).child(fileName)
            
            val uploadTask = fileRef.putFile(fileUri).await()
            val downloadUrl = uploadTask.metadata?.reference?.downloadUrl?.await()
            
            if (downloadUrl != null) {
                Result.success(downloadUrl.toString())
            } else {
                Result.failure(Exception("Failed to get download URL"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseStorageManager", "Upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload a local file to Firebase Storage
     * @param file The local file to upload
     * @param folderName The folder in the user's storage to upload to
     * @param fileName The name to give the file in storage
     * @return The download URL of the uploaded file, or null if upload failed
     */
    suspend fun uploadLocalFile(
        file: File,
        folderName: String,
        fileName: String
    ): Result<String> {
        return try {
            val userRef = getUserStorageReference()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val fileRef = userRef.child(folderName).child(fileName)
            
            val uploadTask = fileRef.putFile(Uri.fromFile(file)).await()
            val downloadUrl = uploadTask.metadata?.reference?.downloadUrl?.await()
            
            if (downloadUrl != null) {
                Result.success(downloadUrl.toString())
            } else {
                Result.failure(Exception("Failed to get download URL"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseStorageManager", "Upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download a file from Firebase Storage
     * @param remotePath The path to the file in storage (e.g. "profile_pictures/avatar.jpg")
     * @param destinationFile The local file to download to
     * @return True if download was successful, false otherwise
     */
    suspend fun downloadFile(
        remotePath: String,
        destinationFile: File
    ): Result<Unit> {
        return try {
            val userRef = getUserStorageReference()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val fileRef = userRef.child(remotePath)
            
            fileRef.getFile(destinationFile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseStorageManager", "Download failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a file from Firebase Storage
     * @param remotePath The path to the file in storage (e.g. "profile_pictures/avatar.jpg")
     * @return True if deletion was successful, false otherwise
     */
    suspend fun deleteFile(remotePath: String): Result<Unit> {
        return try {
            val userRef = getUserStorageReference()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val fileRef = userRef.child(remotePath)
            
            fileRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseStorageManager", "Delete failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the download URL for a file
     * @param remotePath The path to the file in storage
     * @return The download URL, or null if the file doesn't exist or operation failed
     */
    suspend fun getFileDownloadUrl(remotePath: String): Result<String> {
        return try {
            val userRef = getUserStorageReference()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val fileRef = userRef.child(remotePath)
            val url = fileRef.downloadUrl.await()
            
            Result.success(url.toString())
        } catch (e: Exception) {
            Log.e("FirebaseStorageManager", "Get download URL failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
