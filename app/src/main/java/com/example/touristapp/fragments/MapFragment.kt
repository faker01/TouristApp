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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.touristapp.databinding.FragmentMapBinding
import com.example.touristapp.models.Attraction
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import android.location.LocationListener
import com.example.touristapp.R
import com.example.touristapp.data.AttractionsData
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
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Job
import com.yandex.runtime.Error

class MapFragment : Fragment(), LocationListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationManager: LocationManager
    private var userPlacemark: PlacemarkMapObject? = null
    private var attractionsCollection: MapObjectCollection? = null
    private lateinit var drivingRouter: com.yandex.mapkit.directions.driving.DrivingRouter
    private var drivingSession: DrivingSession? = null
    private var currentPolyline: PolylineMapObject? = null
    private var routeBuildingJob: Job? = null
    private var isLocationUpdatesActive = false
    private var currentLocation: Location? = null

    private val drivingRouteListener = object : DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
            if (drivingRoutes.isEmpty()) return
            val route = drivingRoutes[0]
            currentPolyline = binding.mapView.mapWindow.map.mapObjects.addPolyline(route.geometry).apply {
                setStrokeColor(0xFF3390FF.toInt())
            }
        }

        override fun onDrivingRoutesError(error: Error) {
            Toast.makeText(requireContext(), "Ошибка маршрута", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.onStart()
        MapKitFactory.getInstance().onStart()

        val yandexMap = binding.mapView.mapWindow.map
        yandexMap.isIndoorEnabled = false
        yandexMap.poiLimit = 0

        attractionsCollection = yandexMap.mapObjects.addCollection()

        initLocationUpdates()
        setUserLocation(yandexMap)

        val selected = arguments
            ?.getSerializable("selected_attractions") as? List<Attraction>

        if (selected != null) {
            getAttractions(yandexMap, selected)
            drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)
            getRoute(yandexMap, selected)
        } else {
            getAttractions(yandexMap)
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
    }

    // ========== ГЕОЛОКАЦИЯ ==========

    private fun initLocationUpdates() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Проверяем, есть ли разрешение
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Запрашиваем разрешение
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        // Разрешение есть - запускаем обновление геолокации
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Запрашиваем обновления геолокации
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,  // обновление каждые 2 секунды
                5f,     // или при изменении на 5 метров
                this,
                Looper.getMainLooper()
            )
            isLocationUpdatesActive = true

            // Получаем последнюю известную позицию
            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastKnown?.let {
                currentLocation = it
                updateUserMarkerPosition(it)
            }
        }
    }

    private fun updateUserMarkerPosition(location: Location) {
        val yandexMap = binding.mapView.mapWindow.map
        val point = Point(location.latitude, location.longitude)

        if (userPlacemark == null) {
            userPlacemark = yandexMap.mapObjects.addPlacemark().apply {
                geometry = point
                setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_user_marker),
                    IconStyle().apply { scale = 0.05f })
            }
        } else {
            userPlacemark?.geometry = point
        }

        // Плавно перемещаем камеру к пользователю
        yandexMap.move(CameraPosition(point, yandexMap.cameraPosition.zoom, 0.0f, 0.0f))
    }

    private fun setUserLocation(yandexMap: Map) {
        val currentLocation = currentLocation
        if (currentLocation != null) {
            val point = Point(currentLocation.latitude, currentLocation.longitude)
            yandexMap.move(CameraPosition(point, 14.0f, 0.0f, 0.0f))
            userPlacemark = yandexMap.mapObjects.addPlacemark().apply {
                geometry = point
                setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_user_marker),
                    IconStyle().apply { scale = 0.05f })
            }
        } else {
            // Центр Калининграда по умолчанию
            val defaultLat = 54.7065
            val defaultLon = 20.5090
            yandexMap.move(CameraPosition(Point(defaultLat, defaultLon), 12.0f, 0.0f, 0.0f))
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        updateUserMarkerPosition(location)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено - запускаем геолокацию
                startLocationUpdates()
                Toast.makeText(requireContext(), "Доступ к геолокации разрешён", Toast.LENGTH_SHORT).show()
            } else {
                // Разрешение не получено
                Toast.makeText(requireContext(), "Для работы квеста нужен доступ к геолокации", Toast.LENGTH_LONG).show()
                // Показываем центр Калининграда
                val yandexMap = binding.mapView.mapWindow.map
                yandexMap.move(CameraPosition(Point(54.7065, 20.5090), 12.0f, 0.0f, 0.0f))
            }
        }
    }

    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER && !isLocationUpdatesActive) {
            startLocationUpdates()
            Toast.makeText(requireContext(), "GPS включён, определяю местоположение...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            isLocationUpdatesActive = false
            Toast.makeText(requireContext(), "GPS выключен. Включите GPS для навигации", Toast.LENGTH_LONG).show()
        }
    }

    // ========== ОСТАЛЬНЫЕ МЕТОДЫ БЕЗ ИЗМЕНЕНИЙ ==========

    private fun getAttractions(yandexMap: Map, selected: List<Attraction>? = null) {
        val imageProvider = ImageProvider.fromResource(requireContext(), R.drawable.ic_marker)
        val attractions: List<Attraction> = selected ?: AttractionsData.all
        attractions.forEach { attraction ->
            attractionsCollection!!.addPlacemark().apply {
                geometry = Point(attraction.lat, attraction.lon)
                setIcon(imageProvider, IconStyle().apply { scale = 0.05f })
                addTapListener { _, _ ->
                    showAttractionInfo(attraction)
                    true
                }
            }
        }
    }

    private fun getRoute(yandexMap: Map, selected: List<Attraction>) {
        drivingSession?.cancel()
        val drivingOptions = DrivingOptions().apply { routesCount = 1 }
        val vehicleOptions = VehicleOptions()
        val userPointCoordinates = currentLocation?.let { arrayOf(it.latitude, it.longitude) }
        val points = buildList {
            if (userPointCoordinates != null) {
                add(RequestPoint(Point(userPointCoordinates[0], userPointCoordinates[1]), RequestPointType.WAYPOINT, null, null, null))
            }
            selected.forEach { point ->
                add(RequestPoint(Point(point.lat, point.lon), RequestPointType.WAYPOINT, null, null, null))
            }
        }
        if (points.size >= 2) {
            drivingSession = drivingRouter.requestRoutes(points, drivingOptions, vehicleOptions, drivingRouteListener)
        }
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
}