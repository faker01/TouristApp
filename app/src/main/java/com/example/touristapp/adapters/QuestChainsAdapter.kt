package com.example.touristapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.touristapp.R
import com.example.touristapp.databinding.ItemRouteBinding
import com.example.touristapp.models.Route


class QuestChainsAdapter(
    private val chains: List<com.example.touristapp.data.QuestChainConfig>,
    private val onItemClick: (com.example.touristapp.data.QuestChainConfig) -> Unit
) : RecyclerView.Adapter<QuestChainsAdapter.QuestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quest_chain, parent, false)
        return QuestViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestViewHolder, position: Int) {
        val chain = chains[position]
        holder.bind(chain)

        if (chain.isLocked) {
            holder.itemView.isEnabled = false
            holder.itemView.alpha = 0.6f
            holder.itemView.setOnClickListener(null)
        } else {
            holder.itemView.isEnabled = true
            holder.itemView.alpha = 1f
            holder.itemView.setOnClickListener { onItemClick(chain) }
        }
    }

    override fun getItemCount(): Int = chains.size

    class QuestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_quest_name)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_quest_description)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_quest_duration)
        private val tvDistance: TextView = itemView.findViewById(R.id.tv_quest_distance)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_quest_count)
        private val ivLock: ImageView = itemView.findViewById(R.id.iv_lock)

        fun bind(chain: com.example.touristapp.data.QuestChainConfig) {
            tvName.text = chain.name
            tvDescription.text = chain.description
            tvDuration.text = chain.duration
            tvDistance.text = chain.distance
            tvCount.text = chain.count

            if (chain.isLocked) {
                ivLock.visibility = View.VISIBLE
            } else {
                ivLock.visibility = View.GONE
            }
        }
    }
}