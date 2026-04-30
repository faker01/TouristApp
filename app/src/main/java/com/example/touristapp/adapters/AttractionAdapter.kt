package com.example.touristapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.touristapp.databinding.ItemAttractionBinding
import com.example.touristapp.models.Attraction

class AttractionsAdapter(
    private val attractions: List<Attraction>,
    private val onCheckChanged: (Attraction, Boolean) -> Unit
) : RecyclerView.Adapter<AttractionsAdapter.AttractionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttractionViewHolder {
        val binding = ItemAttractionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttractionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttractionViewHolder, position: Int) {
        holder.bind(attractions[position], onCheckChanged)
    }

    override fun getItemCount() = attractions.size

    class AttractionViewHolder(private val binding: ItemAttractionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(attraction: Attraction, onCheckChanged: (Attraction, Boolean) -> Unit) {
            binding.tvTitle.text = attraction.title
            binding.cbSelect.isChecked = attraction.isSelected
            binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                onCheckChanged(attraction, isChecked)
            }
        }
    }
}