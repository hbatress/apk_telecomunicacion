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
    var airQualityAverageData by remember { mutableStateOf<List<AirQualityAverageResponse>?>(null) }
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

            RetrofitClient.instance.getAirQualityAverage(userId).enqueue(object : Callback<List<AirQualityAverageResponse>> {
                override fun onResponse(call: Call<List<AirQualityAverageResponse>>, response: Response<List<AirQualityAverageResponse>>) {
                    if (response.isSuccessful) {
                        airQualityAverageData = response.body()
                    } else {
                        errorMessage = "No se encontraron datos de calidad de aire para este usuario"
                    }
                }

                override fun onFailure(call: Call<List<AirQualityAverageResponse>>, t: Throwable) {
                    Log.e("AirQualityScreen", "Error: ${t.message}")
                    errorMessage = "Error en la solicitud"
                }
            })

            delay(2000) // Delay for 5 seconds before making the next request
        }
    }

    if (airQualityData != null && airQualityAverageData != null) {
        AirQualityContent(airQualityData!!, airQualityAverageData!!)
    } else if (errorMessage != null) {
        Text(text = errorMessage!!)
    } else {
        Text(text = "Cargando datos...")
    }
}

@Composable
fun AirQualityContent(data: AirQualityResponse, averageData: List<AirQualityAverageResponse>) {
    val maxAirQuality = averageData.maxByOrNull { it.promedio_calidad_aire }?.promedio_calidad_aire ?: 0.0
    val yAxisMax = ((maxAirQuality + 20) / 20).toInt() * 20 // Round up to the nearest 20

    // Parse and format the date
    val parsedDate = ZonedDateTime.parse(data.fecha)
    val formattedDate = parsedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))

    // Determine the color based on the air quality index
    val boxColor = when (data.indice_calidad_aire) {
        in 0..50 -> Color(0xFF00E400) // Green
        in 51..100 -> Color(0xFFFFFF00) // Yellow
        in 101..150 -> Color(0xFFFF7E00) // Orange
        in 151..200 -> Color(0xFFFF0000) // Red
        in 201..300 -> Color(0xFF8F3F97) // Purple
        in 301..500 -> Color(0xFF7E0023) // Brown
        else -> Color.LightGray // Default color
    }

    // Custom function to determine if a color is light
    fun isColorLight(color: Color): Boolean {
        return color.luminance() > 0.5
    }

    // Determine the text color based on the background color
    val textColor = if (isColorLight(boxColor)) Color.Black else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Título: Nombre del dispositivo
        Text(text = data.NombreDispositivo, fontSize = 32.sp, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))

        // Gráfica de promedio de calidad de aire
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f).padding(top = 32.dp)) {
            val barWidth = size.width / 24
            val points = mutableListOf<Offset?>()

            for (i in 0..23) {
                val airQuality = averageData.find { it.hora == i }
                if (airQuality != null) {
                    points.add(
                        Offset(
                            i * barWidth,
                            size.height - (airQuality.promedio_calidad_aire.toFloat() / yAxisMax.toFloat() * size.height)
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
            for (i in 0..yAxisMax step 20) {
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

        // Índice de calidad de aire, fecha y hora en un rectángulo con bordes semiredondos
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(8.dp)
                .background(color = boxColor, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(text = "Fecha: $formattedDate", fontSize = 20.sp, color = textColor)
                    Text(text = "Hora: ${data.hora}", fontSize = 20.sp, color = textColor)
                }
                Text(
                    text = "${data.indice_calidad_aire}",
                    fontSize = 48.sp, // Larger font size
                    fontWeight = FontWeight.Bold, // Bold text
                    color = textColor,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
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
            ),
            listOf(
                AirQualityAverageResponse("2024-10-21T00:00:00.000Z", 0, 50.0),
                AirQualityAverageResponse("2024-10-21T00:00:00.000Z", 1, 60.0),
                AirQualityAverageResponse("2024-10-21T00:00:00.000Z", 2, 70.0),
                AirQualityAverageResponse("2024-10-21T00:00:00.000Z", 3, 80.0),
                AirQualityAverageResponse("2024-10-21T00:00:00.000Z", 4, 90.0),
                AirQualityAverageResponse("2024-10-21T00:00:00.000Z", 5, 100.0)
            )
        )
    }
}