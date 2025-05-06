package com.example.kotlinpassapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseManager {
    private val TAG = "FirebaseManager"
    private val firestore = FirebaseFirestore.getInstance()
    private val collectionName = "passwords"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Upload a password to Firebase
     */
    fun uploadPassword(password: Password) {
        val currentDate = dateFormat.format(Date())
        val encryptedPassword = EncryptionUtil.encrypt(password.password)

        val data = hashMapOf(
            "localId" to password.id,  // Keep track of local ID for syncing
            "appName" to password.appName,
            "username" to password.username,
            "password" to encryptedPassword,
            "appType" to password.appType,
            "order" to password.order,
            "tarih" to currentDate,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        firestore.collection(collectionName)
            .document(password.id.toString())
            .set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Password successfully uploaded: ${password.appName}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading password: ${e.message}")
            }
    }

    /**
     * Update a password in Firebase
     */
    fun updatePassword(password: Password) {
        val currentDate = dateFormat.format(Date())
        val encryptedPassword = EncryptionUtil.encrypt(password.password)

        val updatedData = mapOf(
            "appName" to password.appName,
            "username" to password.username,
            "password" to encryptedPassword,
            "appType" to password.appType,
            "order" to password.order,
            "tarih" to currentDate,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        firestore.collection(collectionName)
            .document(password.id.toString())
            .update(updatedData)
            .addOnSuccessListener {
                Log.d(TAG, "Firebase password updated: ${password.appName}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firebase update error: ${e.message}")

                // If document doesn't exist yet, create it
                if (e.message?.contains("No document to update") == true) {
                    uploadPassword(password)
                }
            }
    }

    /**
     * Delete a password from Firebase
     */
    fun deletePassword(id: Int) {
        firestore.collection(collectionName)
            .document(id.toString())
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Firebase password deleted (id=$id)")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firebase deletion error: ${e.message}")
            }
    }

    /**
     * Restore passwords from Firebase to local database
     * Call this method during app initialization if you want to restore from cloud
     */
    fun restoreFromFirebase(dbHelper: DatabaseHelper) {
        firestore.collection(collectionName)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} documents in Firebase")
                // Process documents and restore to local database
                // Implementation depends on your synchronization strategy
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firebase restore error: ${e.message}")
            }
    }
}