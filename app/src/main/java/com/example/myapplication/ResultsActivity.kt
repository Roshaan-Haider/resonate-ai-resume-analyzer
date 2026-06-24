package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityMainBinding
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import com.example.myapplication.databinding.ActivityResultsBinding

class ResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Read the data out of the Intent
        val score = intent.getStringExtra("SCORE") ?: "0"
        val strengths = intent.getStringExtra("STRENGTHS") ?: "No strengths found."
        val weaknesses = intent.getStringExtra("WEAKNESSES") ?: "No weaknesses found."
        val suggestions = intent.getStringExtra("SUGGESTIONS") ?: "No suggestions found."

        // Populate the views
        binding.tvScore.text = "$score/10"
        binding.tvStrengths.text = strengths
        binding.tvWeaknesses.text = weaknesses
        binding.tvSuggestions.text = suggestions

        // Copy button
        binding.btnCopyResults.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(
                "Resume Analysis",
                "Score: $score/10\n\nStrengths:\n$strengths\n\nWeaknesses:\n$weaknesses\n\nSuggestions:\n$suggestions"
            )
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(this, "Results copied!", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Analyze Another button — goes back to MainActivity
        binding.btnAnalyzeAnother.setOnClickListener {
            finish()
        }
    }
}