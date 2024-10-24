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

class TemperatureMonitorService : Service() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var lastTemperatureRange: Int? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getIntExtra("USER_ID", -1) ?: -1
        if (userId != -1) {
            startMonitoringTemperature(userId)
        }
        startForegroundService()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "temperature_monitor_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Temperature Monitor Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Temperature Monitor Service")
            .setContentText("Monitoring temperature changes...")
            .setSmallIcon(R.drawable.ic_temperature)
            .build()

        startForeground(1, notification)
    }

    private fun startMonitoringTemperature(userId: Int) {
        coroutineScope.launch {
            while (true) {
                RetrofitClient.instance.getTemperature(userId).enqueue(object : Callback<TemperatureResponse> {
                    override fun onResponse(call: Call<TemperatureResponse>, response: Response<TemperatureResponse>) {
                        if (response.isSuccessful) {
                            val temperature = response.body()?.temperatura ?: return
                            val currentRange = getTemperatureRange(temperature)
                            if (currentRange != lastTemperatureRange) {
                                sendNotification(currentRange, temperature)
                                lastTemperatureRange = currentRange
                            }
                        }
                    }

                    override fun onFailure(call: Call<TemperatureResponse>, t: Throwable) {
                        // Handle failure
                    }
                })
                delay(2000) // Delay for 2 seconds before making the next request
            }
        }
    }

    private fun getTemperatureRange(temperature: Double): Int {
        return when {
            temperature <= 0 -> 0
            temperature in 1.0..10.0 -> 1
            temperature in 11.0..20.0 -> 2
            temperature in 21.0..30.0 -> 3
            temperature in 31.0..40.0 -> 4
            temperature in 41.0..50.0 -> 5
            else -> 6
        }
    }

    private fun sendNotification(range: Int, temperature: Double) {
        val (color, description) = when (range) {
            0 -> Pair("#0000FF", "Very Cold")
            1 -> Pair("#00FFFF", "Cold")
            2 -> Pair("#00FF00", "Cool")
            3 -> Pair("#FFFF00", "Warm")
            4 -> Pair("#FFA500", "Hot")
            5 -> Pair("#FF0000", "Very Hot")
            else -> return
        }

        val channelId = "temperature_alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Temperature Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = "Notifications for temperature changes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_temperature)
            .setContentTitle("Temperature Alert")
            .setContentText("Temperature: $description (currently $temperature°C)")
            .setColor(android.graphics.Color.parseColor(color))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(this).notify(range, notification)
                } else {
                    // Handle the case where permission is not granted
                    Log.e("TemperatureMonitorService", "Permission not granted for posting notifications")
                }
            } else {
                NotificationManagerCompat.from(this).notify(range, notification)
            }
        } catch (e: SecurityException) {
            Log.e("TemperatureMonitorService", "SecurityException: ${e.message}")
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