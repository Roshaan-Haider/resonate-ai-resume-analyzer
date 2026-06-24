package com.example.myapplication

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.animation.ObjectAnimator
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var loadingDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingDialog = Dialog(this).apply {
            setContentView(R.layout.dialog_loading)
            setCancelable(false)
            window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
        }

        binding.etResumeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                binding.tvCharCount.text = "$length/5000"
                binding.btnClear.setTextColor(android.graphics.Color.WHITE)
            }
        })

        binding.btnClear.setOnClickListener {
            binding.etResumeInput.text.clear()
        }

        binding.btnAnalyze.setOnClickListener {
            val resumeText = binding.etResumeInput.text.toString()

            if (resumeText.isBlank()) {
                return@setOnClickListener
            }

            loadingDialog.show()
            animateDots(loadingDialog)

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    ResumeAnalyzer.analyze(resumeText)
                }

                loadingDialog.dismiss()

                if (result.isSuccess) {
                    val rawResponse = result.getOrNull() ?: ""
                    val parsed = parseResponse(rawResponse)

                    val intent = Intent(this@MainActivity, ResultsActivity::class.java).apply {
                        putExtra("SCORE", parsed["score"] ?: "0")
                        putExtra("STRENGTHS", parsed["strengths"] ?: "Not found.")
                        putExtra("WEAKNESSES", parsed["weaknesses"] ?: "Not found.")
                        putExtra("SUGGESTIONS", parsed["suggestions"] ?: "Not found.")
                    }
                    startActivity(intent)

                } else {
                    val exception = result.exceptionOrNull()
                    val message = when {
                        exception?.message?.contains("Failed to connect") == true ->
                            "Can't reach LM Studio. Is the server running?"
                        exception?.message?.contains("timeout") == true ->
                            "Analysis timed out. Try a shorter resume."
                        else ->
                            "Something went wrong. Please try again."
                    }

                    showError(message) {
                        binding.btnAnalyze.performClick()
                    }
                }
            }
        }
    }

    private fun parseResponse(raw: String): Map<String, String> {
        val result = mutableMapOf<String, String>()

        val scoreRegex = Regex("SCORE:\\s*(\\d+)")
        val strengthsRegex = Regex("STRENGTHS:\\s*(.+?)(?=WEAKNESSES:|$)", RegexOption.DOT_MATCHES_ALL)
        val weaknessesRegex = Regex("WEAKNESSES:\\s*(.+?)(?=SUGGESTIONS:|$)", RegexOption.DOT_MATCHES_ALL)
        val suggestionsRegex = Regex("SUGGESTIONS:\\s*(.+?)$", RegexOption.DOT_MATCHES_ALL)

        result["score"] = scoreRegex.find(raw)?.groupValues?.get(1)?.trim() ?: "0"
        result["strengths"] = strengthsRegex.find(raw)?.groupValues?.get(1)?.trim() ?: raw
        result["weaknesses"] = weaknessesRegex.find(raw)?.groupValues?.get(1)?.trim() ?: ""
        result["suggestions"] = suggestionsRegex.find(raw)?.groupValues?.get(1)?.trim() ?: ""

        return result
    }

    private fun showError(message: String, onRetry: () -> Unit) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)

        snackbar.setAction("Retry") { onRetry() }
        snackbar.setBackgroundTint(
            ContextCompat.getColor(this, R.color.bg_surface_raised)
        )
        snackbar.setTextColor(
            ContextCompat.getColor(this, R.color.text_primary)
        )
        snackbar.setActionTextColor(
            ContextCompat.getColor(this, R.color.accent_primary)
        )
        snackbar.show()
    }

    private fun animateDots(dialog: Dialog) {
        val dot1 = dialog.findViewById<View>(R.id.dot1)
        val dot2 = dialog.findViewById<View>(R.id.dot2)
        val dot3 = dialog.findViewById<View>(R.id.dot3)

        listOf(dot1, dot2, dot3).forEachIndexed { index, dot ->
            val animator = ObjectAnimator.ofFloat(dot, "translationY", 0f, -20f, 0f).apply {
                duration = 600
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                startDelay = (index * 150).toLong()
            }
            animator.start()
        }
    }
}