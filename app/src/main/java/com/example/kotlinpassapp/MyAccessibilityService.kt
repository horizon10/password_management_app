package com.example.kotlinpassapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat

class MyAccessibilityService : AccessibilityService() {
    private val TAG = "MyAccessibilityService"
    private var lastPackageName = ""
    private var isSecurityKeyboardVisible = false
    private var isPasswordFilled = false
    private var lastPasswordFillTime: Long = 0
    private val PASSWORD_FILL_INTERVAL = 60000 // 60 saniye

    private fun isAppLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!isAppLoggedIn()) {
            event.packageName?.toString()?.let { packageName ->
                if (lastPackageName != packageName) {
                    lastPackageName = packageName
                    isPasswordFilled = false
                    lastPasswordFillTime = 0
                    Log.d(TAG, "Aktif Uygulama: $packageName")
                }
            }
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                processEvent(event)
            }
        }
    }

    private fun processEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        val packageName = event.packageName?.toString() ?: return

        if (isSecurityKeyboardVisible(rootNode)) {
            isSecurityKeyboardVisible = true
            Log.d(TAG, "Güvenlik klavyesi tespit edildi")
            findAndFillPasswordFields(rootNode, packageName)
        } else {
            val source = event.source
            if (source != null && source.className == "android.widget.EditText") {
                // Şifre alanının giriş ekranındaki şifre alanı olup olmadığını kontrol et
                if (isLoginPasswordField(source)) {
                    fillPasswordField(source, packageName)
                }
            } else {
                findAndFillPasswordFields(rootNode, packageName)
            }
        }
        rootNode.recycle()
    }
    private fun isLoginPasswordField(node: AccessibilityNodeInfo): Boolean {
        val inputType = node.inputType
        val isPasswordField = inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                inputType == InputType.TYPE_NUMBER_VARIATION_PASSWORD ||
                node.isPassword ||
                node.contentDescription?.toString()?.contains("şifre", ignoreCase = true) == true ||
                node.contentDescription?.toString()?.contains("password", ignoreCase = true) == true

        // Şifre alanının konumunu ve boyutunu kontrol et
        val rect = Rect()
        node.getBoundsInScreen(rect)

        // Örnek: Şifre alanı ekranın ortasında ve belirli bir boyutta olmalı
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        val isInCenter = rect.centerX() > screenWidth * 0.3 && rect.centerX() < screenWidth * 0.7 &&
                rect.centerY() > screenHeight * 0.3 && rect.centerY() < screenHeight * 0.7

        val isReasonableSize = rect.width() > 100 && rect.height() > 40

        return isPasswordField && isInCenter && isReasonableSize
    }

    private fun isSecurityKeyboardVisible(rootNode: AccessibilityNodeInfo): Boolean {
        val gridLayouts = findNodesByClassName(rootNode, "android.widget.GridLayout")
        if (gridLayouts.isNotEmpty()) {
            for (grid in gridLayouts) {
                val rect = Rect()
                grid.getBoundsInScreen(rect)

                if (rect.top > screenHeight / 2 && grid.childCount > 9) {
                    grid.recycle()
                    return true
                }
                grid.recycle()
            }
        }

        // Daha sağlam güvenlik klavyesi kontrolü
        val keyboardContainers = findNodesByClassName(rootNode, "com.keyboard.SecurityKeyboard") +
                findNodesByClassName(rootNode, "com.bank.securitykeyboard.KeyboardView")

        return keyboardContainers.isNotEmpty()
    }


    private fun findNodesByClassName(root: AccessibilityNodeInfo, className: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        findNodesByClassNameRecursive(root, className, result)
        return result
    }

    private fun findNodesByClassNameRecursive(node: AccessibilityNodeInfo, className: String, result: MutableList<AccessibilityNodeInfo>) {
        if (node.className?.toString() == className) {
            result.add(AccessibilityNodeInfo.obtain(node))
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodesByClassNameRecursive(child, className, result)
            child.recycle()
        }
    }

    private fun findAndFillPasswordFields(rootNode: AccessibilityNodeInfo, packageName: String) {
        val currentTime = System.currentTimeMillis()
        if (isPasswordFilled && (currentTime - lastPasswordFillTime) < PASSWORD_FILL_INTERVAL) return

        val editTexts = findNodesByClassName(rootNode, "android.widget.EditText")

        if (editTexts.isNotEmpty()) {
            for (editText in editTexts) {
                if (isLoginPasswordField(editText)) {
                    fillPasswordField(editText, packageName)
                    isPasswordFilled = true
                    lastPasswordFillTime = currentTime
                    break
                }

                editText.recycle()
            }
        }
    }



    private fun fillPasswordField(node: AccessibilityNodeInfo, packageName: String) {
        val bankName = getBankNameFromPackage(packageName)
        if (bankName.isNotEmpty()) {
            Log.d(TAG, "Tespit Edilen Banka: $bankName")

            // Şifreyi veritabanından al
            val dbHelper = DatabaseHelper(applicationContext)
            val bankPasswords = dbHelper.getPasswordsByAppTypeAndName("Banka Uygulaması", bankName)

            if (bankPasswords.isNotEmpty()) {
                val password = bankPasswords[0].username
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password)
                }

                if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                    // Odaklanmayı deneyip tekrar şifre girme işlemi
                    node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                }

                Log.d(TAG, "Şifre girme denemesi yapıldı")
            } else {
                Log.d(TAG, "Bu banka için şifre bulunamadı: $bankName")
            }
        }
    }

    private fun getBankNameFromPackage(packageName: String): String {
        return when (packageName) {
            "com.ziraat.ziraatmobil" -> "Ziraat Bankasi"
            "com.pozitron.iscep" -> "Is Bankasi"
            "com.ykb.android" -> "Yapi Kredi"
            "com.nextcx.nays" -> "Nays"
            "com.mobillium.papara" -> "Papara"
            "com.turkcell.paycell" -> "Paycell"
            "com.garanti.cepsubesi" -> "Garanti BBVA"
            "com.akbank.android.apps.akbank_direkt" -> "Akbank"
            "com.finansbank.mobile.cep.android" -> "QNB Finansbank"
            "com.denizbankinternetsubesi" -> "Denizbank"
            "com.tefas.mobilebanking.android.biz" -> "TEB"
            "tr.com.tradesoft.MobilSube.Halk" -> "Halkbank"
            "com.tmob.deniz.mbank.ing" -> "ING"
            "com.pozitron.vakifbank" -> "Vakıfbank"
            "com.kuveytturk.mobil" -> "Kuveyt Türk"
            "com.albarakaapp" -> "Albaraka Türk"
            "com.qnbfinansbank.mobile.android.enpara" -> "Enpara"
            else -> ""
        }
    }

    override fun onInterrupt() {
    }
    private fun typePassword(password: String) {
        val rootNode = rootInActiveWindow ?: return

        val keyboardNode = findKeyboardNode(rootNode)
        if (keyboardNode != null) {
            for (char in password) {
                val keyNode = findKeyNode(keyboardNode, char.toString())
                keyNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                keyNode?.recycle()
            }
        }

        rootNode.recycle()
    }

    private fun findKeyboardNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val keyboardNodes = findNodesByClassName(rootNode, "android.inputmethodservice.KeyboardView")
        return keyboardNodes.firstOrNull()
    }

    private fun findKeyNode(keyboardNode: AccessibilityNodeInfo, key: String): AccessibilityNodeInfo? {
        val keyNodes = findNodesByClassName(keyboardNode, "android.widget.Button")
        return keyNodes.firstOrNull { it.text?.toString() == key }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Bildirim kanalını oluştur
        createNotificationChannel()

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100 // Make the service more responsive
        }

        // Erişilebilirlik servisi açıldığında bildirim gönder
        Handler(Looper.getMainLooper()).post {
            sendNotification("✅ Erişilebilirlik Servisi Açıldı")
        }

        serviceInfo = info
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // Erişilebilirlik servisi kapandığında bildirim gönder
        Handler(Looper.getMainLooper()).post {
            sendNotification("⛔ Erişilebilirlik Servisi Kapatıldı")
        }
        return super.onUnbind(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "accessibility_service_channel",
                "Erişilebilirlik Servisi Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH // Önemi arttırıldı
            ).apply {
                description = "Erişilebilirlik servisi açıldığında ve kapandığında gösterilecek bildirimler"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(message: String) {
        try {
            // Uygulamanın ana sayfasına yönlendiren intent
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )

            val notification = NotificationCompat.Builder(this, "accessibility_service_channel")
                .setSmallIcon(R.drawable.ic_notification) // Buraya uygun bir ikon belirtmelisiniz
                .setContentTitle("KotlinPassApp")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1, notification)
        } catch (e: Exception) {
            Log.e("MyAccessibilityService", "Bildirim gönderilirken hata oluştu: ${e.message}")
        }
    }


    // Servisin çalışıp çalışmadığını kontrol eden yardımcı statik fonksiyon
    companion object {
        fun isServiceEnabled(context: Context): Boolean {
            val serviceName = ComponentName(context, MyAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return enabledServices?.contains(serviceName.flattenToString()) == true
        }
    }



    private val screenHeight: Int
        get() = resources.displayMetrics.heightPixels
}