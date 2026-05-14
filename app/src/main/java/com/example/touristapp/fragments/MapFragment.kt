package com.example.touristapp.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.touristapp.databinding.FragmentMapBinding
import com.example.touristapp.models.Attraction
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import android.location.LocationListener
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.touristapp.R
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Job
import com.example.touristapp.AppState
import com.example.touristapp.database.DbConnection
import com.example.touristapp.models.QuestScript


class MapFragment: Fragment(), LocationListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationManager: LocationManager
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            Log.d("MapFragment", "Разрешение получено")
            startLocationUpdates()
            if (_binding != null) {
                setUserLocation(binding.mapView.mapWindow.map)
            }
        } else {
            Log.w("MapFragment", "Разрешение отклонено пользователем")
            Toast.makeText(
                requireContext(),
                "Геолокация недоступна — разрешение не выдано",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    private lateinit var adminUserLocation: Array<Double>
    private var DevMode: Boolean = false
    private val tapListeners = mutableListOf<MapObjectTapListener>()

    private var userPlacemark: PlacemarkMapObject? = null
    private var attractionsCollection: MapObjectCollection? = null
    private lateinit var db: DbConnection

    // Скрипт квеста, полученный из предыдущего фрагмента
    private var questScript: QuestScript? = null

    private lateinit var drivingRouter: com.yandex.mapkit.directions.driving.DrivingRouter
    private var drivingSession: DrivingSession? = null
    private val drivingRouteListener = object : DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
            if (drivingRoutes.isEmpty()) return

            val route = drivingRoutes[0]
            // Удаляем старый маршрут и рисуем новый
            currentPolyline?.let { binding.mapView.mapWindow.map.mapObjects.remove(it) }
            currentPolyline = binding.mapView.mapWindow.map.mapObjects.addPolyline(route.geometry).apply {
                setStrokeColor(0xFF3390FF.toInt())
            }
        }

        override fun onDrivingRoutesError(error: com.yandex.runtime.Error) {
            Toast.makeText(requireContext(), "Ошибка построения маршрута", Toast.LENGTH_SHORT).show()
            Log.e("MapFragment::DrivingRoute", error.toString())
        }
    }
    private var currentPolyline: PolylineMapObject? = null


    private var routeBuildingJob: Job? = null
    private var isLocationUpdatesActive = false

    // Список выбранных точек маршрута — нужен для перестройки при движении
    private var selectedAttractions: List<Attraction>? = null
    // Следование за пользователем активно когда есть маршрут
    private var isFollowing = false

    // Триггер приближения: радиус в метрах и множество уже посещённых id
    private val PROXIMITY_RADIUS = 50f
    private val visitedAttractionIds = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DbConnection(requireContext())

        initLocationUpdates()

        binding.mapView.onStart()
        MapKitFactory.getInstance().onStart()

        val yandexMap = binding.mapView.mapWindow.map
        yandexMap.isIndoorEnabled = false
        yandexMap.poiLimit = 0


        attractionsCollection = yandexMap.mapObjects.addCollection()

        DevMode = AppState.isDev
        if (DevMode)
        {
            adminUserLocation = getUserLocation() ?: arrayOf(54.710325, 20.510053)
        }

        setUserLocation(yandexMap)

        val selected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("selected_attractions", ArrayList::class.java)
                ?.filterIsInstance<Attraction>()
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("selected_attractions") as? ArrayList<Attraction>
        }

        questScript = arguments?.getSerializable("quest_script") as? QuestScript

        if (selected != null) {
            // пришли из AttractionsFragment — показываем выбранные места
            selectedAttractions = selected
            visitedAttractionIds.clear()
            isFollowing = true
            getAttractions(selected)
            drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)
            getRoute( getUserLocation(), selected)
        } else {
            // пришли просто с кнопки "Карта" — показываем все
            getAttractions()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnZoomIn.setOnClickListener {
            val cam = yandexMap.cameraPosition
            yandexMap.move(CameraPosition(cam.target, cam.zoom + 1, 0.0f, 0.0f))
        }

        binding.btnZoomOut.setOnClickListener {
            val cam = yandexMap.cameraPosition
            yandexMap.move(CameraPosition(cam.target, cam.zoom - 1, 0.0f, 0.0f))
        }

        binding.btnToUserLocation.setOnClickListener {
            initLocationUpdates()
            setUserLocation(yandexMap)
            val cam = yandexMap.cameraPosition
            yandexMap.move(CameraPosition(userPlacemark?.geometry ?: cam.target, cam.zoom, 0.0f, 0.0f))
        }

        // Показываем крестовину только в DevMode
        if (DevMode) {
            binding.layoutAdminControls.visibility = View.VISIBLE

            binding.btnMoveRight.setOnClickListener {
                // move=1: lon+
                val newPos = setAdminUserLocation(1) ?: return@setOnClickListener
                yandexMap.move(CameraPosition(Point(newPos[0], newPos[1]), yandexMap.cameraPosition.zoom, 0.0f, 0.0f))

                userPlacemark?.geometry = Point(newPos[0], newPos[1])
                updateLocation(newPos[0], newPos[1])
            }

            binding.btnMoveUp.setOnClickListener {
                // move=2: lat+
                val newPos = setAdminUserLocation(2) ?: return@setOnClickListener
                yandexMap.move(CameraPosition(Point(newPos[0], newPos[1]), yandexMap.cameraPosition.zoom, 0.0f, 0.0f))

                userPlacemark?.geometry = Point(newPos[0], newPos[1])
                updateLocation(newPos[0], newPos[1])
            }

            binding.btnMoveLeft.setOnClickListener {
                // move=3: lon-
                val newPos = setAdminUserLocation(3) ?: return@setOnClickListener
                yandexMap.move(CameraPosition(Point(newPos[0], newPos[1]), yandexMap.cameraPosition.zoom, 0.0f, 0.0f))

                userPlacemark?.geometry = Point(newPos[0], newPos[1])
                updateLocation(newPos[0], newPos[1])
            }

            binding.btnMoveDown.setOnClickListener {
                // move=4: lat-
                val newPos = setAdminUserLocation(4) ?: return@setOnClickListener
                yandexMap.move(CameraPosition(Point(newPos[0], newPos[1]), yandexMap.cameraPosition.zoom, 0.0f, 0.0f))

                userPlacemark?.geometry = Point(newPos[0], newPos[1])
                updateLocation(newPos[0], newPos[1])
            }
        }
    }
    // пользовательская локация
    private fun initLocationUpdates() {
        locationManager = requireContext()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Разрешение уже есть — сразу запускаем
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                // Геолокация выключена — предлагаем включить
                AlertDialog.Builder(requireContext())
                    .setTitle("Геолокация отключена")
                    .setMessage("Для полноценной работы приложения необходимо включить геолокацию. Открыть настройки?")
                    .setPositiveButton("Настройки") { _, _ ->
                        // Открываем системные настройки геолокации
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
            Log.d("MapFragment", "Разрешение уже есть, запускаем геолокацию")
            startLocationUpdates()
        } else {
            // Запрашиваем оба разрешения через launcher
            Log.d("MapFragment", "Запрашиваем разрешение на геолокацию")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startLocationUpdates() {
        when {ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED -> {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                10f,
                this,
                Looper.getMainLooper()
            )
            isLocationUpdatesActive = true
                }
            else -> {
                Toast.makeText(context, "Location not enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setAdminUserLocation(move: Int): Array<Double>?{
        if (move == 1){
            adminUserLocation[1] += 0.0001
            return adminUserLocation
        }
        if (move == 2){
            adminUserLocation[0] += 0.0001
            return adminUserLocation
        }
        if (move == 3){
            adminUserLocation[1] -= 0.0001
            return adminUserLocation
        }
        if (move == 4){
            adminUserLocation[0] -= 0.0001
            return adminUserLocation
        }
        return null
    }

    private fun setUserLocation(yandexMap: Map){

        var currentUserPosition = getUserLocation()

        if (currentUserPosition == null)
        {
            currentUserPosition = arrayOf(54.710325, 20.510053)
        }

        yandexMap.move(CameraPosition(Point(currentUserPosition[0], currentUserPosition[1]), 12.0f, 0.0f, 0.0f))

        userPlacemark = yandexMap.mapObjects.addPlacemark().apply {
            geometry = Point(currentUserPosition[0], currentUserPosition[1])
            setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_user_marker),
                IconStyle().apply{
                    scale = 0.05f
                })
        }

    }

    private fun getUserLocation(): Array<Double>? {
        if (DevMode && ::adminUserLocation.isInitialized && adminUserLocation.isNotEmpty())
        {
            return adminUserLocation
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        Log.d("MapFragment", "Позиция из $provider получена")
                        return arrayOf(location.latitude, location.longitude)
                    }
                }
            } catch (e: Exception) {
                Log.e("MapFragment", "Ошибка получения позиции из $provider: ${e.message}")
            }
        }

        return null
    }

    override fun onLocationChanged(location: Location) {
        if (DevMode) return

        updateLocation(location.latitude, location.longitude)
    }

    private fun updateLocation(lat: Double, lon: Double){

        val point = Point(lat, lon)

        userPlacemark?.geometry = point

        // Проверяем приближение к точкам маршрута
        selectedAttractions?.let { checkProximity(lat, lon, it) }

        // Следование камеры за пользователем
        if (isFollowing) {
            binding.mapView.mapWindow.map.move(
                CameraPosition(point, binding.mapView.mapWindow.map.cameraPosition.zoom, 0.0f, 0.0f)
            )
            // Перестраиваем маршрут от новой позиции
            selectedAttractions?.let { attractions ->
                if (::drivingRouter.isInitialized) {
                    getRoute(
                        arrayOf(lat, lon),
                        attractions
                    )
                }
            }
        }
    }

    private fun checkProximity(userLat: Double, userLon: Double, attractions: List<Attraction>) {
        val userLocation = Location("").apply {
            latitude  = userLat
            longitude = userLon
        }

        attractions.forEach { attraction ->

            val attractionLocation = Location("").apply {
                latitude  = attraction.lat
                longitude = attraction.lon
            }

            val distance = userLocation.distanceTo(attractionLocation)

            if (distance <= PROXIMITY_RADIUS) {
                onAttractionReached(attraction)
            }
        }
    }

    private fun onAttractionReached(attraction: Attraction) {
        selectedAttractions = selectedAttractions?.minus(attraction)?.takeIf { it.isNotEmpty() }

        val attractionInfo = AlertDialog.Builder(context)
        attractionInfo.setTitle(attraction.title)
        attractionInfo.setMessage(attraction.description)
        attractionInfo.setPositiveButton("Далее") {dialog, which ->
            dialog.dismiss()
        }
        attractionInfo.create().show()

        if (selectedAttractions.isNullOrEmpty()) {
            executeScript(questScript)
        }
    }

    private fun executeScript(script: QuestScript?) {
        if (script == null) {
            Toast.makeText(requireContext(), "🎉 Маршрут завершён!", Toast.LENGTH_LONG).show()
            return
        }

        script.actions.forEach { action ->
            when (action.type) {
                "toast" -> {
                    Toast.makeText(requireContext(), action.text, Toast.LENGTH_LONG).show()
                }
                "dialog" -> {
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle(action.title)
                        .setMessage(action.text)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    // локация маршрута
    private fun getAttractions(selected: List<Attraction>? = null) {
        val imageProvider = ImageProvider.fromResource(requireContext(), R.drawable.ic_marker)

        val attractions: List<Attraction> = selected ?: db.getAllAttractions()

        attractions.forEach { attraction ->
            attractionsCollection!!.addPlacemark().apply {
                geometry = Point(attraction.lat, attraction.lon)
                setIcon(
                    imageProvider,
                    IconStyle().apply {
                        scale = 0.05f
                    })
                val listener = MapObjectTapListener { _, _ ->
                    showAttractionInfo(attraction)
                    true
                }
                tapListeners.add(listener)
                addTapListener(listener)
                }
            }
        }

    private fun getRoute(userPointCoordinates: Array<Double>?, selected: List<Attraction>?) {
        drivingSession?.cancel()

        val drivingOptions = DrivingOptions().apply {
            routesCount = 1
        }
        val vehicleOptions = VehicleOptions()

        if (selectedAttractions.isNullOrEmpty()) return

        val points = buildList { if (userPointCoordinates != null) add(
            RequestPoint(
                Point(userPointCoordinates[0], userPointCoordinates[1]),
                RequestPointType.WAYPOINT,
                null,
                null,
                null
            )
        )
            selected!!.forEach { point ->
                add(
                    RequestPoint(
                        Point(point.lat, point.lon),
                        RequestPointType.WAYPOINT,
                        null,
                        null,
                        null
                    )
                )
            } }

        drivingSession = drivingRouter.requestRoutes(
            points,
            drivingOptions,
            vehicleOptions,
            drivingRouteListener
        )
    }

    private fun showAttractionInfo(attraction: Attraction) {
        if (selectedAttractions?.isNotEmpty() ?: false){
            Toast.makeText(context, attraction.title, Toast.LENGTH_SHORT).show()
            return
        }

        val attractionInfo = AlertDialog.Builder(context)
        attractionInfo.setTitle(attraction.title)
        attractionInfo.setMessage(attraction.description)

        attractionInfo.setPositiveButton("Проложить маршрут") {dialog, which ->
            visitedAttractionIds.clear()
            isFollowing = true
            selectedAttractions = listOf(attraction)
            if (!::drivingRouter.isInitialized) drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)
            getRoute(arrayOf(userPlacemark!!.geometry.latitude, userPlacemark!!.geometry.longitude),
                selectedAttractions)
            dialog.dismiss()
        }
        attractionInfo.setNegativeButton("Назад") {dialog, which ->
            dialog.dismiss()
        }
        attractionInfo.create().show()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        drivingSession?.cancel()

        routeBuildingJob?.cancel()
        if (isLocationUpdatesActive && ::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        _binding = null
    }

    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            startLocationUpdates()
            setUserLocation(binding.mapView.mapWindow.map)
        }

    }
    override fun onProviderDisabled(provider: String) {}
}