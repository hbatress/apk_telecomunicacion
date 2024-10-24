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

class AirQualityTemperatureService : Service() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var lastAirQualityRange: Int? = null
    private var lastTemperatureRange: Int? = null
    private var isHighTemperatureNotificationSent = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AirQualityTemperatureService", "onStartCommand called")
        val userId = getUserIdFromCache()?.toIntOrNull() ?: -1
        Log.d("AirQualityTemperatureService", "User ID from cache: $userId")
        if (userId != -1) {
            startMonitoring(userId)
        }
        startForegroundService()
        return START_STICKY
    }

    private fun getUserIdFromCache(): String? {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("user_id", null)
    }

    private fun startMonitoring(userId: Int) {
        Log.d("AirQualityTemperatureService", "Starting monitoring for user ID: $userId")
        coroutineScope.launch {
            while (true) {
                Log.d("AirQualityTemperatureService", "Fetching air quality and temperature data")
                fetchAirQuality(userId)
                fetchTemperature(userId)
                delay(5000)
            }
        }
    }

    private fun fetchAirQuality(userId: Int) {
        Log.d("AirQualityTemperatureService", "Fetching air quality for user ID: $userId")
        RetrofitClient.instance.fetchAirQuality(userId).enqueue(object : Callback<NewAirQualityResponse> {
            override fun onResponse(call: Call<NewAirQualityResponse>, response: Response<NewAirQualityResponse>) {
                if (response.isSuccessful) {
                    val airQualityIndex = response.body()?.indice_calidad_aire ?: return
                    Log.d("AirQualityTemperatureService", "Air quality data received: $airQualityIndex")
                    val currentRange = getAirQualityRange(airQualityIndex)
                    Log.d("AirQualityTemperatureService", "Comparing air quality: $lastAirQualityRange vs $currentRange")
                    if (lastAirQualityRange != currentRange) {
                        sendAirQualityNotification(currentRange, airQualityIndex)
                        lastAirQualityRange = currentRange
                    }
                } else {
                    Log.e("AirQualityTemperatureService", "Failed to receive air quality data")
                }
            }

            override fun onFailure(call: Call<NewAirQualityResponse>, t: Throwable) {
                Log.e("AirQualityTemperatureService", "Error fetching air quality: ${t.message}")
            }
        })
    }

    private fun fetchTemperature(userId: Int) {
        Log.d("AirQualityTemperatureService", "Fetching temperature for user ID: $userId")
        RetrofitClient.instance.fetchTemperature(userId).enqueue(object : Callback<NewTemperatureResponse> {
            override fun onResponse(call: Call<NewTemperatureResponse>, response: Response<NewTemperatureResponse>) {
                if (response.isSuccessful) {
                    val temperature = response.body()?.temperatura ?: return
                    Log.d("AirQualityTemperatureService", "Temperature data received: $temperature")
                    val currentRange = getTemperatureRange(temperature)
                    Log.d("AirQualityTemperatureService", "Comparing temperature: $lastTemperatureRange vs $currentRange")
                    if (lastTemperatureRange != currentRange) {
                        sendTemperatureNotification(temperature)
                        lastTemperatureRange = currentRange
                    }
                    if (temperature > 40) {
                        if (!isHighTemperatureNotificationSent) {
                            sendHighTemperatureNotification(temperature)
                            isHighTemperatureNotificationSent = true
                        }
                    } else {
                        isHighTemperatureNotificationSent = false
                    }
                } else {
                    Log.e("AirQualityTemperatureService", "Failed to receive temperature data")
                }
            }

            override fun onFailure(call: Call<NewTemperatureResponse>, t: Throwable) {
                Log.e("AirQualityTemperatureService", "Error fetching temperature: ${t.message}")
            }
        })
    }

    private fun getAirQualityRange(index: Int): Int {
        Log.d("AirQualityTemperatureService", "Determining air quality range for index: $index")
        return when (index) {
            in 0..200 -> 0
            in 201..400 -> 1
            in 401..600 -> 2
            in 601..800 -> 3
            in 801..1000 -> 4
            in 1001..1200 -> 5
            in 1201..1400 -> 6
            in 1401..1600 -> 7
            in 1601..1800 -> 8
            in 1801..2000 -> 9
            else -> 10
        }
    }

    private fun getTemperatureRange(temperature: Double): Int {
        Log.d("AirQualityTemperatureService", "Determining temperature range for temperature: $temperature")
        return when (temperature) {
            in 0.0..10.0 -> 0
            in 10.1..20.0 -> 1
            in 20.1..30.0 -> 2
            in 30.1..40.0 -> 3
            in 40.1..Double.MAX_VALUE -> 4
            else -> -1
        }
    }

    private fun sendAirQualityNotification(range: Int, index: Int) {
        Log.d("AirQualityTemperatureService", "Sending air quality notification for range: $range, index: $index")
        val (color, description) = when (range) {
            0 -> Pair("#00E400", "Niveles normales de CO2 en el aire exterior")
            1 -> Pair("#A8D8A0", "Niveles aceptables; ideal para espacios interiores")
            2 -> Pair("#FFFF00", "Niveles aceptables, pero vigile el ambiente")
            3 -> Pair("#FF7E00", "Niveles moderadamente elevados; considere ventilar")
            4 -> Pair("#FF0000", "Niveles altos; se recomienda ventilar inmediatamente")
            5 -> Pair("#FF7E00", "Niveles muy altos; reducir la exposición")
            6 -> Pair("#FF0000", "Niveles peligrosos; evite el área y busque aire fresco")
            7 -> Pair("#7E0023", "Niveles críticos; peligro inminente para la salud")
            8 -> Pair("#7E0023", "Niveles muy críticos; situación de emergencia")
            9 -> Pair("#7E0023", "Extremadamente peligroso; evacuar de inmediato")
            else -> return
        }

        val notificationId = 1
        val channelId = "air_quality_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notificaciones de Calidad del Aire", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = "Notificaciones sobre cambios en la calidad del aire"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_air_quality)
            .setContentTitle("Alerta de Calidad del Aire")
            .setContentText(description)
            .setColor(android.graphics.Color.parseColor(color))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(this).notify(notificationId, notification)
                    Log.d("AirQualityTemperatureService", "Air quality notification sent: $description")
                } else {
                    Log.e("AirQualityTemperatureService", "Permission not granted to post notifications")
                }
            } else {
                NotificationManagerCompat.from(this).notify(notificationId, notification)
                Log.d("AirQualityTemperatureService", "Air quality notification sent: $description")
            }
        } catch (e: SecurityException) {
            Log.e("AirQualityTemperatureService", "SecurityException: ${e.message}")
        }
    }

    private fun sendTemperatureNotification(temperature: Double) {
        Log.d("AirQualityTemperatureService", "Sending temperature notification for temperature: $temperature")
        val color = when (lastTemperatureRange) {
            0 -> "#00BFFF"
            1 -> "#7FFF00"
            2 -> "#FFD700"
            3 -> "#FFA500"
            4 -> "#FF0000"
            else -> "#FFFFFF"
        }
        val description = "Temperatura actual: $temperature°C"

        val notificationId = 2
        val channelId = "temperature_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notificaciones de Temperatura", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = "Notificaciones sobre cambios en la temperatura"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_temperature)
            .setContentTitle("Alerta de Temperatura")
            .setContentText(description)
            .setColor(android.graphics.Color.parseColor(color))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(this).notify(notificationId, notification)
                    Log.d("AirQualityTemperatureService", "Temperature notification sent: $description")
                } else {
                    Log.e("AirQualityTemperatureService", "Permission not granted to post notifications")
                }
            } else {
                NotificationManagerCompat.from(this).notify(notificationId, notification)
                Log.d("AirQualityTemperatureService", "Temperature notification sent: $description")
            }
        } catch (e: SecurityException) {
            Log.e("AirQualityTemperatureService", "SecurityException: ${e.message}")
        }
    }

    private fun sendHighTemperatureNotification(temperature: Double) {
        Log.d("AirQualityTemperatureService", "Sending high temperature notification for temperature: $temperature")
        val channelId = "high_temperature_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notificaciones de Alta Temperatura", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = "Notificaciones sobre temperaturas altas"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_temperature)
            .setContentTitle("¡Alerta de Alta Temperatura!")
            .setContentText("La temperatura ha superado los 40°C. Temperatura actual: $temperature°C")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(3, notification)
            Log.d("AirQualityTemperatureService", "High temperature notification sent: $temperature°C")
            coroutineScope.launch {
                while (temperature > 40) {
                    NotificationManagerCompat.from(this@AirQualityTemperatureService).notify(3, notification)
                    delay(3000)
                }
            }
        } catch (e: SecurityException) {
            Log.e("AirQualityTemperatureService", "SecurityException: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("AirQualityTemperatureService", "onBind called")
        return null
    }

    private fun startForegroundService() {
        Log.d("AirQualityTemperatureService", "Starting foreground service")
        val channelId = "foreground_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Servicio en Primer Plano", NotificationManager.IMPORTANCE_LOW).apply {
                this.description = "Canal del Servicio en Primer Plano"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitoreo de Calidad del Aire y Temperatura")
            .setContentText("El servicio está ejecutándose en primer plano")
            .setSmallIcon(R.drawable.ic_settings)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
        Log.d("AirQualityTemperatureService", "Foreground service started")
    }
}