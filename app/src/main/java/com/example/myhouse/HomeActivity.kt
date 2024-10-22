package com.example.myhouse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.myhouse.ui.theme.MyHouseTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeViewModel : ViewModel() {
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices

    fun fetchDevices(userId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getDevices(userId)
                _devices.value = response
            } catch (e: Exception) {
                _devices.value = emptyList()
            }
        }
    }
}

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // Ensure content extends into system bars
        setContent {
            MyHouseTheme {
                BaseLayout(showFooter = true, currentPage = "Inicio") { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        HomeScreen(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel()
    val devices by viewModel.devices.collectAsState()

    LaunchedEffect(Unit) {
        val userId = getUserIdFromCache(context)?.toIntOrNull()
        if (userId != null) {
            viewModel.fetchDevices(userId)
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFFecf0f1)) // Set the background color
            .fillMaxSize()
            .padding(horizontal = 16.dp) // Add padding to avoid touching the edges
    ) {
        Text(
            text = "BIENVENIDO",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)
        )

        if (devices.isEmpty()) {
            Text(text = "Sin dispositivos enlazados", color = Color.Black, fontWeight = FontWeight.Bold)
        } else {
            ListSection(title = "Cámara de Seguridad", items = devices.filter { it.NombreTipo == "Cámara de seguridad" }, iconResId = R.drawable.ic_camera)
            Spacer(modifier = Modifier.height(16.dp))
            ListSection(title = "Control de Temperatura", items = devices.filter { it.NombreTipo == "Sensor de temperatura y humedad" }, iconResId = R.drawable.ic_temperature)
            Spacer(modifier = Modifier.height(16.dp))
            ListSection(title = "Calidad de Aire", items = devices.filter { it.NombreTipo == "Monitor de calidad de aire" }, iconResId = R.drawable.ic_air_quality)
        }
    }
}

@Composable
fun ListSection(title: String, items: List<Device>, iconResId: Int) {
    Column {
        Text(text = title, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        items.forEach { item ->
            ListItem(text = item.NombreDispositivo, iconResId = iconResId, deviceId = item.ID_Dispositivo, deviceType = item.NombreTipo)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ListItem(text: String, iconResId: Int, deviceId: Int, deviceType: String) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .padding(8.dp)
            .clickable {
                val intent = when (deviceType) {
                    "Cámara de seguridad" -> Intent(context, SecurityCameraActivity::class.java)
                    "Sensor de temperatura y humedad" -> Intent(context, TemperatureControlActivity::class.java)
                    "Monitor de calidad de aire" -> Intent(context, AirQualityActivity::class.java)
                    else -> null
                }
                intent?.putExtra("DEVICE_ID", deviceId)
                context.startActivity(intent)
            }
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = text,
            modifier = Modifier.size(35.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Color.Black)
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.ic_delete),
            contentDescription = "Eliminar dispositivo",
            modifier = Modifier
                .size(45.dp)
                .clickable {
                    val userId = getUserIdFromCache(context)?.toIntOrNull()
                    if (userId != null) {
                        com.example.myhouse.deleteDevice(context, userId, deviceId, onSuccess = {
                            viewModel.fetchDevices(userId) // Update the list after deletion
                        }, onError = { errorMessage ->
                            // Handle error
                        })
                    }
                }
        )
    }
}

fun deleteDevice(context: Context, userId: Int, deviceId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val jsonObject = JSONObject().apply {
        put("ID_Dispositivo", deviceId)
        put("ID_USER", userId)
    }
    val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

    RetrofitClient.instance.deleteDevice(requestBody).enqueue(object : Callback<DeleteResponse> {
        override fun onResponse(call: Call<DeleteResponse>, response: Response<DeleteResponse>) {
            if (response.isSuccessful) {
                onSuccess()
            } else {
                onError("Error: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<DeleteResponse>, t: Throwable) {
            onError("Network error: ${t.message}")
        }
    })
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MyHouseTheme {
        HomeScreen()
    }
}