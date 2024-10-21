package com.example.myhouse

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myhouse.ui.theme.MyHouseTheme
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

class AirQualityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceId = intent.getIntExtra("DEVICE_ID", -1)
        val userId = getUserIdFromCache(this)?.toIntOrNull()

        setContent {
            MyHouseTheme {
                if (userId != null) {
                    AirQualityScreen(deviceId, userId)
                } else {
                    Text(text = "User ID not found")
                }
            }
        }
    }
}

@Composable
fun AirQualityScreen(deviceId: Int, userId: Int) {
    var airQualityData by remember { mutableStateOf<AirQualityResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(userId) {
        while (true) {
            RetrofitClient.instance.getAirQuality(userId).enqueue(object : Callback<AirQualityResponse> {
                override fun onResponse(call: Call<AirQualityResponse>, response: Response<AirQualityResponse>) {
                    if (response.isSuccessful) {
                        airQualityData = response.body()
                    } else {
                        errorMessage = "No se encontraron datos de calidad de aire para este usuario"
                    }
                }

                override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                    Log.e("AirQualityScreen", "Error: ${t.message}")
                    errorMessage = "Error en la solicitud"
                }
            })
            delay(5000) // Delay for 5 seconds before making the next request
        }
    }

    if (airQualityData != null) {
        AirQualityContent(airQualityData!!)
    } else if (errorMessage != null) {
        Text(text = errorMessage!!)
    } else {
        Text(text = "Cargando datos...")
    }
}

@Composable
fun AirQualityContent(data: AirQualityResponse) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Título: Nombre del dispositivo
        Text(text = data.NombreDispositivo, fontSize = 24.sp, style = MaterialTheme.typography.headlineSmall)

        // Icono
        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(100.dp).padding(16.dp))

        // Índice de calidad de aire
        Text(text = "Calidad de aire: ${data.indice_calidad_aire}", fontSize = 20.sp)

        // Gráfica de semáforo
        TrafficLight(data.indice_calidad_aire)

        // Fecha y hora
        val dateTime = data.fecha.split("T")
        val date = dateTime[0].split("-")
        val time = data.hora.split(":")
        Text(text = "Fecha: ${date[2]}/${date[1]}/${date[0]} Hora: ${time[0]}:${time[1]}", fontSize = 16.sp)
    }
}

@Composable
fun TrafficLight(airQualityIndex: Int) {
    val color = when (airQualityIndex) {
        in 0..50 -> Color.Green
        in 51..100 -> Color.Yellow
        in 101..150 -> Color(0xFFFFA500) // Orange
        in 151..200 -> Color.Red
        in 201..300 -> Color(0xFF8B0000) // Dark Red
        else -> Color(0xFF800080) // Purple
    }

    Canvas(modifier = Modifier.size(100.dp)) {
        drawCircle(color = color, radius = size.minDimension / 2)
    }
}

@Preview(showBackground = true)
@Composable
fun AirQualityScreenPreview() {
    MyHouseTheme {
        AirQualityContent(
            AirQualityResponse(
                indice_calidad_aire = 140,
                fecha = "2024-10-21T06:00:00.000Z",
                hora = "08:23:52",
                NombreDispositivo = "Garaje"
            )
        )
    }
}