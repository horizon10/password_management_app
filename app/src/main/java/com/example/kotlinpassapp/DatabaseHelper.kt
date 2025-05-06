package com.example.kotlinpassapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * Database helper for password management
 * Handles local storage and synchronization with Firebase
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "passwordManager.db"
        private const val DATABASE_VERSION = 3
        const val TABLE_NAME = "passwords"
        private const val COLUMN_ID = "id"
        private const val COLUMN_APP_NAME = "app_name"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"
        private const val COLUMN_APP_TYPE = "app_type"
        private const val COLUMN_ORDER = "display_order"

        private val TAG = "DatabaseHelper"
    }

    private val firebaseManager = FirebaseManager()

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_APP_NAME TEXT NOT NULL,
                $COLUMN_USERNAME TEXT NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_APP_TYPE TEXT NOT NULL,
                $COLUMN_ORDER INTEGER DEFAULT 0
            )
        """
        try {
            db?.execSQL(createTableQuery)
            Log.d(TAG, "Database table created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database table: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        try {
            if (oldVersion < 2) {
                // Version 2 update - add app_type column
                db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_APP_TYPE TEXT NOT NULL DEFAULT ''")
                Log.d(TAG, "Database upgraded to version 2")
            }
            if (oldVersion < 3) {
                // Version 3 update - add order column
                db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_ORDER INTEGER DEFAULT 0")
                Log.d(TAG, "Database upgraded to version 3")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading database: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Add a new password entry to both local database and Firebase
     */
    fun addPassword(appName: String, username: String, password: String, appType: String): Long {
        val db = this.writableDatabase

        // Encrypt the password before storing
        val encryptedPassword = EncryptionUtil.encrypt(password)

        // Store encrypted password in local database
        val values = ContentValues().apply {
            put(COLUMN_APP_NAME, appName)
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, encryptedPassword)
            put(COLUMN_APP_TYPE, appType)
        }

        val rowId = try {
            db.insert(TABLE_NAME, null, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding password to local database: ${e.message}")
            e.printStackTrace()
            return -1
        }

        // If local insertion successful, add to Firebase
        if (rowId != -1L) {
            val newPassword = Password(
                id = rowId.toInt(),
                appName = appName,
                username = username,
                password = password, // Store decrypted password in object for immediate use
                appType = appType
            )
            firebaseManager.uploadPassword(newPassword)
        }

        return rowId
    }

    /**
     * Get all passwords from the database
     */
    fun getAllPasswords(): List<Password> {
        val passwords = mutableListOf<Password>()
        val db = this.readableDatabase
        var cursor: android.database.Cursor? = null

        try {
            cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ORDER ASC", null)
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    val appName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APP_NAME))
                    val username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
                    val encryptedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))
                    val appType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APP_TYPE))
                    val order = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ORDER))

                    // Decrypt the password
                    val decryptedPassword = EncryptionUtil.decrypt(encryptedPassword)

                    passwords.add(
                        Password(
                            id = id,
                            appName = appName,
                            username = username,
                            password = decryptedPassword,
                            appType = appType,
                            order = order
                        )
                    )
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving passwords: ${e.message}")
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return passwords
    }

    /**
     * Update password order
     */
    fun updatePasswordOrder(id: Int, newOrder: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ORDER, newOrder)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())

        return try {
            val result = db.update(TABLE_NAME, values, whereClause, whereArgs)
            if (result > 0) {
                // Get the updated password to sync with Firebase
                val password = getPasswordById(id)
                password?.let { firebaseManager.updatePassword(it) }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating password order: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Get a password by ID
     */
    private fun getPasswordById(id: Int): Password? {
        val db = this.readableDatabase
        var cursor: android.database.Cursor? = null
        var password: Password? = null

        try {
            cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID = ?",
                arrayOf(id.toString())
            )

            if (cursor.moveToFirst()) {
                val appName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APP_NAME))
                val username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
                val encryptedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))
                val appType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APP_TYPE))
                val order = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ORDER))

                // Decrypt the password
                val decryptedPassword = EncryptionUtil.decrypt(encryptedPassword)

                password = Password(
                    id = id,
                    appName = appName,
                    username = username,
                    password = decryptedPassword,
                    appType = appType,
                    order = order
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving password by ID: ${e.message}")
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return password
    }

    /**
     * Update password
     */
    fun updatePassword(id: Int, newPassword: String): Boolean {
        val db = this.writableDatabase

        // Encrypt the new password
        val encryptedPassword = EncryptionUtil.encrypt(newPassword)

        val values = ContentValues().apply {
            put(COLUMN_PASSWORD, encryptedPassword)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())

        return try {
            val result = db.update(TABLE_NAME, values, whereClause, whereArgs)
            if (result > 0) {
                // Get the updated password to sync with Firebase
                val password = getPasswordById(id)
                password?.let { firebaseManager.updatePassword(it) }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating password: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Get passwords by app type and name
     */
    fun getPasswordsByAppTypeAndName(appType: String, appName: String): List<Password> {
        val passwords = mutableListOf<Password>()
        val db = this.readableDatabase
        var cursor: android.database.Cursor? = null

        try {
            cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_APP_TYPE = ? AND $COLUMN_APP_NAME = ? ORDER BY $COLUMN_ORDER ASC",
                arrayOf(appType, appName)
            )

            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    val username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
                    val encryptedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))
                    val order = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ORDER))

                    // Decrypt the password
                    val decryptedPassword = EncryptionUtil.decrypt(encryptedPassword)

                    passwords.add(
                        Password(
                            id = id,
                            appName = appName,
                            username = username,
                            password = decryptedPassword,
                            appType = appType,
                            order = order
                        )
                    )
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving passwords by app type and name: ${e.message}")
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return passwords
    }

    /**
     * Check if a bank app exists
     */
    fun isBankAppExists(appName: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_APP_NAME = ? AND $COLUMN_APP_TYPE = ?"
        val cursor = db.rawQuery(query, arrayOf(appName, "Banka Uygulaması"))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    /**
     * Update username
     */
    fun updateUsername(id: Int, newUsername: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, newUsername)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())

        return try {
            val result = db.update(TABLE_NAME, values, whereClause, whereArgs)
            if (result > 0) {
                // Get the updated password to sync with Firebase
                val password = getPasswordById(id)
                password?.let { firebaseManager.updatePassword(it) }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating username: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Update app name
     */
    fun updateAppName(id: Int, newAppName: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_APP_NAME, newAppName)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())

        return try {
            val result = db.update(TABLE_NAME, values, whereClause, whereArgs)
            if (result > 0) {
                // Get the updated password to sync with Firebase
                val password = getPasswordById(id)
                password?.let { firebaseManager.updatePassword(it) }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating app name: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete password
     */
    fun deletePassword(id: Int): Boolean {
        val db = this.writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())

        // Delete from Firebase first
        firebaseManager.deletePassword(id)

        // Then delete from local database
        return try {
            val result = db.delete(TABLE_NAME, whereClause, whereArgs)
            result > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting password: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

/**
 * Password data class
 */
data class Password(
    val id: Int,
    var appName: String,
    var username: String,
    var password: String,
    var appType: String,
    var order: Int = 0
)