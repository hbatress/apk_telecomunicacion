package com.example.myhouse

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import retrofit2.Response

class SecurityCameraActivity : ComponentActivity() {
    private val apiService = RetrofitClient.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceId = intent.getIntExtra("DEVICE_ID", -1)
        val deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Camara de Seguridad"
        val userId = getUserIdFromCache(this)?.toIntOrNull()

        // Check camera status before entering the screen
        checkCameraStatus(deviceId)

        setContent {
            SecurityCameraScreen(deviceId, deviceName, userId, apiService)
        }
    }

    private fun checkCameraStatus(deviceId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response<CameraStatusResponse> = withContext(Dispatchers.IO) { apiService.getCameraStatus(deviceId).execute() }
                if (response.isSuccessful) {
                    val status = response.body()?.estado
                    if (status == "encendida") {
                        Log.d("SecurityCameraActivity", "Camera is on")
                    } else {
                        Log.d("SecurityCameraActivity", "Camera is off")
                    }
                } else {
                    Log.e("SecurityCameraActivity", "Error checking camera status: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("SecurityCameraActivity", "Error checking camera status", e)
            }
        }
    }
}

@Composable
fun SecurityCameraScreen(deviceId: Int, deviceName: String, userId: Int?, apiService: ApiService) {
    var cameraResponse by remember { mutableStateOf<ImageResponse?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cameraStatus by remember { mutableStateOf("encendida") }
    var currentDateTime by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycle = (context as ComponentActivity).lifecycle

    DisposableEffect(deviceId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isPaused = false
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_DESTROY -> isPaused = true
                else -> {}
            }
        }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(deviceId, isPaused) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            while (true) {
                if (!isPaused) {
                    try {
                        val response: Response<ImageResponse> = withContext(Dispatchers.IO) { apiService.getImage(ImageRequest(userId ?: 1, deviceId)).execute() }
                        if (response.isSuccessful) {
                            cameraResponse = response.body()
                            cameraResponse?.let {
                                withContext(Dispatchers.Default) {
                                    bitmap = decodeBase64ToBitmap(it.image, 200, 200)
                                }
                            }
                        } else {
                            Log.e("SecurityCameraScreen", "Error fetching image: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e("SecurityCameraScreen", "Error fetching image", e)
                    }
                }
                delay(500) // Adjusted delay to 500 milliseconds
            }
        }

        // Check camera status every 3 seconds
        scope.launch {
            while (true) {
                try {
                    val response: Response<CameraStatusResponse> = withContext(Dispatchers.IO) { apiService.getCameraStatus(deviceId).execute() }
                    if (response.isSuccessful) {
                        val status = response.body()?.estado
                        if (status == "encendida") {
                            cameraStatus = "encendida"
                            Log.d("SecurityCameraScreen", "Camera is on")
                        } else {
                            cameraStatus = "desconectada"
                            Log.d("SecurityCameraScreen", "Camera is off")
                        }
                    } else {
                        cameraStatus = "desconectada"
                        Log.e("SecurityCameraScreen", "Error checking camera status: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    cameraStatus = "desconectada"
                    Log.e("SecurityCameraScreen", "Error checking camera status", e)
                }
                delay(3000) // Check every 3 seconds
            }
        }

        // Update date and time every second
        scope.launch {
            while (true) {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                currentDateTime = sdf.format(Date())
                delay(1000)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = deviceName,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White, // Text color
            modifier = Modifier
                .padding(bottom = 16.dp)
                .background(Color(0xFF6200EE)) // Background color
                .border(2.dp, Color(0xFF3700B3), RoundedCornerShape(8.dp)) // Border
                .padding(8.dp) // Padding inside the border
        )

        if (cameraStatus == "desconectada") {
            Image(
                painter = painterResource(id = R.drawable.desconectar), // Replace with your actual icon resource
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cámara desconectada o fuera de servicio",
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        } else {
            cameraResponse?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    bitmap?.let {
                        Box(
                            modifier = Modifier
                                .size(500.dp) // Increased size
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp)) // Apply rounded corners
                        ) {
                            DisplayImage(it)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFBBDEFB)), // Light blue background
                        modifier = Modifier
                            .padding(8.dp)
                            .border(2.dp, Color.Blue, RoundedCornerShape(8.dp)) // Border
                    ) {
                        Text(
                            text = "Fecha: Hora: $currentDateTime",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Blue,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val file = File(downloadsDir, "camera_image_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(file).use { out ->
                                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 70, out)
                                }
                                Toast.makeText(context, "Imagen guardada", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(8.dp)
                                .weight(1f)
                        ) {
                            Text(text = "Guardar Imagen", color = Color.White)
                        }

                        Button(
                            onClick = {
                                isPaused = !isPaused
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPaused) Color.Green else Color.Red),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(8.dp)
                                .weight(1f)
                        ) {
                            Text(text = if (isPaused) "Encender" else "Apagar", color = Color.White)
                        }
                    }
                }
            } ?: run {
                Text(text = "Cargando...", modifier = Modifier.fillMaxSize(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun DisplayImage(bitmap: Bitmap) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp)) // Apply semi-rounded corners
    )
}

fun decodeBase64ToBitmap(base64Str: String, width: Int, height: Int): Bitmap {
    val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}