package com.example.myhouse

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myhouse.ui.theme.MyHouseTheme
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TemperatureControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceId = intent.getIntExtra("DEVICE_ID", -1)
        val userId = getUserIdFromCache(this)?.toIntOrNull()
        setContent {
            MyHouseTheme {
                TemperatureControlScreen(deviceId, userId)
            }
        }
    }
}

@Composable
fun TemperatureControlScreen(deviceId: Int, userId: Int?) {
    var temperatureData by remember { mutableStateOf<TemperatureResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var isActive by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            while (isActive) {
                RetrofitClient.instance.getTemperature(userId).enqueue(object : Callback<TemperatureResponse> {
                    override fun onResponse(call: Call<TemperatureResponse>, response: Response<TemperatureResponse>) {
                        if (response.isSuccessful) {
                            temperatureData = response.body()
                        } else {
                            errorMessage = "Error al obtener los datos de temperatura"
                        }
                    }

                    override fun onFailure(call: Call<TemperatureResponse>, t: Throwable) {
                        Log.e("TemperatureControlScreen", "Error: ${t.message}")
                        errorMessage = "Error en la solicitud"
                    }
                })
                delay(1000) // Wait for 1 second before fetching the data again
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isActive = false
        }
    }

    if (temperatureData != null) {
        TemperatureContent(temperatureData!!)
    } else if (errorMessage != null) {
        Text(text = errorMessage!!)
    } else {
        Text(text = "Cargando datos...")
    }
}

@Composable
fun TemperatureContent(data: TemperatureResponse) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Título: Nombre del dispositivo
        Text(text = data.NombreDispositivo, fontSize = 24.sp, style = MaterialTheme.typography.headlineSmall)

        // Temperatura
        Text(text = "Temperatura: ${data.temperatura}°C", fontSize = 20.sp)

        // Fecha y hora
        val dateTime = data.fecha.split("T")
        val date = dateTime[0].split("-")
        val time = data.hora.split(":")
        Text(text = "Fecha: ${date[2]}/${date[1]}/${date[0]} Hora: ${time[0]}:${time[1]}", fontSize = 16.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun TemperatureControlScreenPreview() {
    MyHouseTheme {
        TemperatureContent(
            TemperatureResponse(
                temperatura = 33.1,
                fecha = "2024-10-21T06:00:00.000Z",
                hora = "18:25:57",
                NombreDispositivo = "Patio"
            )
        )
    }
}