package com.example.myhouse

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AirQualityNotificationService : Service() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var lastAirQualityRange: Int? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getIntExtra("USER_ID", -1) ?: -1
        if (userId != -1) {
            startMonitoringAirQuality(userId)
        }
        return START_STICKY
    }

    private fun startMonitoringAirQuality(userId: Int) {
        coroutineScope.launch {
            while (true) {
                RetrofitClient.instance.getAirQuality(userId).enqueue(object : Callback<AirQualityResponse> {
                    override fun onResponse(call: Call<AirQualityResponse>, response: Response<AirQualityResponse>) {
                        if (response.isSuccessful) {
                            val airQualityIndex = response.body()?.indice_calidad_aire ?: return
                            val currentRange = getAirQualityRange(airQualityIndex)
                            if (currentRange != lastAirQualityRange) {
                                sendNotification(currentRange, airQualityIndex)
                                lastAirQualityRange = currentRange
                            }
                        }
                    }

                    override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                        // Handle failure
                    }
                })
                delay(2000) // Delay for 2 seconds before making the next request
            }
        }
    }

    private fun getAirQualityRange(index: Int): Int {
        return when (index) {
            in 0..400 -> 0
            in 401..1000 -> 1
            in 1001..2000 -> 2
            in 2001..5000 -> 3
            else -> 4
        }
    }

    private fun sendNotification(range: Int, index: Int) {
        val (color, description) = when (range) {
            0 -> Pair("#00E400", "Niveles normales de CO2 en el aire exterior")
            1 -> Pair("#FFFF00", "Niveles aceptables en espacios interiores")
            2 -> Pair("#FF7E00", "Niveles moderadamente elevados")
            3 -> Pair("#FF0000", "Niveles altos")
            4 -> Pair("#7E0023", "Niveles peligrosos")
            else -> return
        }

        val channelId = "air_quality_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Air Quality Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = "Notifications for air quality changes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_air_quality)
            .setContentTitle("Alerta de Calidad del Aire")
            .setContentText("Calidad de Aire: $description (con nivel de $index)")
            .setColor(android.graphics.Color.parseColor(color))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(this).notify(range, notification)
                } else {
                    // Handle the case where permission is not granted
                    Log.e("AirQualityNotificationService", "Permission not granted for posting notifications")
                }
            } else {
                NotificationManagerCompat.from(this).notify(range, notification)
            }
        } catch (e: SecurityException) {
            Log.e("AirQualityNotificationService", "SecurityException: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}