package com.example.kotlinpassapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.kotlinpassapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences

    private var isChangePasswordRequested = false // Şifre değiştirme isteği için bayrak

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View Binding ile layout'u bağla
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Parmak izi doğrulama ayarlarını başlat
        setupBiometricAuthentication()

        // Parmak izi ile giriş butonuna tıklanabilirlik ekle
        binding.biometricButton.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }

        // Şifre ile giriş butonuna tıklanabilirlik ekle
        binding.passwordButton.setOnClickListener {
            val inputPassword = binding.passwordEditText.text.toString()
            val savedPassword = sharedPreferences.getString("user_password", null)

            if (savedPassword == null) {
                // Eğer henüz şifre tanımlanmadıysa, kullanıcının şifre belirlemesine izin ver
                sharedPreferences.edit().putString("user_password", inputPassword).apply()
                Toast.makeText(this, "Şifre oluşturuldu!", Toast.LENGTH_SHORT).show()
                goToPasswordListActivity()
            } else if (inputPassword == savedPassword) {
                // Mevcut şifre doğru ise giriş yap
                goToPasswordListActivity()
            } else {
                // Yanlış şifre
                Toast.makeText(this, "Yanlış şifre!", Toast.LENGTH_SHORT).show()
            }
        }

        // Şifre değiştirme butonuna tıklanabilirlik ekle
        binding.changePasswordButton.setOnClickListener {
            isChangePasswordRequested = true // Şifre değiştirme isteği başlatılıyor
            biometricPrompt.authenticate(promptInfo)
        }

        // Uygulama ilk açıldığında otomatik olarak parmak izi doğrulaması başlatılacak
        biometricPrompt.authenticate(promptInfo)
    }

    private fun setupBiometricAuthentication() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext, "Doğrulama başarılı!", Toast.LENGTH_SHORT).show()

                // Şifre değiştirme isteği varsa şifre değiştirme ekranına yönlendir
                if (isChangePasswordRequested) {
                    isChangePasswordRequested = false // Bayrağı sıfırla
                    showChangePasswordDialog()
                } else {
                    // Aksi halde şifre listesi ekranına yönlendir
                    goToPasswordListActivity()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Doğrulama hatası: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Doğrulama başarısız. Tekrar deneyin.", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Parmak İzi ile Giriş")
            .setSubtitle("Kimliğinizi doğrulamak için parmak izinizi kullanın")
            .setNegativeButtonText("Şifre ile Giriş Yap")
            .build()
    }

    private fun showChangePasswordDialog() {
        // Şifre değiştirme için yeni bir Activity başlatılır
        val intent = Intent(this, ChangePasswordActivity::class.java)
        startActivity(intent)
    }

    private fun goToPasswordListActivity() {
        val intent = Intent(this, PasswordListActivity::class.java)
        startActivity(intent)
        finish()
    }
}
