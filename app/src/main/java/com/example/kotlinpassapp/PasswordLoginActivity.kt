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
import com.example.kotlinpassapp.databinding.ActivityPasswordLoginBinding
import com.example.kotlinpassapp.databinding.DialogChangePasswordBinding

class PasswordLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordLoginBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        setupBiometricAuthentication()

        // Giriş butonu tıklanma olayı
        binding.loginButton.setOnClickListener {
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

        // Şifre değiştirme butonu tıklanma olayı
        binding.changePasswordButton.setOnClickListener {
            // Şifre değiştirmek için önce biyometrik doğrulama iste
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun setupBiometricAuthentication() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext, "Doğrulama başarılı!", Toast.LENGTH_SHORT).show()
                // Şifre değiştirme modalını göster
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

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Parmak İzi Doğrulama")
            .setSubtitle("Şifre değiştirmek için kimliğinizi doğrulayın")
            .setNegativeButtonText("İptal")
            .build()
    }

    private fun showChangePasswordDialog() {
        // Dialog için binding oluştur
        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(this))

        // AlertDialog oluştur
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

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