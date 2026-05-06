package com.example.touristapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.touristapp.R
import com.example.touristapp.data.QuestData
import com.example.touristapp.models.QuestProgress

class QuestFinalFragment : Fragment() {

    private var progress = QuestProgress()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quest_final, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("UNCHECKED_CAST")
        progress = arguments?.getSerializable("quest_progress") as? QuestProgress ?: QuestProgress()

        val tvRiddle = view.findViewById<TextView>(R.id.tv_final_riddle)
        val tvSparks = view.findViewById<TextView>(R.id.tv_final_sparks)
        val etFinal = view.findViewById<EditText>(R.id.et_final_answer)
        val btnCheck = view.findViewById<Button>(R.id.btn_final_check)
        val tvResult = view.findViewById<TextView>(R.id.tv_final_result)
        val layoutCert = view.findViewById<View>(R.id.layout_certificate)
        val tvCertRole = view.findViewById<TextView>(R.id.tv_cert_role)
        val tvCertSparks = view.findViewById<TextView>(R.id.tv_cert_sparks)
        val btnShare = view.findViewById<Button>(R.id.btn_share_cert)
        val btnHome = view.findViewById<Button>(R.id.btn_home)

        tvRiddle.text = QuestData.FINAL_RIDDLE
        tvSparks.text = "✨ Накоплено Искр: ${progress.sparks}"
        layoutCert.visibility = View.GONE

        btnCheck.setOnClickListener {
            val answer = etFinal.text.toString().trim().lowercase()
            if (answer == QuestData.FINAL_ANSWER) {
                tvResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
                tvResult.text = "🏆 ЗАГОВОР РАЗРУШЕН!\nЯнтарное Сердце снова бьётся.\nСемья непобедима!\n\nТы — Великий Янтарный Детектив!"
                tvResult.visibility = View.VISIBLE
                btnCheck.isEnabled = false
                etFinal.isEnabled = false

                // Показать сертификат
                layoutCert.visibility = View.VISIBLE
                tvCertRole.text = "Роль: ${progress.role}"
                tvCertSparks.text = "Заработано Искр: ${progress.sparks}"
            } else {
                tvResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                tvResult.text = "❌ Неверно. Колдун ещё силён...\nПодумай о том, что объединяет всех хомлинов."
                tvResult.visibility = View.VISIBLE
            }
        }

        btnShare.setOnClickListener {
            val shareText = "Я прошёл квест «Хранители Янтарного Сердца» в Калининграде!\n" +
                    "Роль: ${progress.role}\n" +
                    "Искры: ${progress.sparks}\n" +
                    "#КенигсбергТур #ХомлиныКалининграда"
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            }
            startActivity(android.content.Intent.createChooser(intent, "Поделиться сертификатом"))
        }

        btnHome.setOnClickListener {
            findNavController().navigate(R.id.mainFragment)
        }
    }
}
