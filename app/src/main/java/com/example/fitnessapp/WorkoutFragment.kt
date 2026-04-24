package com.example.fitnessapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class WorkoutVideo(
    val title: String,
    val category: String,
    val durationMin: Int,
    val difficulty: String,
    val youtubeUrl: String,
    val calories: Int
)

class WorkoutFragment : Fragment() {

    private val allVideos = listOf(
        WorkoutVideo("15-Min Full Body HIIT",        "HIIT",       15, "Beginner",     "https://www.youtube.com/watch?v=ml6cT4AZdqI", 150),
        WorkoutVideo("30-Min Intense HIIT Cardio",   "HIIT",       30, "Intermediate", "https://www.youtube.com/watch?v=M0uO8X3_tEA", 300),
        WorkoutVideo("20-Min HIIT No Equipment",     "HIIT",       20, "Intermediate", "https://www.youtube.com/watch?v=cbKkB3POqaY", 200),
        WorkoutVideo("45-Min Advanced HIIT",         "HIIT",       45, "Advanced",     "https://www.youtube.com/watch?v=TkaYafQ-XC4", 450),

        WorkoutVideo("Morning Yoga for Beginners",   "Yoga",       20, "Beginner",     "https://www.youtube.com/watch?v=sTANio_2E0Q", 80),
        WorkoutVideo("Full Body Yoga Flow",          "Yoga",       30, "Intermediate", "https://www.youtube.com/watch?v=v7AYKMP6rOE", 120),
        WorkoutVideo("Yoga for Flexibility",         "Yoga",       25, "Beginner",     "https://www.youtube.com/watch?v=4pKly2JojMw", 90),
        WorkoutVideo("Power Yoga — Core + Balance",  "Yoga",       40, "Advanced",     "https://www.youtube.com/watch?v=9kOCY0KNByw", 200),

        WorkoutVideo("Beginner Bodyweight Strength", "Strength",   25, "Beginner",     "https://www.youtube.com/watch?v=UBMk30rjy0o", 180),
        WorkoutVideo("Upper Body Dumbbell Workout",  "Strength",   30, "Intermediate", "https://www.youtube.com/watch?v=vc1E5CfRfos", 220),
        WorkoutVideo("Lower Body Strength Training", "Strength",   35, "Intermediate", "https://www.youtube.com/watch?v=Dy28eq2PjcM", 250),
        WorkoutVideo("Full Body Strength (Advanced)","Strength",   50, "Advanced",     "https://www.youtube.com/watch?v=oAPCPjnU1wA", 380),

        WorkoutVideo("Low-Impact Cardio Walk",       "Cardio",     20, "Beginner",     "https://www.youtube.com/watch?v=fnJDOcs_nu8", 120),
        WorkoutVideo("Jump Rope Cardio Blast",       "Cardio",     15, "Intermediate", "https://www.youtube.com/watch?v=1BZM2Vre5oc", 180),
        WorkoutVideo("Step Aerobics for Beginners",  "Cardio",     30, "Beginner",     "https://www.youtube.com/watch?v=1ZkNE12LDGA", 200),
        WorkoutVideo("Boxing Cardio Workout",        "Cardio",     25, "Intermediate", "https://www.youtube.com/watch?v=N5NKqitGXr0", 260),

        WorkoutVideo("5-Min Morning Stretch",        "Stretching", 5,  "Beginner",     "https://www.youtube.com/watch?v=UiDqMKSgcOY", 20),
        WorkoutVideo("Full Body Stretch + Cool Down","Stretching", 15, "Beginner",     "https://www.youtube.com/watch?v=L_xrDAtykMI", 40),
        WorkoutVideo("Deep Stretch for Tight Muscles","Stretching",20, "Intermediate", "https://www.youtube.com/watch?v=qULTwquOuT4", 50),

        WorkoutVideo("Zumba Beginner — 30 Min",      "Dance",      30, "Beginner",     "https://www.youtube.com/watch?v=Hb0OsOlMNBM", 200),
        WorkoutVideo("Bollywood Dance Workout",      "Dance",      25, "Beginner",     "https://www.youtube.com/watch?v=yYGqMJyGjIg", 180)
    )

    private val categories = listOf("All", "HIIT", "Yoga", "Strength", "Cardio", "Stretching", "Dance")
    private var selectedCategory = "All"
    private var completedVideos  = mutableSetOf<String>()
    private var activeTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workout, container, false)

        val categorySpinner = view.findViewById<Spinner?>(R.id.spinner_category)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner?.adapter = adapter
        categorySpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedCategory = categories[pos]
                refreshVideoList(view)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val timerText      = view.findViewById<TextView?>(R.id.tv_workout_timer)
        val timerStart     = view.findViewById<Button?>(R.id.btn_timer_start)
        val timerResume    = view.findViewById<Button?>(R.id.btn_timer_resume)
        val timerStop      = view.findViewById<Button?>(R.id.btn_timer_stop)
        val timerReset     = view.findViewById<Button?>(R.id.btn_timer_reset)
        var timerMinutes   = 20
        var timeLeftMs     = 0L

        val timerMinus = view.findViewById<Button?>(R.id.btn_timer_minus)
        val timerPlus  = view.findViewById<Button?>(R.id.btn_timer_plus)
        val timerSetLabel = view.findViewById<TextView?>(R.id.tv_timer_set)

        fun updateTimerLabel() { 
            timerSetLabel?.text = "$timerMinutes min" 
            if (activeTimer == null && timerResume?.visibility != View.VISIBLE) {
                timerText?.text = "%02d:00".format(timerMinutes)
            }
        }
        updateTimerLabel()

        timerMinus?.setOnClickListener {
            if (timerMinutes > 5) { timerMinutes -= 5; updateTimerLabel() }
        }
        timerPlus?.setOnClickListener {
            timerMinutes += 5; updateTimerLabel()
        }

        fun startTimer(durationMs: Long) {
            activeTimer?.cancel()
            timerMinus?.isEnabled = false
            timerPlus?.isEnabled = false
            activeTimer = object : CountDownTimer(durationMs, 1000) {
                override fun onTick(msLeft: Long) {
                    timeLeftMs = msLeft
                    val m = msLeft / 60000
                    val s = (msLeft % 60000) / 1000
                    timerText?.text = "%02d:%02d".format(m, s)
                }
                override fun onFinish() {
                    timerText?.text = "Done! 🏆"
                    timerStart?.visibility = View.VISIBLE
                    timerResume?.visibility = View.GONE
                    timerStop?.visibility = View.GONE
                    timerMinus?.isEnabled = true
                    timerPlus?.isEnabled = true
                    activeTimer = null
                    Toast.makeText(context, "Workout complete!", Toast.LENGTH_LONG).show()
                }
            }.start()
        }

        timerStart?.setOnClickListener {
            val totalMs = timerMinutes * 60 * 1000L
            timeLeftMs = totalMs
            startTimer(totalMs)
            timerStart.visibility = View.GONE
            timerResume?.visibility = View.GONE
            timerStop?.visibility = View.VISIBLE
        }
        timerStop?.setOnClickListener {
            activeTimer?.cancel()
            activeTimer = null
            timerResume?.visibility = View.VISIBLE
            timerStop?.visibility = View.GONE
            timerStart?.visibility = View.GONE
        }
        timerResume?.setOnClickListener {
            startTimer(timeLeftMs)
            timerResume.visibility = View.GONE
            timerStop?.visibility = View.VISIBLE
        }
        timerReset?.setOnClickListener {
            activeTimer?.cancel()
            activeTimer = null
            timeLeftMs = 0
            timerText?.text = "%02d:00".format(timerMinutes)
            timerStart?.visibility = View.VISIBLE
            timerResume?.visibility = View.GONE
            timerStop?.visibility = View.VISIBLE
            timerMinus?.isEnabled = true
            timerPlus?.isEnabled = true
        }

        refreshVideoList(view)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeTimer?.cancel()
    }

    private fun refreshVideoList(view: View) {
        val container = view.findViewById<LinearLayout?>(R.id.ll_video_list) ?: return
        container.removeAllViews()

        val filtered = if (selectedCategory == "All") allVideos
                       else allVideos.filter { it.category == selectedCategory }

        val completedCount = view.findViewById<TextView?>(R.id.tv_completed_count)
        completedCount?.text = "Completed today: ${completedVideos.size}"

        filtered.forEach { video -> container.addView(buildVideoCard(video)) }
    }

    private fun buildVideoCard(video: WorkoutVideo): View {
        val ctx = requireContext()
        val isDone = completedVideos.contains(video.title)

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
            layoutParams = lp
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        }

        val titleText = TextView(ctx).apply {
            text = if (isDone) "✅ ${video.title}" else video.title
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val metaText = TextView(ctx).apply {
            text = "${video.category}  •  ${video.difficulty}  •  ${video.durationMin} min  •  ~${video.calories} kcal"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#757575"))
            setPadding(0, 4, 0, 8)
        }

        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val watchBtn = Button(ctx).apply {
            text = "▶  Watch"
            setOnClickListener { openVideo(video.youtubeUrl) }
        }

        val doneBtn = Button(ctx).apply {
            text = if (isDone) "Logged" else "Mark done"
            isEnabled = !isDone
            setOnClickListener {
                val user = SupabaseManager.client.auth.currentUserOrNull()
                if (user != null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            SupabaseManager.client.from("exercise_logs").insert(
                                buildJsonObject {
                                    put("user_id", user.id)
                                    put("exercise_name", video.title)
                                    put("calories_burned", video.calories)
                                    put("duration_min", video.durationMin)
                                    put("category", video.category)
                                }
                            )
                            completedVideos.add(video.title)
                            Toast.makeText(ctx, "Saved to Supabase!", Toast.LENGTH_SHORT).show()
                            refreshVideoList(this@WorkoutFragment.view ?: return@launch)
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Sync failed, saved locally", Toast.LENGTH_SHORT).show()
                            completedVideos.add(video.title)
                            refreshVideoList(this@WorkoutFragment.view ?: return@launch)
                        }
                    }
                } else {
                    completedVideos.add(video.title)
                    refreshVideoList(this@WorkoutFragment.view ?: return@setOnClickListener)
                }
            }
        }

        btnRow.addView(watchBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        btnRow.addView(doneBtn,  LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        card.addView(titleText)
        card.addView(metaText)
        card.addView(btnRow)

        return card
    }

    private fun openVideo(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
