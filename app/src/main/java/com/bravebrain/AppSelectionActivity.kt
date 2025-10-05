package com.bravebrain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.card.MaterialCardView

class AppSelectionActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var saveButton: MaterialButton
    private lateinit var searchEditText: TextInputEditText
    private lateinit var selectAllButton: MaterialButton
    private lateinit var deselectAllButton: MaterialButton
    private lateinit var selectionCounter: LinearLayout
    private lateinit var counterText: TextView
    private lateinit var adapter: AppListAdapter
    private lateinit var appList: List<AppInfo>
    private var filteredList: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LayoutInflater.from(this).inflate(R.layout.activity_app_selection, null)
        setContentView(root)

        // Initialize UI components
        recyclerView = root.findViewById(R.id.recyclerView)
        saveButton = root.findViewById(R.id.saveButton)
        searchEditText = root.findViewById(R.id.searchEditText)
        selectAllButton = root.findViewById(R.id.selectAllButton)
        deselectAllButton = root.findViewById(R.id.deselectAllButton)
        selectionCounter = root.findViewById(R.id.selectionCounter)
        counterText = root.findViewById(R.id.counterText)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Get data
        appList = getInstalledApps()
        filteredList = appList
        val previouslySelected = getPreviouslySelectedApps()
        
        // Set up adapter with selection listener
        adapter = AppListAdapter(filteredList, previouslySelected, object : SelectionListener {
            override fun onSelectionChanged(selectedCount: Int) {
                updateSelectionCounter(selectedCount)
            }
        })
        recyclerView.adapter = adapter
        
        // Initial update for selection counter
        updateSelectionCounter(previouslySelected.size)
        
        // Set up search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                filterApps(s.toString())
            }
        })
        
        // Setup quick action buttons
        selectAllButton.setOnClickListener {
            adapter.selectAll()
            updateSelectionCounter(adapter.getSelectedCount())
            Toast.makeText(this, "All apps selected", Toast.LENGTH_SHORT).show()
        }
        
        deselectAllButton.setOnClickListener {
            adapter.deselectAll()
            updateSelectionCounter(0)
            Toast.makeText(this, "All apps deselected", Toast.LENGTH_SHORT).show()
        }
        
        // Setup save button
        saveButton.setOnClickListener {
            val selectedApps = adapter.getSelectedApps()
            saveSelectedApps(selectedApps)
            Toast.makeText(this, "Saved ${selectedApps.size} apps to block", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun updateSelectionCounter(count: Int) {
        if (count > 0) {
            selectionCounter.visibility = View.VISIBLE
            counterText.text = "$count app${if (count == 1) "" else "s"} selected"
            // Update save button text and state
            saveButton.text = "💾 Save $count App${if (count == 1) "" else "s"}"
            saveButton.isEnabled = true
            saveButton.alpha = 1.0f
        } else {
            selectionCounter.visibility = View.GONE
            saveButton.text = "💾 Save Selection"
            saveButton.isEnabled = false
            saveButton.alpha = 0.6f
        }
    }
    
    private fun filterApps(query: String) {
        if (query.isEmpty()) {
            filteredList = appList
        } else {
            filteredList = appList.filter { 
                it.appName.contains(query, ignoreCase = true) || 
                it.packageName.contains(query, ignoreCase = true) 
            }
        }
        adapter.updateData(filteredList)
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val myPackage = packageName
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos
            .filter { it.activityInfo.packageName != myPackage }
            .map {
                val appInfo = pm.getApplicationInfo(it.activityInfo.packageName, 0)
                AppInfo(
                    it.loadLabel(pm).toString(),
                    it.activityInfo.packageName,
                    appInfo.loadIcon(pm)
                )
            }
            .sortedBy { it.appName }
    }

    private fun getPreviouslySelectedApps(): Set<String> {
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        return prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
    }

    private fun saveSelectedApps(selectedApps: List<AppInfo>) {
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putStringSet("blocked_packages", selectedApps.map { it.packageName }.toSet())
        
        // Set default time limit of 1 minute for each selected app
        val existingTimeLimits = prefs.getString("time_limits", null)
        val existingMap = mutableMapOf<String, Int>()
        existingTimeLimits?.split("|")?.forEach { entry ->
            val parts = entry.split(",")
            if (parts.size == 2) existingMap[parts[0]] = parts[1].toIntOrNull() ?: 0
        }
        
        // Add new apps with 1 minute default, keep existing limits
        selectedApps.forEach { app ->
            if (!existingMap.containsKey(app.packageName)) {
                existingMap[app.packageName] = 1 // 1 minute default
            }
        }
        
        val timeLimitsStr = existingMap.entries.joinToString("|") { "${it.key},${it.value}" }
        editor.putString("time_limits", timeLimitsStr)
        editor.apply()
    }
}

// Interface for selection change callbacks
interface SelectionListener {
    fun onSelectionChanged(selectedCount: Int)
}

data class AppInfo(val appName: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class AppListAdapter(
    private var apps: List<AppInfo>, 
    previouslySelected: Set<String>,
    private val selectionListener: SelectionListener? = null
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {
    
    private val selected = previouslySelected.toMutableSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        val isSelected = selected.contains(app.packageName)
        
        // Bind data
        holder.appName.text = app.appName
        holder.appPackage.text = app.packageName
        holder.appIcon.setImageDrawable(app.icon)
        
        // Update visual state
        updateItemVisualState(holder, isSelected)
        
        // Setup checkbox
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = isSelected
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            toggleSelection(app.packageName, isChecked)
            updateItemVisualState(holder, isChecked)
            selectionListener?.onSelectionChanged(selected.size)
        }
        
        // Setup card click
        holder.cardView.setOnClickListener {
            val newState = !holder.checkBox.isChecked
            holder.checkBox.isChecked = newState
            toggleSelection(app.packageName, newState)
            updateItemVisualState(holder, newState)
            selectionListener?.onSelectionChanged(selected.size)
        }
    }
    
    private fun updateItemVisualState(holder: ViewHolder, isSelected: Boolean) {
        if (isSelected) {
            // Selected state
            holder.cardView.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.colorSelectedBorder)
            holder.cardView.strokeWidth = 3
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.colorSelectedBackground))
            holder.selectionIndicator.visibility = View.VISIBLE
            holder.selectedBadge.visibility = View.VISIBLE
            holder.appName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorSelectedText))
        } else {
            // Unselected state
            holder.cardView.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.cardBorder)
            holder.cardView.strokeWidth = 1
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.cardBackground))
            holder.selectionIndicator.visibility = View.GONE
            holder.selectedBadge.visibility = View.GONE
            holder.appName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.textPrimary))
        }
    }
    
    private fun toggleSelection(packageName: String, isSelected: Boolean) {
        if (isSelected) {
            selected.add(packageName)
        } else {
            selected.remove(packageName)
        }
    }

    override fun getItemCount() = apps.size

    fun getSelectedApps(): List<AppInfo> = apps.filter { selected.contains(it.packageName) }
    
    fun getSelectedCount(): Int = selected.size
    
    fun selectAll() {
        selected.clear()
        apps.forEach { selected.add(it.packageName) }
        notifyDataSetChanged()
    }
    
    fun deselectAll() {
        selected.clear()
        notifyDataSetChanged()
    }
    
    fun updateData(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.cardView)
        val appName: TextView = view.findViewById(R.id.appName)
        val appPackage: TextView = view.findViewById(R.id.appPackage)
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val selectionIndicator: View = view.findViewById(R.id.selectionIndicator)
        val selectedBadge: LinearLayout = view.findViewById(R.id.selectedBadge)
    }
}
