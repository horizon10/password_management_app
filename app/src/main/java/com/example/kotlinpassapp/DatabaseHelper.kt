package com.example.kotlinpassapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "passwordManager.db"
        private const val DATABASE_VERSION = 3 // Versiyonu artırıyoruz
        const val TABLE_NAME = "passwords"
        private const val COLUMN_ID = "id"
        private const val COLUMN_APP_NAME = "app_name"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"
        private const val COLUMN_APP_TYPE = "app_type"
        private const val COLUMN_ORDER = "display_order"

    }

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        try {
            if (oldVersion < 2) {
                // Versiyon 2 güncellemesi - app_type sütunu ekleniyor
                db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_APP_TYPE TEXT NOT NULL DEFAULT ''")
            }
            if (oldVersion < 3) {
                // Versiyon 3 güncellemesi - order sütunu ekleniyor
                db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_ORDER INTEGER DEFAULT 0")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // Şifre sırasını güncelleme
    fun updatePasswordOrder(id: Int, newOrder: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ORDER, newOrder)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())
        return try {
            val result = db.update(TABLE_NAME, values, whereClause, whereArgs)
            result > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Şifre ekleme
    fun addPassword(appName: String, username: String, password: String, appType: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_APP_NAME, appName)
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_APP_TYPE, appType)
        }

        return try {
            db.insert(TABLE_NAME, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
            -1 // Hata durumunda -1 döndürüyoruz
        }
    }



    // Tüm şifreleri sıralı şekilde alma
    fun getAllPasswords(): List<Password> {
        val passwords = mutableListOf<Password>()
        val db = this.readableDatabase
        var cursor: android.database.Cursor? = null
        try {
            // ORDER BY ekliyoruz, sıralamaya göre getirmek için
            cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ORDER ASC", null)
            if (cursor.moveToFirst()) {
                do {
                    passwords.add(
                        Password(
                            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            appName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APP_NAME)),
                            username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                            password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                            appType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APP_TYPE)),
                            order = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ORDER))
                        )
                    )
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return passwords
    }

    // Şifre güncelleme
    fun updatePassword(id: Int, newPassword: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PASSWORD, newPassword)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())
        return try {
            val result = db.update(TABLE_NAME, values, whereClause, whereArgs)
            result > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    // Uygulama tipi ve uygulama adına göre şifreleri getir
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
                    passwords.add(
                        Password(
                            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            appName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APP_NAME)),
                            username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                            password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                            appType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APP_TYPE)),
                            order = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ORDER))
                        )
                    )
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return passwords
    }
    fun isBankAppExists(appName: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_APP_NAME = ? AND $COLUMN_APP_TYPE = ?"
        val cursor = db.rawQuery(query, arrayOf(appName, "Banka Uygulaması"))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // Kullanıcı adını güncelleme
    fun updateUsername(id: Int, newUsername: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, newUsername)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())
        return try {
            val result = db.update(TABLE_NAME, values, whereClause, whereArgs)
            result > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    // Uygulama adını güncelleme
    fun updateAppName(id: Int, newAppName: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_APP_NAME, newAppName)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())
        return try {
            val result = db.update(TABLE_NAME, values, whereClause, whereArgs)
            result > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Şifre silme
    fun deletePassword(id: Int): Boolean {
        val db = this.writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())
        return try {
            val result = db.delete(TABLE_NAME, whereClause, whereArgs)
            result > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

// Yeni model sınıfı
data class Password(
    val id: Int,
    var appName: String,
    var username: String,
    var password: String,
    var appType: String,
    var order: Int = 0
)
