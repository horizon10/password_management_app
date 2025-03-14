package com.example.kotlinpassapp

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.widget.SearchView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast

class AddPasswordDialog(
    context: Context,
    private val dbHelper: DatabaseHelper,
    private val adapter: PasswordAdapter
) : Dialog(context) {

    private lateinit var appNameSpinner: Spinner
    private lateinit var searchView: SearchView
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var appList = arrayOf(
        "E-imza",
        "Ziraat Bankasi",
        "Yapi Kredi",
        "Nays",
        "Papara",
        "Paycell",
        "Is Bankasi",
        "Kuveyt Turk",
        "Halkbank",
        "Garanti BBVA",
        "Akbank",
        "QNB Finansbank",
        "ING",
        "Denizbank",
        "TEB",
        "Vakıfbank",
        "Albaraka Turk",
        "Enpara",
        "Discord",
        "Aliexpress",
        "Amazon",
        "EA",
        "E-Devlet",
        "Epic Games",
        "GitHub",
        "Chat GPT",
        "Hepsiburada",
        "Instagram",
        "Istanbul Kart",
        "KYK Internet",
        "LinkedIn",
        "Mail Hesabi",
        "Mavi",
        "Mc Donald's",
        "Notion",
        "Rockstar",
        "Steam",
        "Trendyol",
        "Ubisoft Connect",
        "Udemy",
        "X",
        "Diger"
    )
    private var filteredAppList = appList.toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_add_password)

        // Bileşenleri buluyoruz
        appNameSpinner = findViewById(R.id.appNameSpinner)
        searchView = findViewById(R.id.searchView)
        val appNameInput = findViewById<EditText>(R.id.appNameEditText)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val normalRadioInput = findViewById<RadioButton>(R.id.normalAppRadioButton)
        val bankRadioInput = findViewById<RadioButton>(R.id.bankAppRadioButton)
        val addButton = findViewById<Button>(R.id.addButton)

        // Spinner adapter'ını oluşturuyoruz
        spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, filteredAppList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        appNameSpinner.adapter = spinnerAdapter

        // SearchView'a listener ekliyoruz
        setupSearchView()

        // "Diğer" seçeneği seçildiğinde EditText görünür olacak
        appNameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = appNameSpinner.selectedItem.toString()
                appNameInput.visibility = if (selectedItem == "Diger") {
                    View.VISIBLE // "Diğer" seçildiğinde EditText görünür olacak
                } else {
                    View.GONE // Diğer seçeneklerde EditText gizlenecek
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Hiçbir şey seçilmediğinde yapılacak bir şey yok
            }
        }

        // Ekle butonuna tıklama işlemi
        addButton.setOnClickListener {
            val appName: String = if (appNameInput.visibility == View.VISIBLE) {
                appNameInput.text.toString().trim() // "Diğer" seçildiyse kullanıcının girdiği ad
            } else {
                appNameSpinner.selectedItem.toString() // Spinner'dan seçilen değer
            }

            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // RadioButton'dan seçim
            val appType = when {
                bankRadioInput.isChecked -> "Banka Uygulaması"
                normalRadioInput.isChecked -> "Normal Uygulama"
                else -> ""
            }

            // Alanların boş olup olmadığını kontrol ediyoruz
            if (appName.isEmpty() || username.isEmpty() || password.isEmpty() || appType.isEmpty()) {
                Toast.makeText(context, "Tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Eğer "Banka Uygulaması" seçildiyse ve aynı isimde bir kayıt varsa uyarı göster
            if (appType == "Banka Uygulaması" && dbHelper.isBankAppExists(appName)) {
                Toast.makeText(context, "$appName zaten kayıtlı!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Veritabanına ekleme işlemi
            dbHelper.addPassword(appName, username, password, appType)

            // Adapter'ı güncelleme işlemi
            adapter.updateData(dbHelper.getAllPasswords())

            // Dialog'u kapatma
            dismiss()
        }
    }

    private fun setupSearchView() {
        // SearchView'a listener ekliyoruz
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterAppList(newText)
                return true
            }
        })

        // SearchView iptal edildiğinde tüm listeyi göster
        searchView.setOnCloseListener {
            filterAppList("")
            false
        }
    }

    private fun filterAppList(query: String?) {
        filteredAppList.clear()

        if (query.isNullOrEmpty()) {
            filteredAppList.addAll(appList)
        } else {
            val lowercaseQuery = query.lowercase()
            appList.filter { it.lowercase().contains(lowercaseQuery) }
                .forEach { filteredAppList.add(it) }

            // "Diger" seçeneği her zaman listenin sonunda olmalı
            if (!"diger".contains(lowercaseQuery) && "Diger" !in filteredAppList) {
                filteredAppList.add("Diger")
            }
        }

        spinnerAdapter.notifyDataSetChanged()
    }
}