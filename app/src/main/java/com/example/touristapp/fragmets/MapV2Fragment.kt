package com.example.touristapp.fragmets

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.touristapp.R
import com.example.touristapp.databinding.FragmentMapBinding
import com.example.touristapp.models.Attraction
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import kotlinx.coroutines.Job
import kotlin.collections.forEach

class MapV2Fragment: Fragment(), LocationListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationManager: LocationManager

    private var currentRoutePoints: List<Attraction>? = null
    private var currentPolyline: PolylineMapObject? = null
    private val placemarks = mutableListOf<PlacemarkMapObject>()

    private var routeBuildingJob: Job? = null
    private var isLocationUpdatesActive = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MapKitFactory.initialize(requireContext())
        binding.mapView.onStart()
        MapKitFactory.getInstance().onStart()

        // MapKit 4.x: карта получается через mapView.map
        val yandexMap = binding.mapView.map

        val kaliningrad = Point(54.7065, 20.5090)
        yandexMap.move(CameraPosition(kaliningrad, 12.0f, 0.0f, 0.0f))

        processArgumentsAndShowRoute(yandexMap)

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

        binding.btnCenter.setOnClickListener {
            moveToUserLocation(yandexMap)
        }

        binding.btnStart.setOnClickListener {
            currentRoutePoints?.let { attractions ->
                if (attractions.isNotEmpty()) openNavigator(attractions)
            }
        }

        initLocationUpdates()
    }

    private fun processArgumentsAndShowRoute(yandexMap: com.yandex.mapkit.map.Map) {
        arguments?.let { args ->
            val routeName = args.getString("route_name")
            if (routeName != null) {
                val attractionsNames = args.getStringArrayList("route_attractions") ?: emptyList<String>()
                currentRoutePoints = attractionsNames.mapNotNull { getAttractionByName(it) }
                updateRouteInfo(
                    routeName,
                    args.getString("route_duration") ?: "",
                    args.getString("route_distance") ?: "",
                    currentRoutePoints?.size ?: 0
                )
                currentRoutePoints?.let { showRouteOnMap(yandexMap, it) }
            } else {
                @Suppress("UNCHECKED_CAST")
                val selected = args.getSerializable("selected_attractions") as? ArrayList<Attraction>
                if (selected != null) {
                    currentRoutePoints = selected
                    updateRouteInfo("Ваш маршрут", "—", "—", selected.size)
                    showRouteOnMap(yandexMap, selected)
                }
            }
        }
    }

    private fun getAttractionByName(name: String): Attraction? {
        return listOf(
            Attraction("Собор", 54.7065, 20.5090),
            Attraction("Рыбная деревня", 54.7030, 20.5095),
            Attraction("Королевские ворота", 54.7210, 20.5155),
            Attraction("Форт №5", 54.7240, 20.4550),
            Attraction("Музей океана", 54.7044, 20.4994)
        ).find { it.title == name }
    }

    private fun updateRouteInfo(title: String, duration: String, distance: String, count: Int) {
        binding.tvTitle.text = title
        binding.tvDuration.text = "⏱ $duration"
        binding.tvDistance.text = "📏 $distance"
        binding.tvPlaces.text = "📍 $count мест"
    }

    private fun showRouteOnMap(yandexMap: com.yandex.mapkit.map.Map, attractions: List<Attraction>) {
        yandexMap.mapObjects.clear()
        placemarks.clear()
        currentPolyline = null

        attractions.forEach { attr ->
            val point = Point(attr.lat, attr.lon)
            val placemark = yandexMap.mapObjects.addPlacemark(point)
            placemark.setText(attr.title)
            placemarks.add(placemark)
        }

        if (attractions.size >= 2) {
            val points = attractions.map { Point(it.lat, it.lon) }
            currentPolyline = yandexMap.mapObjects.addPolyline(Polyline(points))
            currentPolyline?.apply {
                setStrokeColor(ContextCompat.getColor(requireContext(), R.color.teal_200))
                setStrokeWidth(8.0f)
            }
            attractions.first().let {
                yandexMap.move(CameraPosition(Point(it.lat, it.lon), 14.0f, 0.0f, 0.0f))
            }
        }
    }

    private fun openNavigator(attractions: List<Attraction>) {
        val first = attractions.first()
        val uri = Uri.parse("yandexnavi://build_route_on_map?lat_to=${first.lat}&lon_to=${first.lon}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            val gmmIntentUri = Uri.parse("google.navigation:q=${first.lat},${first.lon}")
            startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
        }
    }

    // ---------- Геолокация ----------
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

    private fun moveToUserLocation(yandexMap: com.yandex.mapkit.map.Map) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastKnown?.let {
                yandexMap.move(CameraPosition(Point(it.latitude, it.longitude), 15.0f, 0.0f, 0.0f))
            }
        }
    }

    override fun onLocationChanged(location: Location) {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
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
        routeBuildingJob?.cancel()
        if (isLocationUpdatesActive && ::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        _binding = null
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}