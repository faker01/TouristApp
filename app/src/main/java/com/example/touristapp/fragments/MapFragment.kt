package com.example.touristapp.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.touristapp.databinding.FragmentMapBinding
import com.example.touristapp.models.Attraction
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import android.location.LocationListener
import android.widget.Toast
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
    private var adminUserLocation: Array<Double> = arrayOf(54.710325, 20.510053)
    private var adminMode: Boolean = false

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
            Toast.makeText(requireContext(), "Ошибка маршрута", Toast.LENGTH_SHORT).show()
        }
    }

    private var currentRoutePoints: List<Attraction>? = null
    private var currentPolyline: PolylineMapObject? = null
    private val placemarks = mutableListOf<PlacemarkMapObject>()


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

        binding.mapView.onStart()
        MapKitFactory.getInstance().onStart()

        val yandexMap = binding.mapView.mapWindow.map
        yandexMap.isIndoorEnabled = false
        yandexMap.poiLimit = 0

        attractionsCollection = yandexMap.mapObjects.addCollection()

        initLocationUpdates()

        adminMode = AppState.isAdmin
        if (adminMode)
        {
            adminUserLocation = getUserLocation() ?: arrayOf(54.710325, 20.510053)
        }

        setUserLocation(yandexMap)

        val selected = arguments
            ?.getSerializable("selected_attractions") as? List<Attraction>

        questScript = arguments?.getSerializable("quest_script") as? QuestScript

        if (selected != null) {
            // пришли из AttractionsFragment — показываем выбранные места
            selectedAttractions = selected
            visitedAttractionIds.clear()
            isFollowing = true
            getAttractions(selected)
            drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)
            getRoute(yandexMap, getUserLocation(), selected)
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

        // Показываем крестовину только в adminMode
        if (adminMode) {
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
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                10f,
                this,
                Looper.getMainLooper()
            )
            isLocationUpdatesActive = true
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
        if (adminMode && !adminUserLocation.isEmpty())
        {
            return adminUserLocation
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastKnown?.let {
                return arrayOf(it.latitude, it.longitude)
            }
        }
        return null
    }

    override fun onLocationChanged(location: Location) {
        if (adminMode) return

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
                        binding.mapView.mapWindow.map,
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

        Toast.makeText(
            requireContext(),
            "📍 Вы достигли: ${attraction.title}",
            Toast.LENGTH_LONG
        ).show()

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }

    // локация маршрута
    private fun getAttractions(selected: List<Attraction>? = null) {
        val imageProvider = ImageProvider.fromResource(requireContext(), R.drawable.ic_marker)

        val attractions: List<Attraction>

        if (selected != null) {
            attractions = selected
        }
        else {
            attractions = db.getAllAttractions()
        }

        attractions.forEach { attraction ->
            attractionsCollection!!.addPlacemark().apply {
                geometry = Point(attraction.lat, attraction.lon)
                setIcon(
                    imageProvider,
                    IconStyle().apply {
                        scale = 0.05f
                    })
                addTapListener { mapObject, point ->
                    showAttractionInfo(attraction)
                    true
                }
            }
        }
    }

    private fun getRoute(yandexMap: Map, userPointCoordinates: Array<Double>?, selected: List<Attraction>) {
        drivingSession?.cancel()

        val drivingOptions = DrivingOptions().apply {
            routesCount = 1
        }
        val vehicleOptions = VehicleOptions()

        val points = buildList { if (userPointCoordinates != null) add(
            RequestPoint(
                Point(userPointCoordinates[0], userPointCoordinates[1]),
                RequestPointType.WAYPOINT,
                null,
                null,
                null
            )
        )
            selected.forEach { point ->
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
        Toast.makeText(requireContext(), attraction.title, Toast.LENGTH_SHORT).show()
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

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            startLocationUpdates()
            setUserLocation(binding.mapView.mapWindow.map)
        }

    }
    override fun onProviderDisabled(provider: String) {}
}