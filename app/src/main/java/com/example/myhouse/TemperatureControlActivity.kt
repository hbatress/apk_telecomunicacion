package com.example.myhouse

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.myhouse.ui.theme.MyHouseTheme
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import androidx.compose.ui.graphics.luminance
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class TemperatureControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceId = intent.getIntExtra("DEVICE_ID", -1)
        val userId = getUserIdFromCache(this)?.toIntOrNull()
        setContent {
            MyHouseTheme {
                if (userId != null) {
                    TemperatureControl2Screen(deviceId, userId)
                } else {
                    Text(text = "User ID not found")
                }
            }
        }
    }
}

@Composable
fun TemperatureControl2Screen(deviceId: Int, userId: Int) {
    var temperatureData by remember { mutableStateOf<TemperatureResponse?>(null) }
    var temperatureAverageData by remember { mutableStateOf<List<TemperatureAverageResponse>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(userId) {
        while (true) {
            RetrofitClient.instance.getTemperature(userId).enqueue(object : Callback<TemperatureResponse> {
                override fun onResponse(call: Call<TemperatureResponse>, response: Response<TemperatureResponse>) {
                    if (response.isSuccessful) {
                        temperatureData = response.body()
                    } else {
                        errorMessage = "No se encontraron datos de temperatura para este usuario"
                    }
                }

                override fun onFailure(call: Call<TemperatureResponse>, t: Throwable) {
                    Log.e("TemperatureControl2Screen", "Error: ${t.message}")
                    errorMessage = "Error en la solicitud"
                }
            })

            RetrofitClient.instance.getTemperatureAverage(userId).enqueue(object : Callback<List<TemperatureAverageResponse>> {
                override fun onResponse(call: Call<List<TemperatureAverageResponse>>, response: Response<List<TemperatureAverageResponse>>) {
                    if (response.isSuccessful) {
                        temperatureAverageData = response.body()
                    } else {
                        errorMessage = "No se encontraron datos de temperatura para este usuario"
                    }
                }

                override fun onFailure(call: Call<List<TemperatureAverageResponse>>, t: Throwable) {
                    Log.e("TemperatureControl2Screen", "Error: ${t.message}")
                    errorMessage = "Error en la solicitud"
                }
            })

            delay(5000) // Delay for 5 seconds before making the next request
        }
    }

    if (temperatureData != null && temperatureAverageData != null) {
        TemperatureContent2(temperatureData!!, temperatureAverageData!!)
    } else if (errorMessage != null) {
        Text(text = errorMessage!!)
    } else {
        Text(text = "Cargando datos...")
    }
}

@Composable
fun TemperatureContent2(data: TemperatureResponse, averageData: List<TemperatureAverageResponse>) {
    val maxTemperature = averageData.maxByOrNull { it.promedio_temperatura }?.promedio_temperatura ?: 0.0
    val yAxisMax = ((maxTemperature + 5) / 5).toInt() * 5 // Round up to the nearest 5

    // Parse and format the date
    val parsedDate = ZonedDateTime.parse(data.fecha)
    val formattedDate = parsedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Título: Nombre del dispositivo
        Text(text = data.NombreDispositivo, fontSize = 32.sp, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))

        // Gráfica de promedio de temperatura
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f).padding(top = 32.dp)) {
            val barWidth = size.width / 24
            val points = mutableListOf<Offset?>()

            for (i in 0..23) {
                val temperature = averageData.find { it.hora == i }
                if (temperature != null) {
                    points.add(
                        Offset(
                            i * barWidth,
                            size.height - (temperature.promedio_temperatura.toFloat() / yAxisMax.toFloat() * size.height)
                        )
                    )
                } else {
                    points.add(null)
                }
            }

            // Draw X and Y axes
            drawLine(
                color = Color.Black,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Black,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 2f
            )

            // Draw X axis labels
            for (i in 0..24) {
                drawContext.canvas.nativeCanvas.drawText(
                    i.toString(),
                    i * barWidth,
                    size.height + 20,
                    Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 24f
                    }
                )
            }

            // Draw Y axis labels
            for (i in 0..yAxisMax step 5) {
                val y = size.height - (i / yAxisMax.toFloat() * size.height)
                drawContext.canvas.nativeCanvas.drawText(
                    i.toString(),
                    -40f,
                    y,
                    Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 24f
                    }
                )
            }

            // Draw points and lines
            for (i in 0 until points.size - 1) {
                if (points[i] != null) {
                    drawCircle(
                        color = Color.Cyan,
                        radius = 8f, // Larger points
                        center = points[i]!!
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 8f,
                        center = points[i]!!,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
                if (points[i] != null && points[i + 1] != null) {
                    drawLine(
                        color = Color.Cyan,
                        start = points[i]!!,
                        end = points[i + 1]!!,
                        strokeWidth = 6f // Thicker lines
                    )
                }
            }
            // Draw the last point
            if (points.last() != null) {
                drawCircle(
                    color = Color.Cyan,
                    radius = 8f,
                    center = points.last()!!
                )
                drawCircle(
                    color = Color.Black,
                    radius = 8f,
                    center = points.last()!!,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Add space between the graph and the text

        // Promedio de temperatura, fecha y hora en un rectángulo con bordes semiredondos
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(8.dp)
                .background(color = Color.LightGray, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(text = "Fecha: $formattedDate", fontSize = 20.sp, color = Color.Black)
                    Text(text = "Hora: ${data.hora}", fontSize = 20.sp, color = Color.Black)
                }
                Text(
                    text = "${data.temperatura}°C",
                    fontSize = 48.sp, // Larger font size
                    fontWeight = FontWeight.Bold, // Bold text
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TemperatureControl2ScreenPreview() {
    MyHouseTheme {
        TemperatureContent2(
            TemperatureResponse(
                temperatura = 33.1,
                fecha = "2024-10-21T06:00:00.000Z",
                hora = "18:25:57",
                NombreDispositivo = "Patio"
            ),
            listOf(
                TemperatureAverageResponse("2024-10-21T00:00:00.000Z", 0, 26.0),
                TemperatureAverageResponse("2024-10-21T00:00:00.000Z", 1, 27.0),
                TemperatureAverageResponse("2024-10-21T00:00:00.000Z", 2, 28.0),
                TemperatureAverageResponse("2024-10-21T00:00:00.000Z", 3, 29.0),
                TemperatureAverageResponse("2024-10-21T00:00:00.000Z", 4, 30.0),
                TemperatureAverageResponse("2024-10-21T00:00:00.000Z", 5, 31.0)
            )
        )
    }
}