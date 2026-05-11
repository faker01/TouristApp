package com.example.touristapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.touristapp.R
import com.example.touristapp.data.RegularRouteConfig
import com.example.touristapp.data.RouteQuestData
import com.example.touristapp.databinding.FragmentRoutesBinding
import com.example.touristapp.models.Attraction

class RoutesFragment : Fragment() {

    private var _binding: FragmentRoutesBinding? = null
    private val binding get() = _binding!!

    // ВРЕМЕННОЕ ХРАНИЛИЩЕ ПРОГРЕССА (в памяти)
    companion object {
        private val completedQuestChains = mutableSetOf<Int>()

        fun addCompletedQuestChain(chainId: Int) {
            completedQuestChains.add(chainId)
        }

        fun resetProgress() {
            completedQuestChains.clear()
        }

        fun getCompletedChains(): Set<Int> = completedQuestChains
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        updateRoutesList()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возврате из квеста
        updateRoutesList()
    }

    fun completeQuestChain(chainId: Int) {
        completedQuestChains.add(chainId)
        updateRoutesList()
    }

    private fun updateRoutesList() {
        // ============================================================
        // 1. КВЕСТОВЫЕ ЦЕПОЧКИ
        // ============================================================
        val allQuestChains = RouteQuestData.getQuestChains()

        // Определяем, какие цепочки разблокированы
        val unlockedChains = allQuestChains.map { chain ->
            val isUnlocked = chain.id == 0 || (chain.id - 1) in completedQuestChains
            chain.copy(isLocked = !isUnlocked)
        }

        if (unlockedChains.isNotEmpty()) {
            binding.questSection.visibility = View.VISIBLE

            val questAdapter = QuestChainsAdapter(unlockedChains) { chain ->
                if (!chain.isLocked) {
                    val bundle = Bundle().apply {
                        putString("csv_file_name", chain.csvFileName)
                        putInt("quest_chain_id", chain.id)
                    }
                    findNavController().navigate(R.id.action_routes_to_quest, bundle)
                }
            }

            binding.rvQuestRoutes.layoutManager = LinearLayoutManager(requireContext())
            binding.rvQuestRoutes.adapter = questAdapter
        } else {
            binding.questSection.visibility = View.GONE
        }

        // ============================================================
        // 2. ГОТОВЫЕ МАРШРУТЫ
        // ============================================================
        val regularRoutes = RouteQuestData.getRegularRoutes()

        val regularAdapter = RegularRoutesAdapter(regularRoutes) { route ->
            val attractions = route.attractions.map {
                Attraction(it.title, it.lat, it.lon)
            }

            val bundle = Bundle().apply {
                putSerializable("selected_attractions", ArrayList(attractions))
                putString("route_name", route.name)
                putBoolean("is_quest_route", false)
            }
            findNavController().navigate(R.id.action_routes_to_map, bundle)
        }

        binding.rvRegularRoutes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRegularRoutes.adapter = regularAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ============================================================
// АДАПТЕР ДЛЯ КВЕСТОВЫХ ЦЕПОЧЕК
// ============================================================

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

// ============================================================
// АДАПТЕР ДЛЯ ГОТОВЫХ МАРШРУТОВ
// ============================================================

class RegularRoutesAdapter(
    private val routes: List<RegularRouteConfig>,
    private val onItemClick: (RegularRouteConfig) -> Unit
) : RecyclerView.Adapter<RegularRoutesAdapter.RegularViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegularViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_regular_route, parent, false)
        return RegularViewHolder(view)
    }

    override fun onBindViewHolder(holder: RegularViewHolder, position: Int) {
        val route = routes[position]
        holder.bind(route)
        holder.itemView.setOnClickListener { onItemClick(route) }
    }

    override fun getItemCount(): Int = routes.size

    class RegularViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_route_name)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val tvDistance: TextView = itemView.findViewById(R.id.tv_distance)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_attractions_count)

        fun bind(route: RegularRouteConfig) {
            tvName.text = route.name
            tvDuration.text = route.duration
            tvDistance.text = route.distance
            tvCount.text = route.count
        }
    }
}