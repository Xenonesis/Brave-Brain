package com.bravebrain

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ScreentimeHistoryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyStateView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var firestoreService: FirestoreService
    private lateinit var statsCard: MaterialCardView
    private lateinit var statsContainer: LinearLayout
    private lateinit var tabLayout: TabLayout
    private var screenTimeHistory = mutableListOf<DailyScreenTime>()
    private var currentView = ViewType.DAILY
    
    enum class ViewType {
        DAILY, WEEKLY, MONTHLY, STATS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply theme before setContentView
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
        
        setContentView(R.layout.activity_screentime_history)
        
        // Set up toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Screentime History"
        
        firestoreService = FirestoreService(this)
        
        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewScreentimeHistory)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshScreentime)
        emptyStateView = findViewById(R.id.emptyStateText)
        progressBar = findViewById(R.id.progressBarScreentime)
        statsCard = findViewById(R.id.statsCard)
        statsContainer = findViewById(R.id.statsContainer)
        tabLayout = findViewById(R.id.tabLayout)
        
        // Set up tabs
        setupTabs()
        
        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ScreentimeHistoryAdapter(screenTimeHistory)
        
        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadScreentimeHistory()
        }
        
        // Load initial data
        loadScreentimeHistory()
    }
    
    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Daily"))
        tabLayout.addTab(tabLayout.newTab().setText("Weekly"))
        tabLayout.addTab(tabLayout.newTab().setText("Monthly"))
        tabLayout.addTab(tabLayout.newTab().setText("Stats"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        currentView = ViewType.DAILY
                        showDailyView()
                    }
                    1 -> {
                        currentView = ViewType.WEEKLY
                        showWeeklyView()
                    }
                    2 -> {
                        currentView = ViewType.MONTHLY
                        showMonthlyView()
                    }
                    3 -> {
                        currentView = ViewType.STATS
                        showStatsView()
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun loadScreentimeHistory() {
        showLoading(true)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firestoreService.getScreenTimeHistory(30) // Last 30 days
                
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { history ->
                            screenTimeHistory.clear()
                            screenTimeHistory.addAll(history)
                            
                            if (screenTimeHistory.isEmpty()) {
                                showEmptyState(true)
                            } else {
                                showEmptyState(false)
                                // Refresh current view
                                when (currentView) {
                                    ViewType.DAILY -> showDailyView()
                                    ViewType.WEEKLY -> showWeeklyView()
                                    ViewType.MONTHLY -> showMonthlyView()
                                    ViewType.STATS -> showStatsView()
                                }
                            }
                            
                            showLoading(false)
                            swipeRefreshLayout.isRefreshing = false
                        },
                        onFailure = { error ->
                            Log.e("ScreentimeHistory", "Error loading history: ${error.message}")
                            showEmptyState(true)
                            showLoading(false)
                            swipeRefreshLayout.isRefreshing = false
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("ScreentimeHistory", "Exception loading history: ${e.message}")
                withContext(Dispatchers.Main) {
                    showEmptyState(true)
                    showLoading(false)
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }
    
    private fun showDailyView() {
        statsCard.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.adapter = ScreentimeHistoryAdapter(screenTimeHistory)
        recyclerView.adapter?.notifyDataSetChanged()
    }
    
    private fun showWeeklyView() {
        statsCard.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        
        val weeklyStats = ScreentimeStatsHelper.groupByWeek(screenTimeHistory)
        recyclerView.adapter = WeeklyStatsAdapter(weeklyStats.values.toList().sortedByDescending { it.startDate })
        recyclerView.adapter?.notifyDataSetChanged()
    }
    
    private fun showMonthlyView() {
        statsCard.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        
        val monthlyStats = ScreentimeStatsHelper.groupByMonth(screenTimeHistory)
        recyclerView.adapter = MonthlyStatsAdapter(monthlyStats.values.toList().sortedByDescending { it.monthKey })
        recyclerView.adapter?.notifyDataSetChanged()
    }
    
    private fun showStatsView() {
        recyclerView.visibility = View.GONE
        statsCard.visibility = View.VISIBLE
        
        val overallStats = ScreentimeStatsHelper.calculateOverallStats(screenTimeHistory)
        displayOverallStats(overallStats)
    }
    
    private fun displayOverallStats(stats: OverallStats) {
        statsContainer.removeAllViews()
        
        // Add stats items
        addStatItem("📊 Total Period", "${stats.daysCount} days")
        addStatItem("⏱️ Total Screentime", ScreentimeStatsHelper.formatTime(stats.totalMinutes))
        addStatItem("📈 Average per Day", ScreentimeStatsHelper.formatTime(stats.averageMinutes))
        addStatItem("🔝 Highest Day", ScreentimeStatsHelper.formatTime(stats.maxDayMinutes))
        addStatItem("⬇️ Lowest Day", ScreentimeStatsHelper.formatTime(stats.minDayMinutes))
        
        // Add divider
        addDivider()
        
        // Add top apps section
        val topAppsTitle = TextView(this).apply {
            text = "🌟 Top Apps"
            textSize = 18f
            setTextColor(getColor(android.R.color.white))
            setPadding(0, 32, 0, 16)
        }
        statsContainer.addView(topAppsTitle)
        
        stats.topApps.entries.take(5).forEachIndexed { index, (appName, minutes) ->
            addTopAppItem(index + 1, appName, ScreentimeStatsHelper.formatTime(minutes))
        }
    }
    
    private fun addStatItem(label: String, value: String) {
        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
        }
        
        val labelView = TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(getColor(android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val valueView = TextView(this).apply {
            text = value
            textSize = 18f
            setTextColor(getColor(android.R.color.white))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        itemLayout.addView(labelView)
        itemLayout.addView(valueView)
        statsContainer.addView(itemLayout)
    }
    
    private fun addTopAppItem(rank: Int, appName: String, time: String) {
        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
        }
        
        val rankView = TextView(this).apply {
            text = "#$rank"
            textSize = 14f
            setTextColor(getColor(android.R.color.white))
            setPadding(0, 0, 16, 0)
            minWidth = 60
        }
        
        val nameView = TextView(this).apply {
            text = appName
            textSize = 16f
            setTextColor(getColor(android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val timeView = TextView(this).apply {
            text = time
            textSize = 16f
            setTextColor(getColor(android.R.color.white))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        itemLayout.addView(rankView)
        itemLayout.addView(nameView)
        itemLayout.addView(timeView)
        statsContainer.addView(itemLayout)
    }
    
    private fun addDivider() {
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                topMargin = 24
                bottomMargin = 8
            }
            setBackgroundColor(Color.parseColor("#40FFFFFF"))
        }
        statsContainer.addView(divider)
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun showEmptyState(show: Boolean) {
        emptyStateView.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    // Adapter for RecyclerView
    inner class ScreentimeHistoryAdapter(
        private val data: List<DailyScreenTime>
    ) : RecyclerView.Adapter<ScreentimeHistoryAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cardView: MaterialCardView = view.findViewById(R.id.cardViewScreentime)
            val dateText: TextView = view.findViewById(R.id.textViewDate)
            val screenTimeText: TextView = view.findViewById(R.id.textViewScreenTime)
            val topAppsText: TextView = view.findViewById(R.id.textViewTopApps)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_screentime_history, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            
            // Format date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
            val date = try {
                dateFormat.parse(item.date)
            } catch (e: Exception) {
                Date()
            }
            
            holder.dateText.text = displayFormat.format(date ?: Date())
            
            // Format screentime
            val hours = item.screenTimeMinutes / 60
            val minutes = item.screenTimeMinutes % 60
            holder.screenTimeText.text = if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
            
            // Display top apps
            if (item.topApps.isNotEmpty()) {
                val topAppsStr = item.topApps.take(3).joinToString("\n") { app ->
                    val appName = app["appName"] as? String ?: "Unknown"
                    val usageMin = (app["usageMinutes"] as? Number)?.toInt() ?: 0
                    "• $appName: ${usageMin}m"
                }
                holder.topAppsText.text = topAppsStr
                holder.topAppsText.visibility = View.VISIBLE
            } else {
                holder.topAppsText.visibility = View.GONE
            }
            
            // Color code based on usage
            val color = when {
                item.screenTimeMinutes < 120 -> android.graphics.Color.parseColor("#4CAF50") // Green
                item.screenTimeMinutes < 240 -> android.graphics.Color.parseColor("#FF9800") // Orange
                else -> android.graphics.Color.parseColor("#F44336") // Red
            }
            holder.screenTimeText.setTextColor(color)
        }
        
        override fun getItemCount() = data.size
    }
    
    // Adapter for Weekly Stats
    inner class WeeklyStatsAdapter(
        private val data: List<WeeklyStats>
    ) : RecyclerView.Adapter<WeeklyStatsAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cardView: MaterialCardView = view.findViewById(R.id.cardViewScreentime)
            val dateText: TextView = view.findViewById(R.id.textViewDate)
            val screenTimeText: TextView = view.findViewById(R.id.textViewScreenTime)
            val topAppsText: TextView = view.findViewById(R.id.textViewTopApps)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_screentime_history, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            
            // Format date range
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            
            val startDate = try {
                dateFormat.parse(item.startDate)
            } catch (e: Exception) {
                Date()
            }
            
            val endDate = try {
                dateFormat.parse(item.endDate)
            } catch (e: Exception) {
                Date()
            }
            
            holder.dateText.text = "Week: ${displayFormat.format(startDate ?: Date())} - ${displayFormat.format(endDate ?: Date())}"
            
            // Format screentime
            holder.screenTimeText.text = ScreentimeStatsHelper.formatTime(item.totalMinutes)
            
            // Display stats
            val statsStr = "📊 Avg: ${ScreentimeStatsHelper.formatTime(item.averageMinutes)}/day\n" +
                          "📈 Max: ${ScreentimeStatsHelper.formatTime(item.maxDayMinutes)}\n" +
                          "⬇️ Min: ${ScreentimeStatsHelper.formatTime(item.minDayMinutes)}\n" +
                          "📅 Days tracked: ${item.daysCount}"
            holder.topAppsText.text = statsStr
            holder.topAppsText.visibility = View.VISIBLE
            
            // Color code based on average usage
            val color = when {
                item.averageMinutes < 120 -> android.graphics.Color.parseColor("#4CAF50") // Green
                item.averageMinutes < 240 -> android.graphics.Color.parseColor("#FF9800") // Orange
                else -> android.graphics.Color.parseColor("#F44336") // Red
            }
            holder.screenTimeText.setTextColor(color)
        }
        
        override fun getItemCount() = data.size
    }
    
    // Adapter for Monthly Stats
    inner class MonthlyStatsAdapter(
        private val data: List<MonthlyStats>
    ) : RecyclerView.Adapter<MonthlyStatsAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cardView: MaterialCardView = view.findViewById(R.id.cardViewScreentime)
            val dateText: TextView = view.findViewById(R.id.textViewDate)
            val screenTimeText: TextView = view.findViewById(R.id.textViewScreenTime)
            val topAppsText: TextView = view.findViewById(R.id.textViewTopApps)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_screentime_history, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            
            holder.dateText.text = item.monthName
            
            // Format screentime
            holder.screenTimeText.text = ScreentimeStatsHelper.formatTime(item.totalMinutes)
            
            // Display stats
            val statsStr = "📊 Avg: ${ScreentimeStatsHelper.formatTime(item.averageMinutes)}/day\n" +
                          "📈 Max: ${ScreentimeStatsHelper.formatTime(item.maxDayMinutes)}\n" +
                          "⬇️ Min: ${ScreentimeStatsHelper.formatTime(item.minDayMinutes)}\n" +
                          "📅 Days tracked: ${item.daysCount}"
            holder.topAppsText.text = statsStr
            holder.topAppsText.visibility = View.VISIBLE
            
            // Color code based on average usage
            val color = when {
                item.averageMinutes < 120 -> android.graphics.Color.parseColor("#4CAF50") // Green
                item.averageMinutes < 240 -> android.graphics.Color.parseColor("#FF9800") // Orange
                else -> android.graphics.Color.parseColor("#F44336") // Red
            }
            holder.screenTimeText.setTextColor(color)
        }
        
        override fun getItemCount() = data.size
    }
}
