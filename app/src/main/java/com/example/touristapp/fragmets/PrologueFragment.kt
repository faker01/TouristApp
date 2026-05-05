package com.example.touristapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.touristapp.R

class PrologueFragment : Fragment() {

    private lateinit var prologueContainer: LinearLayout
    private lateinit var tvPrologueText: TextView
    private lateinit var btnStartQuest: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_prologue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prologueContainer = view.findViewById(R.id.prologueContainer)
        tvPrologueText = view.findViewById(R.id.tvPrologueText)
        btnStartQuest = view.findViewById(R.id.btnStartQuest)

        // Применяем стиль
        tvPrologueText.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
        tvPrologueText.typeface = android.graphics.Typeface.MONOSPACE

        val prologueText = "Хранитель Янтарного Сердца…\n\n" +
                "Тьма сгущается над Калининградом.\n" +
                "Колдун украл осколки сердца,\n" +
                "и только ты можешь их вернуть.\n\n" +
                "Собери 7 осколков, освободи хомлинов\n" +
                "и спаси Янтарный край!"

        // Плавное появление через ViewPropertyAnimator (более простой способ)
        prologueContainer.alpha = 0f
        prologueContainer.animate()
            .alpha(1f)
            .setDuration(1000)
            .withEndAction {
                animateTextTyping(prologueText, 40L)
            }
            .start()
    }

    private fun animateTextTyping(fullText: String, delayPerChar: Long) {
        tvPrologueText.text = ""
        val handler = Handler(Looper.getMainLooper())
        var index = 0

        val runnable = object : Runnable {
            override fun run() {
                if (index < fullText.length) {
                    tvPrologueText.append(fullText[index].toString())
                    index++
                    handler.postDelayed(this, delayPerChar)
                } else {
                    // Показываем кнопку
                    btnStartQuest.visibility = Button.VISIBLE
                    btnStartQuest.alpha = 0f
                    btnStartQuest.animate()
                        .alpha(1f)
                        .setDuration(500)
                        .start()

                    btnStartQuest.setOnClickListener {
                        startQuest()
                    }
                }
            }
        }
        handler.post(runnable)
    }

    private fun startQuest() {

        findNavController().navigate(R.id.action_prologue_to_quest)
    }
}