package com.example.touristapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.touristapp.databinding.ItemRouteBinding
import com.example.touristapp.models.Route

class RoutesAdapter(
    private val routes: List<Route>,
    private val onClick: (Route) -> Unit
) : RecyclerView.Adapter<RoutesAdapter.RouteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routes[position], onClick)
    }

    override fun getItemCount() = routes.size

    class RouteViewHolder(private val binding: ItemRouteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(route: Route, onClick: (Route) -> Unit) {
            binding.tvRouteName.text = route.name
            binding.tvDuration.text = "⏱ ${route.duration}"
            binding.tvDistance.text = "📏 ${route.distance}"
            binding.tvAttractionsCount.text = "📍 ${route.count}"
            binding.root.setOnClickListener { onClick(route) }
        }
    }
}