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
import com.example.touristapp.models.QuestScript
import com.example.touristapp.models.ScriptAction
import com.example.touristapp.database.DbConnection

class AttractionsFragment : Fragment() {

    private var _binding: FragmentAttractionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AttractionsAdapter
    private lateinit var db: DbConnection
    private lateinit var attractionsList: List<Attraction>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttractionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DbConnection(requireContext())
        attractionsList = db.getAllAttractions()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnDone.setOnClickListener {
            val selected = attractionsList.filter { it.isSelected }
            if (selected.isNotEmpty()) {

                // Скрипт выполняется когда пользователь достигнет последней точки
                val script = QuestScript(listOf(
                    ScriptAction("toast",  text = "Вы прошли все выбранные места!"),
                    ScriptAction("dialog", title = "Маршрут завершён",
                        text = "Отличная прогулка! Вы посетили ${selected.size} мест.")
                ))

                val bundle = Bundle().apply {
                    putSerializable("selected_attractions", ArrayList(selected))
                    putSerializable("quest_script", script)
                }
                findNavController().navigate(R.id.action_attractions_to_map, bundle)
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
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