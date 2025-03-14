package com.example.kotlinpassapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.kotlinpassapp.databinding.ActivityLoginBinding
import com.example.kotlinpassapp.databinding.DialogChangePasswordBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // View Binding ile layout'u bağla
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Parmak izi doğrulama ayarlarını başlat
        setupBiometricAuthentication()

        // Parmak izi ikonu tıklanma olayı
        binding.biometricButton.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }

        // Şifre ikonu tıklanma olayı
        binding.passwordButton.setOnClickListener {
            startActivity(Intent(this, PasswordLoginActivity::class.java))
        }


        // Uygulama açıldığında otomatik parmak izi doğrulaması başlat
        biometricPrompt.authenticate(promptInfo)
    }

    private fun setupBiometricAuthentication() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext, "Doğrulama başarılı!", Toast.LENGTH_SHORT).show()
                goToPasswordListActivity()
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
            .setNegativeButtonText("İptal")
            .build()
    }

    private fun authenticateForPasswordChange() {
        val executor = ContextCompat.getMainExecutor(this)

        val passwordChangePrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Başarılı doğrulama sonrası şifre değiştirme modalını göster
                showChangePasswordDialog()
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

        val passwordChangePromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Şifre Değiştirme")
            .setSubtitle("Şifre değiştirmek için kimliğinizi doğrulayın")
            .setNegativeButtonText("İptal")
            .build()

        passwordChangePrompt.authenticate(passwordChangePromptInfo)
    }

    private fun showChangePasswordDialog() {
        // Dialog için binding oluştur
        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(this))

        // AlertDialog oluştur
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Dialog butonlarını ayarla
        dialogBinding.saveNewPasswordButton.setOnClickListener {
            val newPassword = dialogBinding.newPasswordEditText.text.toString()
            val confirmPassword = dialogBinding.confirmPasswordEditText.text.toString()

            when {
                newPassword.isEmpty() -> {
                    dialogBinding.newPasswordEditText.error = "Şifre boş olamaz!"
                }
                confirmPassword.isEmpty() -> {
                    dialogBinding.confirmPasswordEditText.error = "Şifreyi doğrulayın!"
                }
                newPassword != confirmPassword -> {
                    dialogBinding.confirmPasswordEditText.error = "Şifreler eşleşmiyor!"
                }
                else -> {
                    // Şifreyi kaydet
                    sharedPreferences.edit().putString("user_password", newPassword).apply()
                    Toast.makeText(this, "Şifre başarıyla değiştirildi!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Modalı göster
        dialog.show()
    }

    private fun goToPasswordListActivity() {
        val intent = Intent(this, PasswordListActivity::class.java)
        startActivity(intent)
        finish()
    }
}