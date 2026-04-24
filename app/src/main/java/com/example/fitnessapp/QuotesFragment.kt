package com.example.fitnessapp

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class QuotesFragment : Fragment() {

    private lateinit var quoteText: TextView
    private lateinit var authorText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var refreshButton: Button
    private lateinit var shareButton: Button
    private lateinit var rootLayout: LinearLayout

    private val gradients = listOf(
        intArrayOf(Color.parseColor("#8E24AA"), Color.parseColor("#3949AB")), // Purple to Indigo
        intArrayOf(Color.parseColor("#1E88E5"), Color.parseColor("#00ACC1")), // Blue to Cyan
        intArrayOf(Color.parseColor("#43A047"), Color.parseColor("#00897B")), // Green to Teal
        intArrayOf(Color.parseColor("#F4511E"), Color.parseColor("#FB8C00")), // Deep Orange to Orange
        intArrayOf(Color.parseColor("#546E7A"), Color.parseColor("#263238"))  // Blue Grey to Charcoal
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        updateBackground(0)

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 64, 48, 64)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            elevation = 10f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val icon = TextView(context).apply {
            text = "“"
            textSize = 80f
            setTextColor(Color.parseColor("#9B59B6"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, -40)
        }
        card.addView(icon)

        progressBar = ProgressBar(context).apply {
            visibility = View.GONE
        }
        card.addView(progressBar)

        quoteText = TextView(context).apply {
            text = "The only bad workout is the one that didn't happen."
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD_ITALIC)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        card.addView(quoteText)

        authorText = TextView(context).apply {
            text = "— Unknown"
            textSize = 14f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        card.addView(authorText)

        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        refreshButton = Button(context).apply {
            text = "Next Quote"
            setBackgroundColor(Color.parseColor("#9B59B6"))
            setTextColor(Color.WHITE)
        }
        refreshButton.setOnClickListener {
            fetchQuote()
        }
        
        shareButton = Button(context).apply {
            text = "Share"
            setTextColor(Color.parseColor("#9B59B6"))
            background = null // Text button style
        }
        shareButton.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${quoteText.text}\n${authorText.text}\n\nSent from Fitness Tracker")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Quote via"))
        }

        buttonContainer.addView(refreshButton)
        buttonContainer.addView(shareButton)
        card.addView(buttonContainer)
        
        rootLayout.addView(card)

        fetchQuote() // Initial fetch

        return rootLayout
    }

    private fun updateBackground(index: Int) {
        if (!::rootLayout.isInitialized) return
        val colors = gradients[index % gradients.size]
        val gd = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors)
        gd.cornerRadius = 0f
        rootLayout.background = gd
    }

    private fun fetchQuote() {
        progressBar.visibility = View.VISIBLE
        refreshButton.isEnabled = false
        quoteText.alpha = 0.3f

        lifecycleScope.launch {
            val quoteData = withContext(Dispatchers.IO) {
                try {
                    // Using a timestamp to bypass any caching and get a truly random quote every time
                    val timestamp = System.currentTimeMillis()
                    val url = URL("https://zenquotes.io/api/random?t=$timestamp")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val firstObject = jsonArray.getJSONObject(0)
                    
                    val q = firstObject.getString("q")
                    val a = firstObject.getString("a")
                    Pair(q, a)
                } catch (e: Exception) {
                    null
                }
            }

            progressBar.visibility = View.GONE
            refreshButton.isEnabled = true
            quoteText.alpha = 1.0f
            updateBackground((0..gradients.size-1).random())

            if (quoteData != null) {
                quoteText.text = quoteData.first
                authorText.text = "— ${quoteData.second}"
            } else {
                quoteText.text = "Success is not final, failure is not fatal: it is the courage to continue that counts."
                authorText.text = "— Winston Churchill"
            }
        }
    }
}
