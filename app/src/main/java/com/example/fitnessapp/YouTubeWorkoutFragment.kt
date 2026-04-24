package com.example.fitnessapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class YouTubeWorkoutFragment : Fragment() {

    // ── Use BuildConfig for API Key ───────────────────────────────────────
    private val YOUTUBE_API_KEY = BuildConfig.YOUTUBE_API_KEY

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── User preference state ─────────────────────────────────────────────
    private var selectedCategory = "HIIT"
    private var selectedLevel    = "Beginner"
    private var selectedDuration = "Any"

    // Video card list container (set in onCreateView)
    private var videoListContainer: LinearLayout? = null
    private var searchStatus: TextView? = null

    private val categories = listOf("HIIT", "Yoga", "Strength", "Cardio", "Stretching", "Dance", "Abs", "Zumba")
    private val levels     = listOf("Beginner", "Intermediate", "Advanced", "Any Level")
    private val durations  = listOf("Any", "Short (<10 min)", "Medium (10–30 min)", "Long (>30 min)")

    // YouTube duration filter: videoDuration param
    // short = <4min, medium = 4-20min, long = >20min
    private val durationParam = mapOf(
        "Any" to "any",
        "Short (<10 min)" to "short",
        "Medium (10–30 min)" to "medium",
        "Long (>30 min)" to "long"
    )

    data class YTVideo(
        val videoId: String,
        val title: String,
        val channelTitle: String,
        val thumbnailUrl: String,
        val duration: String   // formatted like "PT20M30S" → we parse to "20:30"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Load saved preferences
        val prefs = requireContext().getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
        selectedCategory = prefs.getString("yt_pref_category", "HIIT") ?: "HIIT"
        selectedLevel    = prefs.getString("yt_pref_level",    "Beginner") ?: "Beginner"
        selectedDuration = prefs.getString("yt_pref_duration", "Any") ?: "Any"

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // ── Header ─────────────────────────────────────────────────────────
        val header = TextView(requireContext()).apply {
            text = "Workout Videos"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(32, 28, 32, 4)
        }
        root.addView(header)

        // ── Preference filters ─────────────────────────────────────────────
        val filterSection = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 8)
        }

        // Category spinner
        filterSection.addView(makeLabel("Category"))
        val catSpinner = Spinner(requireContext())
        catSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        catSpinner.setSelection(categories.indexOf(selectedCategory).coerceAtLeast(0))
        filterSection.addView(catSpinner)

        // Level spinner
        filterSection.addView(makeLabel("Difficulty Level"))
        val levelSpinner = Spinner(requireContext())
        levelSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, levels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        levelSpinner.setSelection(levels.indexOf(selectedLevel).coerceAtLeast(0))
        filterSection.addView(levelSpinner)

        // Duration spinner
        filterSection.addView(makeLabel("Duration"))
        val durationSpinner = Spinner(requireContext())
        durationSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, durations)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        durationSpinner.setSelection(durations.indexOf(selectedDuration).coerceAtLeast(0))
        filterSection.addView(durationSpinner)

        // Search button
        val searchBtn = Button(requireContext()).apply {
            text = "Search Workouts"
            setBackgroundColor(Color.parseColor("#9B59B6"))
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
            layoutParams = lp
        }
        filterSection.addView(searchBtn)
        root.addView(filterSection)

        // Status text
        searchStatus = TextView(requireContext()).apply {
            text = "Select preferences and tap Search"
            textSize = 14f
            setTextColor(Color.parseColor("#757575"))
            gravity = android.view.Gravity.CENTER
            setPadding(32, 8, 32, 8)
        }
        root.addView(searchStatus)

        // Scrollable video list
        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        videoListContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 32)
        }
        scrollView.addView(videoListContainer)
        root.addView(scrollView)

        // ── Search button listener ─────────────────────────────────────────
        searchBtn.setOnClickListener {
            selectedCategory = categories[catSpinner.selectedItemPosition]
            selectedLevel    = levels[levelSpinner.selectedItemPosition]
            selectedDuration = durations[durationSpinner.selectedItemPosition]

            // Save preference for next time
            prefs.edit()
                .putString("yt_pref_category", selectedCategory)
                .putString("yt_pref_level", selectedLevel)
                .putString("yt_pref_duration", selectedDuration)
                .apply()

            searchYouTube(selectedCategory, selectedLevel, selectedDuration)
        }

        // Auto-load last preference on open
        searchYouTube(selectedCategory, selectedLevel, selectedDuration)

        return root
    }

    // ── Build YouTube search query ─────────────────────────────────────────
    private fun buildQuery(category: String, level: String): String {
        val levelStr = if (level == "Any Level") "" else "$level "
        return "${levelStr}$category workout"
    }

    // ── YouTube Data API v3 search ─────────────────────────────────────────
    private fun searchYouTube(category: String, level: String, duration: String) {
        searchStatus?.text = "Searching YouTube…"
        videoListContainer?.removeAllViews()

        scope.launch {
            val videos = withContext(Dispatchers.IO) {
                fetchYouTubeVideos(category, level, duration)
            }
            if (videos.isEmpty()) {
                if (searchStatus?.text?.contains("Error") != true) {
                    searchStatus?.text = "No results found. Try different filters or check API key."
                }
                Toast.makeText(requireContext(), "Search returned no results. Check logs or API configuration.", Toast.LENGTH_LONG).show()
            } else {
                searchStatus?.text = "Found ${videos.size} videos for '$category – $level'"
                videos.forEach { video ->
                    videoListContainer?.addView(buildVideoCard(video))
                }
            }
        }
    }

    private suspend fun fetchYouTubeVideos(category: String, level: String, duration: String): List<YTVideo> {
        Log.d("YouTube", "Using API Key: ${YOUTUBE_API_KEY.take(5)}...${YOUTUBE_API_KEY.takeLast(5)}")
        return try {
            val query    = buildQuery(category, level)
            val encoded  = URLEncoder.encode(query, "UTF-8")
            val durParam = durationParam[duration] ?: "any"

            // YouTube Data API v3 — search endpoint
            val searchUrl = "https://www.googleapis.com/youtube/v3/search" +
                "?part=snippet" +
                "&q=$encoded" +
                "&type=video" +
                "&videoCategoryId=17" +   // 17 = Sports — keeps results relevant
                "&videoDuration=$durParam" +
                "&maxResults=10" +
                "&relevanceLanguage=en" +
                "&safeSearch=strict" +
                "&key=$YOUTUBE_API_KEY"

            val conn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 30_000
                readTimeout = 30_000
            }

            val code = conn.responseCode
            if (code != 200) {
                val errStream = conn.errorStream
                val err = if (errStream != null) BufferedReader(InputStreamReader(errStream)).readText() else "No error stream"
                Log.e("YouTube", "Search failed ($code): $err")

                val displayMsg = try {
                    JSONObject(err).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "Status $code: $err"
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "YouTube API: $displayMsg", Toast.LENGTH_LONG).show()
                    searchStatus?.text = "Error: $displayMsg"
                }
                return emptyList()
            }

            val body = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            val json = JSONObject(body)
            val items = json.getJSONArray("items")

            val videoIds = (0 until items.length()).map {
                items.getJSONObject(it).getJSONObject("id").getString("videoId")
            }

            // Fetch durations via videos endpoint
            val durations = fetchDurations(videoIds)

            (0 until items.length()).map { i ->
                val item    = items.getJSONObject(i)
                val snippet = item.getJSONObject("snippet")
                val videoId = item.getJSONObject("id").getString("videoId")
                YTVideo(
                    videoId      = videoId,
                    title        = snippet.getString("title"),
                    channelTitle = snippet.getString("channelTitle"),
                    thumbnailUrl = snippet.getJSONObject("thumbnails")
                        .optJSONObject("medium")?.getString("url") ?: "",
                    duration     = formatDuration(durations[videoId] ?: "")
                )
            }
        } catch (e: Exception) {
            Log.e("YouTube", "fetchYouTubeVideos error: ${e.message}")
            withContext(Dispatchers.Main) {
                val errorMsg = e.message ?: "Unknown error"
                searchStatus?.text = "Error: $errorMsg"
                Toast.makeText(requireContext(), "Network Error: $errorMsg", Toast.LENGTH_LONG).show()
            }
            emptyList()
        }
    }

    // Fetch ISO 8601 durations for a list of video IDs
    private fun fetchDurations(videoIds: List<String>): Map<String, String> {
        return try {
            val ids = videoIds.joinToString(",")
            val url = "https://www.googleapis.com/youtube/v3/videos" +
                "?part=contentDetails" +
                "&id=$ids" +
                "&key=$YOUTUBE_API_KEY"

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 8_000; readTimeout = 8_000
            }
            if (conn.responseCode != 200) return emptyMap()

            val body  = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            val items = JSONObject(body).getJSONArray("items")
            (0 until items.length()).associate {
                val obj = items.getJSONObject(it)
                obj.getString("id") to obj.getJSONObject("contentDetails").getString("duration")
            }
        } catch (e: Exception) { emptyMap() }
    }

    // Convert ISO 8601 "PT20M30S" → "20:30"
    private fun formatDuration(iso: String): String {
        if (iso.isEmpty()) return ""
        val hours   = Regex("(\\d+)H").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = Regex("(\\d+)M").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val seconds = Regex("(\\d+)S").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
               else "%d:%02d".format(minutes, seconds)
    }

    // ── Build a video card view ────────────────────────────────────────────
    private fun buildVideoCard(video: YTVideo): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
            layoutParams = lp
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            isClickable = true
            setOnClickListener { openVideo(video.videoId) }
        }

        // Thumbnail placeholder (gray box with duration overlay)
        val thumbFrame = FrameLayout(ctx).apply {
            val lp = LinearLayout.LayoutParams(140, 90)
            lp.marginEnd = 16
            layoutParams = lp
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        val thumbLabel = TextView(ctx).apply {
            text = "▶"
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val durationLabel = TextView(ctx).apply {
            text = video.duration
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#BB000000"))
            setPadding(6, 2, 6, 2)
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM or android.view.Gravity.END
            )
            lp.setMargins(0, 0, 4, 4)
            layoutParams = lp
        }
        thumbFrame.addView(thumbLabel)
        thumbFrame.addView(durationLabel)
        card.addView(thumbFrame)

        // Text info column
        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleText = TextView(ctx).apply {
            text = video.title
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            maxLines = 2
        }
        val channelText = TextView(ctx).apply {
            text = video.channelTitle
            textSize = 12f
            setTextColor(Color.parseColor("#757575"))
            setPadding(0, 4, 0, 0)
        }
        textCol.addView(titleText)
        textCol.addView(channelText)
        card.addView(textCol)

        return card
    }

    private fun openVideo(videoId: String) {
        // Try YouTube app first, fallback to browser
        val appUri = Uri.parse("vnd.youtube:$videoId")
        val webUri = Uri.parse("https://www.youtube.com/watch?v=$videoId")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, appUri))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    private fun makeLabel(text: String) = TextView(requireContext()).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.parseColor("#757575"))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 12; bottomMargin = 4 }
        layoutParams = lp
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }
}
