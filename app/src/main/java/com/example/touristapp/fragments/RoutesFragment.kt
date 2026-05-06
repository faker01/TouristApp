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
import com.example.touristapp.databinding.FragmentRoutesBinding
import com.example.touristapp.models.Route

class RoutesFragment : Fragment() {

    private var _binding: FragmentRoutesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RoutesAdapter

    private val sampleRoutes = listOf(
        Route("История", "2-3 ч", "4.5 км", "8 мест", listOf("Собор", "Рыбная деревня", "Королевские ворота")),
        Route("Форты", "4-5 ч", "8.2 км", "6 мест", listOf("Форт №5", "Форт №3")),
        Route("Музеи", "3-4 ч", "3.8 км", "5 мест", listOf("Музей океана", "Исторический музей"))
    )

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

        adapter = RoutesAdapter(sampleRoutes) { route ->
            val bundle = Bundle().apply {
                putString("route_name", route.name)
                putString("route_duration", route.duration)
                putString("route_distance", route.distance)
                putString("route_count", route.count)
                putStringArrayList("route_attractions", ArrayList(route.attractions))
            }
            findNavController().navigate(R.id.action_routes_to_map, bundle)
        }

        binding.rvRoutes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoutes.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}