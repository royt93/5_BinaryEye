package com.mckimquyen.binaryeye.frm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.DialogInterface
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.ext.app.applyLocale
import com.mckimquyen.binaryeye.prefs

/**
 * First-time language selection dialog.
 * Hiển thị 1 lần duy nhất khi user cài app lần đầu.
 * Chọn ngôn ngữ → apply ngay bằng Activity.recreate().
 */
class FLanguageDialog : BottomSheetDialogFragment() {

    // Danh sách ngôn ngữ: Triple(flag emoji, tên bản ngữ, locale code)
    private val languages = listOf(
        Triple("🌐", "System Default", ""),
        Triple("🇺🇸", "English", "en"),
        Triple("🇻🇳", "Tiếng Việt", "vi"),
        Triple("🇯🇵", "日本語", "ja"),
        Triple("🇰🇷", "한국어", "ko"),
        Triple("🇨🇳", "简体中文", "zh-rCN"),
        Triple("🇹🇼", "繁體中文", "zh-TW"),
        Triple("🇩🇪", "Deutsch", "de"),
        Triple("🇫🇷", "Français", "fr"),
        Triple("🇪🇸", "Español", "es"),
        Triple("🇵🇹", "Português", "pt"),
        Triple("🇧🇷", "Português do Brasil", "pt-rBR"),
        Triple("🇷🇺", "Русский", "ru-rRU"),
        Triple("🇺🇦", "Українська", "uk"),
        Triple("🇮🇹", "Italiano", "it"),
        Triple("🇳🇱", "Nederlands", "nl"),
        Triple("🇵🇱", "Polski", "pl"),
        Triple("🇨🇿", "Čeština", "cs"),
        Triple("🇭🇺", "Magyar", "hu"),
        Triple("🇩🇰", "Dansk", "da"),
        Triple("🇹🇷", "Türkçe", "tr"),
        Triple("🇮🇩", "Bahasa Indonesia", "in"),
        Triple("🇸🇦", "العربية", "ar"),
        Triple("🇮🇳", "हिन्दी", "hi"),
        Triple("🇵🇰", "اردو", "ur"),
        Triple("🇮🇳", "தமிழ்", "ta"),
        Triple("🇮🇳", "తెలుగు", "te"),
        Triple("🇮🇳", "मराठी", "mr"),
        Triple("🇹🇭", "ภาษาไทย", "th"),
        Triple("🇧🇩", "বাংলা", "bn"),
        Triple("🇲🇾", "Bahasa Melayu", "ms"),
        Triple("🇲🇲", "မြန်မာ", "my"),
        Triple("🇰🇭", "ខ្មែរ", "km"),
        Triple("🇱🇦", "ລາວ", "lo"),
        Triple("🇮🇷", "فارسی", "fa"),
        Triple("🇮🇱", "עברית", "iw"),
        Triple("🇬🇷", "Ελληνικά", "el"),
        Triple("🇷🇴", "Română", "ro"),
        Triple("🇸🇪", "Svenska", "sv"),
        Triple("🇫🇮", "Suomi", "fi"),
        Triple("🇬🇪", "ქართული", "ka"),
    )

    private var selectedIndex = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as com.google.android.material.bottomsheet.BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.roy_frm_language_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-select current locale if already set
        val currentLocale = prefs.customLocale
        selectedIndex = languages.indexOfFirst { it.third == currentLocale }.coerceAtLeast(0)

        val rvLanguages = view.findViewById<RecyclerView>(R.id.rvLanguages)
        val adapter = LanguageAdapter(languages, selectedIndex) { index ->
            selectedIndex = index
            val chosen = languages[index]
            prefs.hasShownLanguageDialog = true

            if (chosen.third != prefs.customLocale) {
                prefs.customLocale = chosen.third
                requireContext().applyLocale(chosen.third)
                dismiss()
                activity?.recreate()
            } else {
                dismiss()
            }
        }
        rvLanguages.layoutManager = LinearLayoutManager(requireContext())
        rvLanguages.adapter = adapter
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Dam bao flag luon duoc set ke ca khi user vuot xuong hoac nhan back
        prefs.hasShownLanguageDialog = true
    }

    // Giai quyet crash tren cac thiet bi dung Theme.AppCompat
    override fun getTheme(): Int {
        return com.google.android.material.R.style.Theme_Design_Light_BottomSheetDialog
    }

    // ───────────────────────────────────────────────
    // Inner Adapter
    // ───────────────────────────────────────────────

    private class LanguageAdapter(
        private val items: List<Triple<String, String, String>>,
        private var selectedIndex: Int,
        private val onSelect: (Int) -> Unit,
    ) : RecyclerView.Adapter<LanguageAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvFlag: TextView = view.findViewById(R.id.tvFlag)
            val tvName: TextView = view.findViewById(R.id.tvLanguageName)
            val rbSelected: RadioButton = view.findViewById(R.id.rbSelected)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.roy_item_language, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (flag, name, _) = items[position]
            holder.tvFlag.text = flag
            holder.tvName.text = name
            holder.rbSelected.isChecked = position == selectedIndex
            holder.itemView.setOnClickListener {
                val prev = selectedIndex
                selectedIndex = holder.adapterPosition
                onSelect(selectedIndex)
                notifyItemChanged(prev)
                notifyItemChanged(selectedIndex)
            }
        }
    }

    companion object {
        const val TAG = "FLanguageDialog"
    }
}
