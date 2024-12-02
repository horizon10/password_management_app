package com.example.kotlinpassapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinpassapp.databinding.ActivityChangePasswordBinding

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View Binding ile layout'u bağlayın
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Şifre değiştirme işlemlerini burada tanımlayın
        binding.saveNewPasswordButton.setOnClickListener {
            val newPassword = binding.newPasswordEditText.text.toString()
            if (newPassword.isNotEmpty()) {
                // Şifreyi kaydedin
                val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                sharedPreferences.edit().putString("user_password", newPassword).apply()
                finish() // Şifre başarıyla kaydedildiğinde bu ekranı kapatın
            } else {
                binding.newPasswordEditText.error = "Şifre boş olamaz!"
            }
        }
    }
}
