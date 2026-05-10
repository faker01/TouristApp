package com.example.touristapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.touristapp.R
import com.example.touristapp.data.RouteConfig

class RoutesAdapter(
    private val routes: List<RouteConfig>,
    private val onItemClick: (RouteConfig) -> Unit
) : RecyclerView.Adapter<RoutesAdapter.RouteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]
        holder.bind(route)

        if (route.isLocked) {
            holder.itemView.isEnabled = false
            holder.itemView.setOnClickListener(null)
            holder.itemView.alpha = 0.6f  // делаем полупрозрачным для наглядности
        } else {
            holder.itemView.isEnabled = true
            holder.itemView.alpha = 1f
            holder.itemView.setOnClickListener { onItemClick(route) }
        }
    }

    override fun getItemCount(): Int = routes.size

    class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_route_name)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val tvDistance: TextView = itemView.findViewById(R.id.tv_distance)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_attractions_count)
        private val ivLock: ImageView? = itemView.findViewById(R.id.iv_lock)

        fun bind(route: RouteConfig) {
            tvName.text = route.name
            tvDuration.text = route.duration
            tvDistance.text = route.distance
            tvCount.text = route.count

            // Показываем или скрываем замочек
            if (route.isLocked) {
                ivLock?.visibility = View.VISIBLE
                tvName.alpha = 0.6f
            } else {
                ivLock?.visibility = View.GONE
                tvName.alpha = 1f
            }
        }
    }
}