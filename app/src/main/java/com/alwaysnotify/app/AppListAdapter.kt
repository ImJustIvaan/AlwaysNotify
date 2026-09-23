package com.alwaysnotify.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val onSelectionChanged: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private var fullList: List<AppInfo> = emptyList()
    private var visibleList: List<AppInfo> = emptyList()
    private var query: String = ""

    fun submitList(apps: List<AppInfo>) {
        fullList = apps
        applyFilter()
    }

    fun filter(text: String) {
        query = text
        applyFilter()
    }

    private fun applyFilter() {
        visibleList = if (query.isBlank()) {
            fullList
        } else {
            fullList.filter { it.label.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(visibleList[position], onSelectionChanged)
    }

    override fun getItemCount(): Int = visibleList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.appIcon)
        private val label: TextView = itemView.findViewById(R.id.appLabel)
        private val checkBox: CheckBox = itemView.findViewById(R.id.appCheckBox)

        fun bind(app: AppInfo, onSelectionChanged: (AppInfo, Boolean) -> Unit) {
            icon.setImageDrawable(app.icon)
            label.text = app.label

            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = PrefsManager.isSelected(itemView.context, app.packageName)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onSelectionChanged(app, isChecked)
            }

            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }
        }
    }
}
