package com.example.kotlinpassapp

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinpassapp.databinding.ActivityPasswordListBinding
import com.example.kotlinpassapp.databinding.DialogUpdatePasswordBinding

class PasswordListActivity : AppCompatActivity(), PasswordAdapter.OnItemClickListener {

    private lateinit var binding: ActivityPasswordListBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: PasswordAdapter
    private val passwordList = mutableListOf<Password>()
    private val allPasswordsList = mutableListOf<Password>() // To store all passwords for filtering

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        adapter = PasswordAdapter(passwordList, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadData()
        binding.addPasswordButton.setOnClickListener { showAddPasswordDialog() }
        setupItemTouchHelper()
        setupSearchView()

        // Ayarlar butonuna tıklanınca erişilebilirlik ayarlarına yönlendir
        binding.settingsButton.setOnClickListener {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Otomatik doldurma özelliği zaten etkin.", Toast.LENGTH_SHORT).show()
            } else {
                showAccessibilitySettingsDialog()
            }
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPasswords(newText)
                return true
            }
        })

        // Clear button in search view
        binding.searchView.setOnCloseListener {
            loadData()
            false
        }
    }

    private fun filterPasswords(query: String?) {
        if (query.isNullOrEmpty()) {
            passwordList.clear()
            passwordList.addAll(allPasswordsList)
        } else {
            val filteredList = allPasswordsList.filter { password ->
                password.appName.contains(query, ignoreCase = true)
                // Kullanıcı adına göre filtreleme kaldırıldı
            }
            passwordList.clear()
            passwordList.addAll(filteredList)
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadData() {
        allPasswordsList.clear()
        allPasswordsList.addAll(dbHelper.getAllPasswords())

        passwordList.clear()
        passwordList.addAll(allPasswordsList)
        adapter.notifyDataSetChanged()
    }

    private fun setupItemTouchHelper() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                passwordList.add(toPosition, passwordList.removeAt(fromPosition))
                adapter.notifyItemMoved(fromPosition, toPosition)
                saveOrderToDatabase()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled(): Boolean = true
        }

        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun saveOrderToDatabase() {
        for (i in passwordList.indices) {
            dbHelper.updatePasswordOrder(passwordList[i].id, i)
        }
    }

    private fun showAddPasswordDialog() {
        val dialog = AddPasswordDialog(this, dbHelper, adapter)
        dialog.show()
    }

    override fun onDeleteClick(password: Password) {
        AlertDialog.Builder(this)
            .setTitle("Silme İşlemi")
            .setMessage("${password.appName} şifresini silmek istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                dbHelper.deletePassword(password.id)
                loadData()
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
                    dbHelper.updatePassword(password.id, newPassword)
                    dbHelper.updateUsername(password.id, newUsername)
                    dbHelper.updateAppName(password.id, newAppName)
                    loadData()
                } else {
                    showErrorDialog("Lütfen tüm alanları doldurun!")
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

    // Erişilebilirlik servisinin etkin olup olmadığını kontrol et
    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = ComponentName(this, MyAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(serviceName.flattenToString()) == true
    }

    // Erişilebilirlik ayarlarına yönlendiren dialog göster
    private fun showAccessibilitySettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Otomatik Doldurma Ayarları")
            .setMessage(
                "Otomatik doldurma özelliğini etkinleştirmek için aşağıdaki adımları izleyin:\n\n" +
                        "1. 'Ayarlar' uygulamasına gidin.\n" +
                        "2. 'Erişilebilirlik' seçeneğine tıklayın.\n" +
                        "3. 'İndirilen Uygulamalar' bölümüne gidin.\n" +
                        "4. 'Şifre Yönetimi' seçeneğini bulun ve uygulamamızı etkinleştirin."
            )
            .setPositiveButton("Ayarlara Git") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}