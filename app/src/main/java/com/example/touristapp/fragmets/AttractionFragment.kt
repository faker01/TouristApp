package com.example.touristapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.touristapp.R
import com.example.touristapp.adapters.AttractionsAdapter
import com.example.touristapp.databinding.FragmentAttractionsBinding
import com.example.touristapp.models.Attraction

class AttractionsFragment : Fragment() {

    private var _binding: FragmentAttractionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AttractionsAdapter

    // Список мест с реальными координатами (пример для Калининграда)
    private val attractionsList = listOf(
        Attraction("Собор", 54.7065, 20.5090),
        Attraction("Музей океана", 54.7044, 20.4994),
        Attraction("Рыбная деревня", 54.7030, 20.5095),
        Attraction("Форт №5", 54.7240, 20.4550),
        Attraction("Королевские ворота", 54.7210, 20.5155)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttractionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnDone.setOnClickListener {
            val selected = attractionsList.filter { it.isSelected }
            if (selected.isNotEmpty()) {
                val bundle = Bundle().apply {
                    putSerializable("selected_attractions", ArrayList(selected))
                }
                findNavController().navigate(R.id.action_attractions_to_map, bundle)
            }
        }

        adapter = AttractionsAdapter(attractionsList) { attraction, isChecked ->
            attraction.isSelected = isChecked
        }

        binding.rvAttractions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttractions.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}