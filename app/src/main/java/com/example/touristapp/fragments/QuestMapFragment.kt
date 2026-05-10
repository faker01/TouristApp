package com.example.touristapp.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.touristapp.R
import com.example.touristapp.databinding.FragmentMapBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.runtime.image.ImageProvider
import android.location.LocationListener

class QuestMapFragment : Fragment(), LocationListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var locationManager: LocationManager
    private var drivingSession: DrivingSession? = null
    private var currentLocation: Location? = null
    private var targetLat: Double = 54.7065
    private var targetLon: Double = 20.5090
    private var targetTitle: String = ""
    private var isLocationUpdatesActive = false
    private var userMarker: com.yandex.mapkit.map.PlacemarkMapObject? = null

    private val drivingRouteListener = object : DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
            if (routes.isEmpty()) return
            val route = routes[0]
            binding.mapView.mapWindow.map.mapObjects.addPolyline(route.geometry).apply {
                setStrokeColor(0xFF3390FF.toInt())
                setStrokeWidth(5f)
            }
            Toast.makeText(requireContext(), "Маршрут построен! Следуйте до цели.", Toast.LENGTH_SHORT).show()
        }

        override fun onDrivingRoutesError(error: com.yandex.runtime.Error) {
            Toast.makeText(requireContext(), "Ошибка построения маршрута", Toast.LENGTH_SHORT).show()
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

        targetLat = arguments?.getDouble("point_lat", 54.7065) ?: 54.7065
        targetLon = arguments?.getDouble("point_lon", 20.5090) ?: 20.5090
        targetTitle = arguments?.getString("point_title", "Цель") ?: "Цель"

        val yandexMap = binding.mapView.mapWindow.map
        val imageProvider = ImageProvider.fromResource(requireContext(), R.drawable.ic_marker)

        // Добавляем метку цели
        yandexMap.mapObjects.addPlacemark().apply {
            geometry = Point(targetLat, targetLon)
            setIcon(imageProvider, IconStyle().apply { scale = 0.07f })
            addTapListener { _, _ ->
                Toast.makeText(requireContext(), "🎯 $targetTitle", Toast.LENGTH_SHORT).show()
                true
            }
        }

        Toast.makeText(requireContext(), "🎯 Ваша цель: $targetTitle", Toast.LENGTH_LONG).show()

        // Инициализация геолокации и построение маршрута
        initLocationUpdates()

        // Через 10 секунд закрываем карту и переходим к заданиям
        handler.postDelayed({
            navigateToQuest()
        }, 10000)

        binding.btnBack.setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            navigateToQuest()
        }
    }

    private fun initLocationUpdates() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                5f,
                this,
                Looper.getMainLooper()
            )
            isLocationUpdatesActive = true

            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastKnown?.let {
                currentLocation = it
                addUserMarker(binding.mapView.mapWindow.map, it)
                buildRoute()
            }
        }
    }

    private fun addUserMarker(yandexMap: com.yandex.mapkit.map.Map, location: Location) {
        val userIcon = ImageProvider.fromResource(requireContext(), R.drawable.ic_user_marker)
        userMarker = yandexMap.mapObjects.addPlacemark().apply {
            geometry = Point(location.latitude, location.longitude)
            setIcon(userIcon, IconStyle().apply { scale = 0.05f })
        }
    }

    private fun updateUserMarker(location: Location) {
        userMarker?.geometry = Point(location.latitude, location.longitude)
    }

    private fun buildRoute() {
        val current = currentLocation ?: return

        val drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)
        val drivingOptions = DrivingOptions().apply { routesCount = 1 }
        val vehicleOptions = VehicleOptions()

        val points = listOf(
            RequestPoint(
                Point(current.latitude, current.longitude),
                RequestPointType.WAYPOINT,
                null,
                null,
                null
            ),
            RequestPoint(
                Point(targetLat, targetLon),
                RequestPointType.WAYPOINT,
                null,
                null,
                null
            )
        )

        drivingSession?.cancel()
        drivingSession = drivingRouter.requestRoutes(points, drivingOptions, vehicleOptions, drivingRouteListener)

        // Перемещаем камеру, чтобы показать весь маршрут
        val midLat = (current.latitude + targetLat) / 2
        val midLon = (current.longitude + targetLon) / 2
        binding.mapView.mapWindow.map.move(CameraPosition(Point(midLat, midLon), 13f, 0f, 0f))
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        updateUserMarker(location)
        buildRoute()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }

    private fun navigateToQuest() {
        val csvFileName = arguments?.getString("csv_file_name") ?: "Homlini_1_dedKarl.csv"
        val questRouteId = arguments?.getInt("quest_route_id", 0) ?: 0
        val currentPointIndex = arguments?.getInt("current_point_index", 0) ?: 0

        val bundle = Bundle().apply {
            putString("csv_file_name", csvFileName)
            putInt("route_index", questRouteId)
            putInt("point_index", currentPointIndex)
            putBoolean("from_route", true)
        }
        findNavController().navigate(R.id.action_quest_map_to_quest, bundle)
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        drivingSession?.cancel()
        handler.removeCallbacksAndMessages(null)
        if (isLocationUpdatesActive && ::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        _binding = null
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}