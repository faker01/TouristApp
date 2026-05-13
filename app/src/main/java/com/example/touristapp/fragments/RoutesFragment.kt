package com.example.touristapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.touristapp.R
import com.example.touristapp.adapters.RoutesAdapter
import com.example.touristapp.data.RouteQuestData
import com.example.touristapp.databinding.FragmentRoutesBinding
import com.example.touristapp.models.Attraction
import com.example.touristapp.models.QuestScript
import com.example.touristapp.models.ScriptAction
import com.example.touristapp.database.DbConnection
import com.example.touristapp.adapters.QuestChainsAdapter

class RoutesFragment : Fragment() {

    private var _binding: FragmentRoutesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RoutesAdapter
    private lateinit var db: DbConnection

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

        db = DbConnection(requireContext())

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        updateRoutesList()
    }

    override fun onResume() {
        super.onResume()
        updateRoutesList()
    }

    fun completeQuestChain(chainId: Int) {
        completedQuestChains.add(chainId)
        updateRoutesList()
    }

    private fun updateRoutesList() {
        // ============================================================
        // КВЕСТОВЫЕ
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
        // ГОТОВЫЕ МАРШРУТЫ
        // ============================================================
        val regularRoutes = db.getAllRoutes()

        adapter = RoutesAdapter(regularRoutes) { route ->

            // Каждый готовый маршрут имеет свой скрипт финала
            val script = QuestScript(listOf(
                ScriptAction("toast",  text = "Маршрут «${route.name}» пройден!"),
                ScriptAction("dialog", title = "🎉 Финиш!",
                    text = "Вы прошли маршрут «${route.name}».\n" +
                            "Протяжённость: ${route.distance}, время: ${route.duration}.")
            ))

            val bundle = Bundle().apply {
                putSerializable("selected_attractions", ArrayList<Attraction>(route.attractions))
                putSerializable("quest_script", script)
                putBoolean("is_quest_route", false)
            }
            findNavController().navigate(R.id.action_routes_to_map, bundle)
        }

        binding.rvRegularRoutes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRegularRoutes.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}