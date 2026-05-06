package com.example.touristapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.touristapp.R
import com.example.touristapp.models.*
import com.example.touristapp.utils.csv.QuestDataLoader

class QuestFragment : Fragment() {

    private lateinit var tvSparks: TextView
    private lateinit var tvTaskText: TextView
    private lateinit var tvTaskNumber: TextView
    private lateinit var btnHint: Button
    private lateinit var layoutInput: LinearLayout
    private lateinit var etAnswer: EditText
    private lateinit var btnCheck: Button
    private lateinit var layoutChoices: LinearLayout
    private lateinit var btnPhoto: Button
    private lateinit var btnNextTask: Button
    private lateinit var tvFeedback: TextView

    private var progress = QuestProgress()
    private lateinit var csvQuestData: LoadedQuestData
    private var currentElementId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)

        progress = arguments?.getSerializable("quest_progress") as? QuestProgress ?: QuestProgress()
        tvSparks.text = "✨ ${progress.sparks} Искр"

        // Загружаем CSV
        val loader = QuestDataLoader(requireContext())
        csvQuestData = loader.loadQuest()
        currentElementId = csvQuestData.startDialogueId

        if (currentElementId == null) {
            Toast.makeText(requireContext(), "Ошибка загрузки квеста", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        showCurrentElement()

        view.findViewById<ImageButton>(R.id.btn_back_quest)?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun bindViews(view: View) {
        tvSparks = view.findViewById(R.id.tv_sparks)
        tvTaskText = view.findViewById(R.id.tv_task_text)
        tvTaskNumber = view.findViewById(R.id.tv_task_number)
        btnHint = view.findViewById(R.id.btn_hint)
        layoutInput = view.findViewById(R.id.layout_input)
        etAnswer = view.findViewById(R.id.et_answer)
        btnCheck = view.findViewById(R.id.btn_check)
        layoutChoices = view.findViewById(R.id.layout_choices)
        btnPhoto = view.findViewById(R.id.btn_photo)
        btnNextTask = view.findViewById(R.id.btn_next_task)
        tvFeedback = view.findViewById(R.id.tv_feedback)
    }

    private fun showCurrentElement() {
        val element = csvQuestData.getElementById(currentElementId ?: return) ?: run {
            finishQuest()
            return
        }

        when (element) {
            is DialogueModel -> showDialogue(element)
            is TaskModel -> showTask(element)
            is ResultModel -> showResult(element)
        }
    }

    private fun showDialogue(dialogue: DialogueModel) {
        hideAllInputs()
        if (dialogue.sparksReward > 0) {
            addSparks(dialogue.sparksReward)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(dialogue.characterName)
            .setMessage(dialogue.text)
            .setPositiveButton("Далее") { _, _ ->
                currentElementId = dialogue.nextElementId
                showCurrentElement()
            }
            .setCancelable(false)
            .show()
    }

    private fun showTask(task: TaskModel) {
        hideAllInputs()
        btnNextTask.visibility = View.GONE  // <-- Явно скрываем
        btnNextTask.setOnClickListener(null) // <-- Очищаем слушатель
        tvFeedback.visibility = View.GONE
        btnCheck.isEnabled = true

        tvTaskNumber.text = if (task.isBonus) "★ Бонусное задание" else "Задание"
        tvTaskText.text = task.text

        btnHint.text = "Подсказка (${task.hintCost} ✨)"
        btnHint.isEnabled = progress.sparks >= task.hintCost
        btnHint.setOnClickListener { showHint(task) }

        when (task.taskType) {
            TaskType.PHOTO -> {
                btnPhoto.visibility = View.VISIBLE
                btnPhoto.setOnClickListener {
                    verifyAnswer("photo", task)
                }
            }
            TaskType.NUMBER_INPUT, TaskType.TEXT_INPUT -> {
                layoutInput.visibility = View.VISIBLE
                etAnswer.text?.clear()
                etAnswer.hint = if (task.taskType == TaskType.NUMBER_INPUT) "Введи число" else "Введи ответ"
                btnCheck.setOnClickListener {
                    val answer = etAnswer.text.toString().trim().lowercase()
                    if (answer.isNotEmpty()) {
                        verifyAnswer(answer, task)
                    } else {
                        Toast.makeText(requireContext(), "Введи ответ!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            TaskType.MULTIPLE_CHOICE -> {
                layoutChoices.visibility = View.VISIBLE
                buildChoiceButtons(task)
            }
        }
    }

    private fun buildChoiceButtons(task: TaskModel) {
        layoutChoices.removeAllViews()
        task.options.forEach { option ->
            val btn = Button(requireContext())
            btn.text = option
            btn.setOnClickListener {
                verifyAnswer(option.lowercase(), task)
            }
            layoutChoices.addView(btn)
        }
    }

    private fun verifyAnswer(userAnswer: String, task: TaskModel) {
        if (userAnswer == task.correctAnswer.lowercase()) {
            addSparks(task.sparkReward)
            tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            tvFeedback.text = "✅ Верно! +${task.sparkReward} Искр"
            tvFeedback.visibility = View.VISIBLE

            // Сохраняем следующий элемент
            val nextId = task.nextElementId
            currentElementId = nextId

            // Показываем кнопку "Далее"
            btnNextTask.visibility = View.VISIBLE
            btnNextTask.text = "Далее →"

            // Отключаем кнопки ввода
            btnCheck.isEnabled = false
            layoutInput.visibility = View.GONE
            layoutChoices.visibility = View.GONE
            btnPhoto.visibility = View.GONE

            // Устанавливаем обработчик для кнопки "Далее"
            btnNextTask.setOnClickListener {
                btnNextTask.visibility = View.GONE
                tvFeedback.visibility = View.GONE
                btnCheck.isEnabled = true

                if (nextId != null) {
                    showCurrentElement()
                } else {
                    finishQuest()
                }
            }
        } else {
            tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            tvFeedback.text = "❌ Неверно, попробуй ещё раз!"
            tvFeedback.visibility = View.VISIBLE
        }
    }

    private fun showResult(result: ResultModel) {
        hideAllInputs()
        addSparks(result.sparksReward)

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("🏆 Результат")
            .setMessage("${result.message}\n\n+${result.sparksReward} искр")
            .setCancelable(false)

        if (result.isFinal) {
            builder.setPositiveButton("В главное меню") { _, _ ->
                findNavController().popBackStack()
            }
        } else {
            builder.setPositiveButton("Продолжить") { _, _ ->
                currentElementId = null // нет следующего
                finishQuest()
            }
        }
        builder.show()
    }

    private fun showHint(task: TaskModel) {
        if (progress.sparks < task.hintCost) {
            Toast.makeText(requireContext(), "Недостаточно Искр!", Toast.LENGTH_SHORT).show()
            return
        }
        progress = progress.copy(sparks = progress.sparks - task.hintCost)
        tvSparks.text = "✨ ${progress.sparks} Искр"

        AlertDialog.Builder(requireContext())
            .setTitle("Подсказка (-${task.hintCost} ✨)")
            .setMessage(task.hint)
            .setPositiveButton("Понял!") { d, _ -> d.dismiss() }
            .show()
    }

    private fun finishQuest() {
        AlertDialog.Builder(requireContext())
            .setTitle("Квест завершён")
            .setMessage("Ты заработал ${progress.sparks} Искр!")
            .setPositiveButton("В главное меню") { _, _ ->
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun addSparks(amount: Int) {
        if (amount == 0) return
        progress = progress.copy(sparks = progress.sparks + amount)
        tvSparks.text = "✨ ${progress.sparks} Искр"
    }

    private fun hideAllInputs() {
        layoutInput.visibility = View.GONE
        layoutChoices.visibility = View.GONE
        btnPhoto.visibility = View.GONE
        btnNextTask.visibility = View.GONE
    }
}