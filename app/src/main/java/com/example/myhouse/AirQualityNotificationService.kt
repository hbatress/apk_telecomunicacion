package com.example.myhouse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
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
            in 0..50 -> 0
            in 51..100 -> 1
            in 101..150 -> 2
            in 151..200 -> 3
            in 201..300 -> 4
            in 301..500 -> 5
            else -> -1
        }
    }

    private fun sendNotification(range: Int, index: Int) {
        val (color, description) = when (range) {
            0 -> Pair("#00E400", "Buena")
            1 -> Pair("#FFFF00", "Moderada")
            2 -> Pair("#FF7E00", "No Saludable para Grupos Sensibles")
            3 -> Pair("#FF0000", "No Saludable")
            4 -> Pair("#8F3F97", "Muy No Saludable")
            5 -> Pair("#7E0023", "Peligrosa")
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
            .setContentTitle("Air Quality Alert")
            .setContentText("Air Quality is $description (Index: $index)")
            .setColor(android.graphics.Color.parseColor(color))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(range, notification)
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