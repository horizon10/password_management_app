package com.example.kotlinpassapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SharedPreferences ile giriş kontrolü
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // Eğer kullanıcı daha önce giriş yaptıysa direkt ana sayfaya yönlendir
            goToPasswordListActivity()
        } else {
            // Parmak izi doğrulama hazırlığı
            setupBiometricPrompt()
        }
    }

    private fun setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Başarılı doğrulama sonrası
                sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()
                Toast.makeText(applicationContext, "Giriş başarılı!", Toast.LENGTH_SHORT).show()
                goToPasswordListActivity()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Hata: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Parmak izi doğrulaması başarısız.", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Parmak İzi Gerekli")
            .setSubtitle("Devam etmek için parmak izinizi doğrulayın.")
            .setNegativeButtonText("İptal")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun goToPasswordListActivity() {
        // Ana sayfaya yönlendirme işlemi
        val intent = Intent(this, PasswordListActivity::class.java)
        startActivity(intent)
        finish() // MainActivity'yi kapat
    }
}
