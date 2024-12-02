package com.example.kotlinpassapp

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlinpassapp.databinding.ActivityPasswordListBinding
import com.example.kotlinpassapp.databinding.DialogUpdatePasswordBinding

class PasswordListActivity : AppCompatActivity(), PasswordAdapter.OnItemClickListener {

    private lateinit var binding: ActivityPasswordListBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: PasswordAdapter
    private val passwordList = mutableListOf<Password>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DatabaseHelper ve Adapter'ı başlatıyoruz
        dbHelper = DatabaseHelper(this)
        adapter = PasswordAdapter(passwordList, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Veritabanından verileri yüklüyoruz
        loadData()

        // Yeni şifre ekleme butonuna tıklama olayını dinliyoruz
        binding.addPasswordButton.setOnClickListener {
            showAddPasswordDialog()
        }
    }

    // Veritabanındaki şifreleri listeye ekliyoruz
    private fun loadData() {
        passwordList.clear()
        passwordList.addAll(dbHelper.getAllPasswords())
        adapter.notifyDataSetChanged()
    }

    // Şifre eklemek için diyaloğu gösteriyoruz
    private fun showAddPasswordDialog() {
        val dialog = AddPasswordDialog(this, dbHelper, adapter)
        dialog.show()
    }

    // Adapter'daki item tıklamaları burada işlenebilir
    override fun onDeleteClick(password: Password) {
        AlertDialog.Builder(this)
            .setTitle("Silme İşlemi")
            .setMessage("${password.appName} şifresini silmek istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                dbHelper.deletePassword(password.id)  // id ile silme işlemi
                loadData()  // Veritabanından güncel verileri yükle
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    override fun onUpdateClick(password: Password) {
        val dialogBinding = DialogUpdatePasswordBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Şifre Güncelle")
            .setView(dialogBinding.root)
            .setPositiveButton("Kaydet") { _, _ ->
                val newAppName = dialogBinding.appNameEditText.text.toString().trim()
                val newUsername = dialogBinding.usernameEditText.text.toString().trim()
                val newPassword = dialogBinding.passwordEditText.text.toString().trim()

                if (newAppName.isNotEmpty() && newUsername.isNotEmpty() && newPassword.isNotEmpty()) {
                    // Şifreyi güncelle
                    password.appName = newAppName
                    password.username = newUsername
                    password.password = newPassword

                    // Veritabanındaki şifreyi de güncelle
                    dbHelper.updatePassword(password.id, newPassword)

                    adapter.notifyDataSetChanged()  // Adapter'ı güncelle
                } else {
                    AlertDialog.Builder(this)
                        .setMessage("Lütfen tüm alanları doldurun!")
                        .setPositiveButton("Tamam", null)
                        .show()
                }
            }
            .setNegativeButton("İptal", null)
            .create()

        dialogBinding.appNameEditText.setText(password.appName)
        dialogBinding.usernameEditText.setText(password.username)
        dialogBinding.passwordEditText.setText(password.password)

        dialog.show()
    }
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .show()
    }
}
