package com.example.zamgavocafront

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DndAppsAdapter(
    private val apps: List<DndAppsManager.AppInfo>,
    private val checked: MutableSet<String>
) : RecyclerView.Adapter<DndAppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAppName: TextView = view.findViewById(R.id.tv_app_name)
        val cbBlock: CheckBox = view.findViewById(R.id.cb_block)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_dnd, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.tvAppName.text = app.appName
        holder.cbBlock.setOnCheckedChangeListener(null)
        holder.cbBlock.isChecked = app.packageName in checked
        holder.cbBlock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checked.add(app.packageName) else checked.remove(app.packageName)
        }
        holder.itemView.setOnClickListener { holder.cbBlock.toggle() }
    }

    override fun getItemCount() = apps.size
}
