package com.example.touristapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    // UI элементы
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

    // Данные квеста
    private var progress = QuestProgress()
    private lateinit var csvQuestData: LoadedQuestData
    private var currentElementId: String? = null
    private var currentTaskTrigger: Trigger? = null

    // Аргументы
    private var questChainId: Int = 0
    private var csvFileName: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)

        csvFileName = arguments?.getString("csv_file_name") ?: "quest_data.csv"
        questChainId = arguments?.getInt("quest_chain_id", 0) ?: 0

        progress = arguments?.getSerializable("quest_progress") as? QuestProgress ?: QuestProgress()
        tvSparks.text = "✨ ${progress.sparks} Искр"

        val loader = QuestDataLoader(requireContext(), csvFileName)
        csvQuestData = loader.loadQuest()
        currentElementId = csvQuestData.startElementId

        if (currentElementId == null) {
            Toast.makeText(requireContext(), "Ошибка загрузки квеста", Toast.LENGTH_SHORT).show()
            finishToRoutes()
            return
        }

        showCurrentElement()

        view.findViewById<ImageButton>(R.id.btn_back_quest)?.setOnClickListener {
            finishToRoutes()
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
            is IntroModel -> showIntro(element)
            is DialogueModel -> showDialogue(element)
            is TaskModel -> showTask(element)
            is ResultModel -> showResult(element)
        }
    }

    // ============================================================
    // INTRO (ВСТУПЛЕНИЕ)
    // ============================================================

    private fun showIntro(intro: IntroModel) {
        hideAllInputs()

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_intro, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_intro_title)
        val tvText = dialogView.findViewById<TextView>(R.id.tv_intro_text)
        val btnNext = dialogView.findViewById<Button>(R.id.btn_intro_next)

        tvTitle.text = intro.title
        tvText.text = ""

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()

        animateTextTyping(tvText, intro.text, 40L) {
            btnNext.visibility = View.VISIBLE
            btnNext.setOnClickListener {
                dialog.dismiss()
                currentElementId = intro.nextElementId
                showCurrentElement()
            }
        }
    }

    // функция написания текста постепенно
    private fun animateTextTyping(textView: TextView, fullText: String, delayPerChar: Long, onComplete: () -> Unit) {
        textView.text = ""
        val handler = Handler(Looper.getMainLooper())
        var index = 0

        val runnable = object : Runnable {
            override fun run() {
                if (index < fullText.length) {
                    textView.append(fullText[index].toString())
                    index++
                    handler.postDelayed(this, delayPerChar)
                } else {
                    onComplete()
                }
            }
        }
        handler.post(runnable)
    }

    // ============================================================
    // ДИАЛОГИ
    // ============================================================

    // отобразить диалог (модель диалога) ПЕРЕДЕЛАТЬ НАПИСАНИЕ НА СТРАНИЦЕ
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

    // ============================================================
    // ЗАДАНИЯ
    // ============================================================

    // отобразить задание (Таск)
    private fun showTask(task: TaskModel) {
        hideAllInputs()
        btnNextTask.visibility = View.GONE
        btnNextTask.setOnClickListener(null)
        tvFeedback.visibility = View.GONE
        btnCheck.isEnabled = true

        tvTaskNumber.text = if (task.isBonus) "★ Бонусное задание" else "Задание"
        tvTaskText.text = task.text
        currentTaskTrigger = task.trigger

        btnHint.text = "Подсказка (${task.hintCost} ✨)"
        btnHint.isEnabled = progress.sparks >= task.hintCost
        btnHint.setOnClickListener { showHint(task) }

        when (task.taskType) {
            TaskType.PHOTO -> {
                btnPhoto.visibility = View.VISIBLE
                btnPhoto.setOnClickListener {
                    verifyAnswerAndProceed("photo", task)
                }
            }
            TaskType.NUMBER_INPUT, TaskType.TEXT_INPUT -> {
                layoutInput.visibility = View.VISIBLE
                etAnswer.text?.clear()
                etAnswer.hint = if (task.taskType == TaskType.NUMBER_INPUT) "Введи число" else "Введи ответ"
                btnCheck.setOnClickListener {
                    val answer = etAnswer.text.toString().trim().lowercase()
                    if (answer.isNotEmpty()) {
                        verifyAnswerAndProceed(answer, task)
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
                verifyAnswerAndProceed(option.lowercase(), task)
            }
            layoutChoices.addView(btn)
        }
    }

    /**
     * Проверка ответа и переход к следующему элементу
     * С учётом триггера (нужно ли открывать карту)
     */
    private fun verifyAnswerAndProceed(userAnswer: String, task: TaskModel) {
        if (userAnswer == task.correctAnswer.lowercase()) {
            addSparks(task.sparkReward)

            tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            tvFeedback.text = "✅ Верно! +${task.sparkReward} Искр"
            tvFeedback.visibility = View.VISIBLE

            // Получаем следующий элемент
            val nextElement = csvQuestData.getNextElement(task.id)
            val nextTrigger = when (nextElement) {
                is TaskModel -> nextElement.trigger
                is DialogueModel -> nextElement.trigger
                else -> null
            }


            // ============================================================
            // ЗАГЛУШКА КАРТЫ
            // Проверяем, нужно ли показывать карту (разные триггеры)
            // ============================================================
            val ShowMap = ShowMapForNext(task.trigger, nextTrigger)

            if (ShowMap) {
                val bundle = Bundle().apply {
                    putSerializable("selected_attractions", ArrayList(selected))
                    putSerializable("quest_script", showMapPlaceholder(nextElement))
                }

                findNavController().navigate(R.id.action_quest_to_map, bundle)

                showMapPlaceholder(nextElement)
            } else {
                // Сразу показываем кнопку "Далее"
                btnNextTask.visibility = View.VISIBLE
                btnNextTask.text = "Далее →"

                btnNextTask.setOnClickListener {
                    btnNextTask.visibility = View.GONE
                    tvFeedback.visibility = View.GONE

                    if (nextElement != null) {
                        currentElementId = nextElement.id
                        showCurrentElement()
                    } else {
                        finishQuest()
                    }
                }
            }

            // Отключаем кнопки ввода
            btnCheck.isEnabled = false
            layoutInput.visibility = View.GONE
            layoutChoices.visibility = View.GONE
            btnPhoto.visibility = View.GONE

        } else {
            tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            tvFeedback.text = "❌ Неверно, попробуй ещё раз!"
            tvFeedback.visibility = View.VISIBLE
        }
    }

    /**
     * Определяет, нужно ли показывать карту при переходе к следующему заданию
     * @return true если координаты триггеров разные
     */
    private fun ShowMapForNext(currentTrigger: Trigger?, nextTrigger: Trigger?): Boolean {
        // Если нет следующего триггера или нет текущего — карта не нужна
        if (currentTrigger == null || nextTrigger == null) return false

        // Если нет гео-координат — карта не нужна
        if (!currentTrigger.isLocationTrigger() || !nextTrigger.isLocationTrigger()) return false

        // Сравниваем координаты
        val lat1 = currentTrigger.latitude ?: return false
        val lon1 = currentTrigger.longitude ?: return false
        val lat2 = nextTrigger.latitude ?: return false
        val lon2 = nextTrigger.longitude ?: return false

        // Если координаты совпадают (в пределах радиуса), карта не нужна
        val distance = calculateDistance(lat1, lon1, lat2, lon2)
        val radiusSum = (currentTrigger.radius ?: 50f) + (nextTrigger.radius ?: 50f)

        return distance > radiusSum
    }

    /**
     * Расчёт расстояния между двумя точками в метрах
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371000f
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (R * c).toFloat()
    }

    /**
     * ЗАГЛУШКА КАРТЫ
     * Временное решение, пока карта не интегрирована
     * TODO: ЗАМЕНИТЬ НА РЕАЛЬНЫЙ ПЕРЕХОД К КАРТЕ С ПОСТРОЕНИЕМ МАРШРУТА
     *
     * Когда карта будет готова, здесь нужно:
     * 1. Открыть MapFragment с передачей координат следующей точки
     * 2. Построить маршрут от текущего местоположения до точки
     * 3. После достижения точки или нажатия кнопки "Прибыл" вернуться к заданиям
     */
    private fun showMapPlaceholder(nextElement: BaseQuestModel?) {
        // Получаем координаты из триггера следующего элемента
        val nextTrigger = when (nextElement) {
            is TaskModel -> nextElement.trigger
            is DialogueModel -> nextElement.trigger
            else -> null
        }

        val pointTitle = when (nextElement) {
            is TaskModel -> "🎯 Следующая точка: ${nextElement.text.take(30)}"
            is DialogueModel -> "🎯 Следующая точка: ${nextElement.text.take(30)}"
            else -> "🎯 Следующая точка маршрута"
        }

        val coordinates = if (nextTrigger != null && nextTrigger.isLocationTrigger()) {
            "📍 Координаты: ${nextTrigger.latitude}, ${nextTrigger.longitude}\n📏 Радиус: ${nextTrigger.radius}м"
        } else {
            "📍 Координаты не указаны"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("🗺️ ПЕРЕМЕЩЕНИЕ")
            .setMessage("$pointTitle\n\n$coordinates\n\nВам нужно переместиться к следующей точке на карте.\n\n(Здесь будет открытие карты с построением маршрута)\n\n---\n⚠️ ВРЕМЕННАЯ ЗАГЛУШКА: вместо карты сразу переходим к следующему заданию")
            .setPositiveButton("Понятно, продолжаю") { _, _ ->
                tvFeedback.visibility = View.GONE
                if (nextElement != null) {
                    currentElementId = nextElement.id
                    showCurrentElement()
                } else {
                    finishQuest()
                }
            }
            .setCancelable(false)
            .show()
    }

    // ============================================================
    // РЕЗУЛЬТАТЫ И ЗАВЕРШЕНИЕ
    // ============================================================

    private fun showResult(result: ResultModel) {
        hideAllInputs()
        addSparks(result.sparksReward)

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("🏆 Результат")
            .setMessage("${result.message}\n\n+${result.sparksReward} искр")
            .setCancelable(false)

        if (result.isFinal) {
            // Помечаем квестовую цепочку как пройденную
            RoutesFragment.addCompletedQuestChain(questChainId)

            builder.setPositiveButton("К маршрутам") { _, _ ->
                findNavController().popBackStack(R.id.routesFragment, false)
            }
        } else {
            builder.setPositiveButton("Продолжить") { _, _ ->
                finishFragment()
            }
        }
        builder.show()
    }

    private fun finishFragment() {
        findNavController().popBackStack()
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

    private fun finishToRoutes() {
        findNavController().popBackStack(R.id.routesFragment, false)
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