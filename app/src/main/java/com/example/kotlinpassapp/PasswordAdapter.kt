package com.example.kotlinpassapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinpassapp.databinding.ItemPasswordBinding
import java.util.Locale

class PasswordAdapter(
    private var passwords: MutableList<Password>, // MutableList kullanıyoruz, çünkü listeyi güncelleyeceğiz
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder>() {

    inner class PasswordViewHolder(val binding: ItemPasswordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        val binding = ItemPasswordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PasswordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        val password = passwords[position]
        holder.binding.apply {
            // Verileri yerleştiriyoruz
            appNameTextView.text = password.appName
            if (password.appType.equals("banka uygulaması", ignoreCase = true)) {
                // Banka uygulaması için özel metinler
                usernameTextView.text = "Uygulama Şifresi: ${password.username}"
                passwordTextView.text = "Kart Şifresi: ${password.password}"
            } else {
                // Normal uygulamalar için metinler
                usernameTextView.text = "Kullanıcı Adı: ${password.username}"
                passwordTextView.text = "Şifre: ${password.password}"
            }
            // Uygulama adı ile logo belirleme
            val logoResId = getLogoForApp(password.appName)
            appLogoImageView.setImageResource(logoResId)

            // Butonlara tıklama işlemleri
            deleteButton.setOnClickListener { listener.onDeleteClick(password) }
            updateButton.setOnClickListener { listener.onUpdateClick(password) }
        }
    }

    override fun getItemCount(): Int = passwords.size

    // Yeni veriyi güncelleyen fonksiyon
    fun updateData(newPasswords: List<Password>) {
        // Eski veriyi temizlemeden önce listenin eşit olup olmadığını kontrol edelim
        if (passwords != newPasswords) {
            passwords.clear() // Eski veriyi temizliyoruz
            passwords.addAll(newPasswords) // Yeni veriyi ekliyoruz
            notifyDataSetChanged() // RecyclerView'ı güncelliyoruz
        }
    }


    private fun getLogoForApp(appName: String): Int {
        // Uygulama adını küçük harfe çevir ve özel karakterlerden (boşluk, tire, vb.) arındır
        val cleanedAppName = appName.toLowerCase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "") // Sadece küçük harfler ve rakamları bırakır, diğer her şeyi kaldırır


        return when (cleanedAppName) {
            "ziraatbankasi" -> R.drawable.zb_amblem
            "yapikredi" -> R.drawable.yk_amblem
            "nays" -> R.drawable.nays_logo
            "papara" -> R.drawable.papara_logo
            "paycell" -> R.drawable.paycell_logo
            "isbankasi" -> R.drawable.is_logo
            "discord" -> R.drawable.discord_logo
            "aliexpress" -> R.drawable.aliexpress_logo
            "amazon" -> R.drawable.amazon_logo
            "ea" -> R.drawable.ea_logo
            "edevlet" -> R.drawable.edevlet_logo
            "epicgames" -> R.drawable.epic_logo
            "github" -> R.drawable.github
            "chatgpt" -> R.drawable.gpt_logo
            "hepsiburada" -> R.drawable.hepsiburada_logo
            "instagram" -> R.drawable.instagram_logo
            "istanbulkart" -> R.drawable.istkart_logo
            "kykinternet" -> R.drawable.kyk_logo
            "linkedin" -> R.drawable.linkedin_logo
            "mailhesabi" -> R.drawable.mail_logo
            "mavi" -> R.drawable.mavi_logo
            "mcdonalds" -> R.drawable.mc_logo
            "notion" -> R.drawable.notion_logo
            "rockstar" -> R.drawable.rockstar_logo
            "steam" -> R.drawable.steam_logo
            "trendyol" -> R.drawable.trendyol_logo
            "ubisoftconnect" -> R.drawable.ubi_logo
            "udemy" -> R.drawable.udemy_logo
            "x" -> R.drawable.x_logo
            else -> R.drawable.default_logo // Bilinmeyen uygulama için default logo
        }
    }


    interface OnItemClickListener {
        fun onDeleteClick(password: Password)
        fun onUpdateClick(password: Password)
    }
}
