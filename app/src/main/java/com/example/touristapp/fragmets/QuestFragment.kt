package com.example.touristapp.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.touristapp.R
import com.example.touristapp.data.QuestData
import com.example.touristapp.models.*

class QuestFragment : Fragment(), LocationListener {

    // UI элементы (ищем через findViewById т.к. нет ViewBinding для нового фрагмента)
    private lateinit var tvSparks: TextView
    private lateinit var tvLocationName: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvTaskText: TextView
    private lateinit var tvTaskNumber: TextView
    private lateinit var btnHint: Button
    private lateinit var layoutInput: LinearLayout
    private lateinit var etAnswer: EditText
    private lateinit var btnCheck: Button
    private lateinit var layoutChoices: LinearLayout
    private lateinit var btnPhoto: Button
    private lateinit var btnNextTask: Button
    private lateinit var btnNextLocation: Button
    private lateinit var tvFeedback: TextView

    // Состояние квеста
    private var progress = QuestProgress()
    private var currentLocationIndex = 0
    private var currentTaskIndex = 0
    private var completedTasksInLocation = mutableSetOf<Int>()

    private lateinit var locationManager: LocationManager
    private var isLocationUpdatesActive = false

    // Текущая локация и задание
    private val currentQuestLocation get() = QuestData.locations[currentLocationIndex]
    private val currentTask get() = currentQuestLocation.tasks[currentTaskIndex]

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)

        // Восстановить прогресс если передан
        @Suppress("UNCHECKED_CAST")
        progress = arguments?.getSerializable("quest_progress") as? QuestProgress ?: QuestProgress()
        currentLocationIndex = progress.currentLocationIndex

        updateUI()
        initLocationUpdates()

        btnCheck.setOnClickListener { checkAnswer() }
        btnPhoto.setOnClickListener { handlePhoto() }
        btnHint.setOnClickListener { showHint() }
        btnNextTask.setOnClickListener { nextTask() }
        btnNextLocation.setOnClickListener { nextLocation() }

        view.findViewById<android.widget.ImageButton>(R.id.btn_back_quest)?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun bindViews(view: View) {
        tvSparks = view.findViewById(R.id.tv_sparks)
        tvLocationName = view.findViewById(R.id.tv_quest_location_name)
        tvProgress = view.findViewById(R.id.tv_quest_progress)
        tvTaskText = view.findViewById(R.id.tv_task_text)
        tvTaskNumber = view.findViewById(R.id.tv_task_number)
        btnHint = view.findViewById(R.id.btn_hint)
        layoutInput = view.findViewById(R.id.layout_input)
        etAnswer = view.findViewById(R.id.et_answer)
        btnCheck = view.findViewById(R.id.btn_check)
        layoutChoices = view.findViewById(R.id.layout_choices)
        btnPhoto = view.findViewById(R.id.btn_photo)
        btnNextTask = view.findViewById(R.id.btn_next_task)
        btnNextLocation = view.findViewById(R.id.btn_next_location)
        tvFeedback = view.findViewById(R.id.tv_feedback)
    }

    private fun updateUI() {
        val loc = currentQuestLocation
        tvSparks.text = "✨ ${progress.sparks} Искр"
        tvLocationName.text = "${loc.homlinName} — ${loc.locationName}"
        tvProgress.text = "Осколков собрано: ${progress.completedLocations.size}/7"

        showTask(currentTaskIndex)
        btnNextLocation.visibility = View.GONE
        tvFeedback.visibility = View.GONE
    }

    private fun showTask(taskIndex: Int) {
        if (taskIndex >= currentQuestLocation.tasks.size) return
        val task = currentQuestLocation.tasks[taskIndex]

        tvTaskNumber.text = if (task.isBonus) "★ Бонусное задание" else "Задание ${taskIndex + 1}/5"
        tvTaskText.text = task.text
        tvFeedback.visibility = View.GONE
        btnNextTask.visibility = View.GONE

        // Подсказка — показать стоимость
        btnHint.text = "Подсказка (${task.hintCost} ✨)"
        btnHint.isEnabled = progress.sparks >= task.hintCost

        // Показать нужный тип ввода
        layoutInput.visibility = View.GONE
        layoutChoices.visibility = View.GONE
        btnPhoto.visibility = View.GONE

        when (task.type) {
            TaskType.PHOTO -> {
                btnPhoto.visibility = View.VISIBLE
            }
            TaskType.NUMBER_INPUT, TaskType.TEXT_INPUT -> {
                layoutInput.visibility = View.VISIBLE
                etAnswer.text?.clear()
                etAnswer.hint = if (task.type == TaskType.NUMBER_INPUT) "Введи число" else "Введи ответ"
            }
            TaskType.MULTIPLE_CHOICE -> {
                layoutChoices.visibility = View.VISIBLE
                buildChoiceButtons(task)
            }
        }
    }

    private fun buildChoiceButtons(task: QuestTask) {
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

    private fun checkAnswer() {
        val userAnswer = etAnswer.text.toString().trim().lowercase()
        if (userAnswer.isEmpty()) {
            Toast.makeText(requireContext(), "Введи ответ!", Toast.LENGTH_SHORT).show()
            return
        }
        verifyAnswer(userAnswer, currentTask)
    }

    private fun handlePhoto() {
        // В реальном приложении здесь открывается камера
        // Для простоты засчитываем автоматически
        verifyAnswer("photo", currentTask)
    }

    private fun verifyAnswer(userAnswer: String, task: QuestTask) {
        val correct = userAnswer.trim().lowercase() == task.correctAnswer.lowercase()

        if (correct) {
            completedTasksInLocation.add(currentTaskIndex)
            val newSparks = progress.sparks + task.sparkReward
            progress = progress.copy(sparks = newSparks)
            tvSparks.text = "✨ ${progress.sparks} Искр"

            tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            tvFeedback.text = "✅ Верно! +${task.sparkReward} Искр"
            tvFeedback.visibility = View.VISIBLE
            btnNextTask.visibility = View.VISIBLE

            // Проверить, можно ли перейти к следующей локации
            checkLocationCompletion()
        } else {
            tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            tvFeedback.text = "❌ Неверно, попробуй ещё раз!"
            tvFeedback.visibility = View.VISIBLE
        }
    }

    private fun checkLocationCompletion() {
        val minRequired = currentQuestLocation.minTasksRequired
        if (completedTasksInLocation.size >= minRequired) {
            btnNextLocation.visibility = View.VISIBLE
            btnNextLocation.text = if (currentLocationIndex < QuestData.locations.size - 1)
                "🔓 Осколок получен! Продолжить →"
            else
                "🏆 Финальная битва!"
        }
    }

    private fun nextTask() {
        // Найти следующее незавершённое задание
        val allTasks = currentQuestLocation.tasks.indices
        val nextIdx = allTasks.firstOrNull { it > currentTaskIndex && it !in completedTasksInLocation }
            ?: allTasks.firstOrNull { it !in completedTasksInLocation }

        if (nextIdx != null) {
            currentTaskIndex = nextIdx
            showTask(currentTaskIndex)
        } else {
            Toast.makeText(requireContext(), "Все задания выполнены!", Toast.LENGTH_SHORT).show()
            checkLocationCompletion()
        }
    }

    private fun nextLocation() {
        // Засчитать осколок
        val newCompleted = progress.completedLocations + currentLocationIndex
        val shardSparks = progress.sparks + currentQuestLocation.sparkRewardForShard
        progress = progress.copy(
            completedLocations = newCompleted,
            sparks = shardSparks,
            currentLocationIndex = currentLocationIndex + 1
        )

        if (currentLocationIndex >= QuestData.locations.size - 1) {
            // Все осколки собраны — финал
            navigateToFinal()
            return
        }

        currentLocationIndex++
        currentTaskIndex = 0
        completedTasksInLocation.clear()

        // Показать диалог с текстом перехода
        AlertDialog.Builder(requireContext())
            .setTitle("${currentQuestLocation.homlinName} освобождён!")
            .setMessage("Следующая локация: ${currentQuestLocation.locationName}\n\n${currentQuestLocation.arrivalText}")
            .setPositiveButton("Идти!") { _, _ -> updateUI() }
            .show()
    }

    private fun navigateToFinal() {
        val bundle = Bundle().apply {
            putSerializable("quest_progress", progress)
        }
        findNavController().navigate(R.id.action_quest_to_final, bundle)
    }

    private fun showHint() {
        val task = currentTask
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

    // ---------- Геолокация (уведомление о приближении) ----------
    private fun initLocationUpdates() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1002)
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000L, 10f, this, Looper.getMainLooper()
            )
            isLocationUpdatesActive = true
        }
    }

    override fun onLocationChanged(location: Location) {
        if (currentLocationIndex >= QuestData.locations.size) return
        val target = currentQuestLocation
        val results = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, target.lat, target.lon, results)
        // При приближении на 50 метров — показать уведомление
        if (results[0] < 50f) {
            tvLocationName.text = "📍 Вы у локации: ${target.locationName}!"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isLocationUpdatesActive && ::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
