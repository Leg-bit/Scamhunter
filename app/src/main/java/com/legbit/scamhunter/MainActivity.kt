
package com.legbit.scamhunter

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var numberInput: EditText
    private lateinit var listContainer: LinearLayout
    private lateinit var statusText: TextView

    private val backgroundColor = Color.rgb(13, 18, 30)
    private val cardColor = Color.rgb(24, 32, 49)
    private val accentColor = Color.rgb(69, 160, 255)
    private val textColor = Color.rgb(239, 244, 255)
    private val mutedColor = Color.rgb(160, 174, 196)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildInterface()
        refreshBlocklist()
        refreshStatus()
    }

    private fun buildInterface() {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(backgroundColor)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }

        scroll.addView(root)
        setContentView(scroll)

        val title = TextView(this).apply {
            text = "ScamHunter"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Penyaring panggilan berbasis daftar blokir"
            textSize = 14f
            setTextColor(mutedColor)
        }
        root.addView(subtitle, marginParams(bottom = 20))

        val statusCard = card()
        statusText = TextView(this).apply {
            textSize = 15f
            setTextColor(textColor)
        }
        statusCard.addView(statusText)
        val activateButton = makeButton("Aktifkan penyaringan", accentColor)
        activateButton.setOnClickListener { requestScreeningRole() }
        statusCard.addView(activateButton, marginParams(top = 14))
        root.addView(statusCard, marginParams(bottom = 18))

        val inputCard = card()
        inputCard.addView(sectionTitle("Tambah nomor blokir"))

        numberInput = EditText(this).apply {
            hint = "Contoh: 081234567890"
            textSize = 16f
            setSingleLine(true)
            setTextColor(textColor)
            setHintTextColor(mutedColor)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(35, 45, 64), 10)
        }
        inputCard.addView(numberInput, marginParams(top = 12))

        val addButton = makeButton("Blokir nomor", accentColor)
        addButton.setOnClickListener { addNumber() }
        inputCard.addView(addButton, marginParams(top = 10))
        root.addView(inputCard, marginParams(bottom = 18))

        val listCard = card()
        listCard.addView(sectionTitle("Daftar nomor diblokir"))
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        listCard.addView(listContainer, marginParams(top = 12))
        root.addView(listCard, marginParams(bottom = 18))

        val note = TextView(this).apply {
            text = "Catatan: hanya nomor yang kamu tambahkan secara manual yang akan ditolak. Nomor lain tetap diizinkan. Periksa nomor sebelum memblokirnya."
            textSize = 13f
            setTextColor(mutedColor)
        }
        root.addView(note)
    }

    private fun addNumber() {
        val number = numberInput.text.toString().trim()
        if (number.isBlank()) {
            toast("Masukkan nomor terlebih dahulu.")
            return
        }
        if (!SpamDetector.addBlockedNumber(this, number)) {
            toast("Nomor terlalu pendek atau tidak valid.")
            return
        }
        numberInput.text.clear()
        refreshBlocklist()
        toast("Nomor ditambahkan ke daftar blokir.")
    }

    private fun refreshBlocklist() {
        if (!::listContainer.isInitialized) return
        listContainer.removeAllViews()
        val numbers = SpamDetector.getBlockedNumbers(this).sorted()

        if (numbers.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Belum ada nomor yang diblokir."
                textSize = 14f
                setTextColor(mutedColor)
            }
            listContainer.addView(empty)
            return
        }

        for (number in numbers) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }

            val label = TextView(this).apply {
                text = number
                textSize = 15f
                setTextColor(textColor)
            }
            row.addView(
                label,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )

            val removeButton = makeButton("Hapus", Color.rgb(180, 61, 72))
            removeButton.setOnClickListener {
                SpamDetector.removeBlockedNumber(this, number)
                refreshBlocklist()
                toast("Nomor dihapus dari daftar blokir.")
            }
            row.addView(removeButton)
            listContainer.addView(row)
        }
    }

    private fun refreshStatus() {
        if (!::statusText.isInitialized) return
        val active = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java)
            manager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }

        statusText.text = if (active) {
            "Status: penyaringan diaktifkan"
        } else {
            "Status: belum diaktifkan"
        }
    }

    private fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            toast("Fitur ini memerlukan Android 10 atau lebih baru.")
            return
        }

        val manager = getSystemService(RoleManager::class.java)
        if (manager == null ||
            !manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
        ) {
            toast("Fitur penyaringan panggilan tidak tersedia di perangkat ini.")
            return
        }

        if (manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            toast("ScamHunter sudah menjadi aplikasi penyaring panggilan.")
            refreshStatus()
            return
        }

        startActivityForResult(
            manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
            REQUEST_SCREENING_ROLE
        )
    }

    @Deprecated("Deprecated by Android API, retained for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREENING_ROLE) {
            refreshStatus()
            toast(
                if (resultCode == RESULT_OK) {
                    "Permintaan aktivasi diterima. Periksa statusnya."
                } else {
                    "Penyaringan belum diaktifkan."
                }
            )
        }
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(cardColor, 16)
        }
    }

    private fun sectionTitle(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }
    }

    private fun makeButton(label: String, color: Int): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = rounded(color, 10)
            minHeight = dp(44)
            setPadding(dp(14), dp(6), dp(14), dp(6))
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun marginParams(
        top: Int = 0,
        bottom: Int = 0
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_SCREENING_ROLE = 1001
    }
}
