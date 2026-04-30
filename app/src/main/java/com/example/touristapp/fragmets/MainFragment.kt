package com.example.touristapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.touristapp.R
import com.example.touristapp.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRoutes.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_routes)
        }
        binding.btnCreate.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_attractions)
        }
        binding.btnMap.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_map)
        }
        binding.btnQuest.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_quest)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}