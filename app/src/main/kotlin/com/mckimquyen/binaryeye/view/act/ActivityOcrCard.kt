package com.mckimquyen.binaryeye.view.act

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mckimquyen.binaryeye.BaseActivity
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.view.actions.vtype.vcard.BusinessCardParser
import com.mckimquyen.binaryeye.view.actions.vtype.vcard.VCardAction
import com.mckimquyen.binaryeye.view.widget.toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [FEAT F10] OCR anh danh thiep co san (khong dung camera truc tiep) -> tach
 * ten/SDT/email/cong ty/chuc danh bang BusinessCardParser -> cho sua tay
 * truoc khi them vao Danh ba. Tai su dung dung VCardAction/VTypeParser da co
 * san cho luong "them contact" thay vi tu viet lai.
 */
class ActivityOcrCard : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        if (uri == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            val lines = try {
                recognizeText(uri)
            } catch (e: Exception) {
                toast(R.string.ocr_failed)
                finish()
                return@launch
            }
            showConfirmDialog(BusinessCardParser.parse(lines))
        }
    }

    private suspend fun recognizeText(uri: Uri): List<String> =
        suspendCancellableCoroutine { cont ->
            val image = try {
                InputImage.fromFilePath(this, uri)
            } catch (e: Exception) {
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            }
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText.text.split("\n"))
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    @Suppress("InflateParams")
    private fun showConfirmDialog(card: BusinessCardParser.ParsedCard) {
        val pad = (16 * resources.displayMetrics.density).toInt()
        fun field(hintResId: Int, initial: String) = EditText(this).apply {
            setHint(hintResId)
            setText(initial)
        }
        val nameField = field(R.string.ocr_card_name_hint, card.name)
        val phoneField = field(R.string.ocr_card_phone_hint, card.phones.firstOrNull().orEmpty())
            .apply { inputType = InputType.TYPE_CLASS_PHONE }
        val emailField = field(R.string.ocr_card_email_hint, card.emails.firstOrNull().orEmpty())
            .apply {
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
        val companyField = field(R.string.ocr_card_company_hint, card.company)
        val titleField = field(R.string.ocr_card_title_hint, card.title)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(nameField)
            addView(phoneField)
            addView(emailField)
            addView(companyField)
            addView(titleField)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.ocr_card_dialog_title)
            .setView(container)
            .setPositiveButton(R.string.ocr_card_add_to_contacts) { _, _ ->
                val edited = BusinessCardParser.ParsedCard(
                    name = nameField.text.toString().trim(),
                    phones = listOfNotNull(
                        phoneField.text.toString().trim().takeIf { it.isNotEmpty() }
                    ),
                    emails = listOfNotNull(
                        emailField.text.toString().trim().takeIf { it.isNotEmpty() }
                    ),
                    company = companyField.text.toString().trim(),
                    title = titleField.text.toString().trim(),
                )
                lifecycleScope.launch {
                    VCardAction.execute(
                        this@ActivityOcrCard,
                        BusinessCardParser.toVCard(edited).toByteArray()
                    )
                    finish()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
