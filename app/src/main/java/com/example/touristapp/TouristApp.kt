package com.example.touristapp

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class TouristApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("5aaca35f-48df-4877-8b53-d680af04cd8f")
    }
}