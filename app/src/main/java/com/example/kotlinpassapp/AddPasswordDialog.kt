package com.example.kotlinpassapp

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore.Audio.Radio
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_add_password)

        // Bileşenleri buluyoruz
        val appNameSpinner = findViewById<Spinner>(R.id.appNameSpinner)
        val appNameInput = findViewById<EditText>(R.id.appNameEditText)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val normalRadioInput=findViewById<RadioButton>(R.id.normalAppRadioButton)
        val bankRadioInput=findViewById<RadioButton>(R.id.bankAppRadioButton)
        val addButton = findViewById<Button>(R.id.addButton)

        // Spinner'da gösterilecek uygulama isimleri
        val appList = arrayOf(
            "Ziraat Bankasi",
            "Yapi Kredi",
            "Nays",
            "Papara",
            "Paycell",
            "Is Bankasi",
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

        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, appList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        appNameSpinner.adapter = spinnerAdapter

        // "Diğer" seçeneği seçildiğinde EditText görünür olacak
        appNameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                appNameInput.visibility = if (position == appList.size - 1) {
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

            // Veritabanına ekleme işlemi
            dbHelper.addPassword(appName, username, password,appType)

            // Adapter'ı güncelleme işlemi
            adapter.updateData(dbHelper.getAllPasswords())

            // Dialog'u kapatma
            dismiss()
        }
    }
}
