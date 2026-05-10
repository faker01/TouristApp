package com.example.touristapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.touristapp.R
import com.example.touristapp.adapters.RoutesAdapter
import com.example.touristapp.data.RouteConfig
import com.example.touristapp.data.RouteQuestData
import com.example.touristapp.databinding.FragmentRoutesBinding
import com.example.touristapp.models.Attraction
import com.example.touristapp.utils.RouteProgressManager

class RoutesFragment : Fragment() {

    private var _binding: FragmentRoutesBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressManager: RouteProgressManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressManager = RouteProgressManager(requireContext())

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // ОБЫЧНЫЕ МАРШРУТЫ
        val regularRoutes = RouteQuestData.regularRoutes
        val regularAdapter = RoutesAdapter(regularRoutes) { route ->
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

        // КВЕСТОВЫЕ МАРШРУТЫ
        val allQuestRoutes = RouteQuestData.questRoutes
        val completedRoutes = progressManager.getCompletedQuestRoutes()

        Log.d("RoutesFragment", "completedRoutes: $completedRoutes")

        val questRouteConfigs = allQuestRoutes.map { questRoute ->
            // Квест разблокирован, если:
            // 1. Это первый квест (id = 0) ИЛИ
            // 2. Предыдущий квест (requiredQuestRouteId) пройден (есть в completedRoutes)
            val isUnlocked = questRoute.id == 0 ||
                    (questRoute.requiredQuestRouteId != null && questRoute.requiredQuestRouteId in completedRoutes)

            Log.d("RoutesFragment", "Квест ${questRoute.id} (${questRoute.name}): isUnlocked=$isUnlocked")

            RouteConfig(
                id = questRoute.id,
                name = questRoute.name,
                duration = questRoute.duration,
                distance = questRoute.distance,
                count = questRoute.count,
                attractions = listOf(questRoute.firstPoint),
                isLocked = !isUnlocked
            )
        }

        if (questRouteConfigs.isNotEmpty()) {
            binding.questSection.visibility = View.VISIBLE

            val questAdapter = RoutesAdapter(questRouteConfigs) { routeConfig ->
                val originalQuest = allQuestRoutes.find { it.id == routeConfig.id }
                originalQuest?.let { quest ->
                    val bundle = Bundle().apply {
                        putString("csv_file_name", quest.csvFileName)
                        putInt("quest_route_id", quest.id)
                        putBoolean("is_quest_route", true)
                        putDouble("first_point_lat", quest.firstPoint.lat)
                        putDouble("first_point_lon", quest.firstPoint.lon)
                        putString("first_point_title", quest.firstPoint.title)
                    }

                    // Пролог только для первого квеста (id = 0)
                    if (quest.id == 0) {
                        findNavController().navigate(R.id.action_routes_to_prologue, bundle)
                    } else {
                        findNavController().navigate(R.id.action_routes_to_quest_map, bundle)
                    }
                }
            }

            binding.rvQuestRoutes.layoutManager = LinearLayoutManager(requireContext())
            binding.rvQuestRoutes.adapter = questAdapter
        } else {
            binding.questSection.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}