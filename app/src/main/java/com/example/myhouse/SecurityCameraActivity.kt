package com.example.myhouse

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

class SecurityCameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceId = intent.getIntExtra("DEVICE_ID", -1)
        val userId = getUserIdFromCache(this)?.toIntOrNull()

        // Iniciar la UI composable
        setContent {
            SecurityCameraScreen(deviceId, userId)
        }
    }
}

@Composable
fun SecurityCameraScreen(deviceId: Int, userId: Int?) {
    var cameraResponse by remember { mutableStateOf<CameraResponse?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val lifecycle = (context as ComponentActivity).lifecycle
    val imageQueue = remember { ConcurrentLinkedQueue<Bitmap>() }
    val idCounter = remember { IdCounter() }

    // Semaphore para limitar las corrutinas concurrentes a 4
    val semaphore = Semaphore(4)

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
                        // Ejecutar hasta 4 solicitudes en paralelo usando el semáforo
                        repeat(4) {
                            scope.launch {
                                semaphore.withPermit {
                                    val response = RetrofitClient.instance.getCameraResource(deviceId)
                                    Log.d("SecurityCameraScreen", "API Response: $response")
                                    cameraResponse = response

                                    // Decodificación de imagen a resolución más baja
                                    val decodedBitmap = decodeBase64ToBitmap(response.guardar_fotografia, 200, 200)
                                    imageQueue.add(decodedBitmap)

                                    // Handle ID counting logic
                                    idCounter.receiveId(response.ID_Dispositivo) { id ->
                                        // Handle the case when the same ID is received for 10 seconds
                                        Log.d("SecurityCameraScreen", "ID $id received for 10 seconds")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SecurityCameraScreen", "Error fetching camera resource", e)
                    }
                }
                delay(1000) // Espera de 1 segundo antes de la siguiente solicitud
            }
        }
    }

    // Tarea en segundo plano para procesar la cola de imágenes
    LaunchedEffect(deviceId, isPaused) {
        context.lifecycleScope.launch {
            while (true) {
                if (imageQueue.isNotEmpty()) {
                    bitmap = imageQueue.poll()
                }
                delay(50) // Actualización más rápida del bitmap
            }
        }
    }

    // Mostrar la interfaz de usuario basada en el estado actual de la cámara y las imágenes
    if (idCounter.isCameraInactive()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Cámara desconectada",
                color = Color.Red,
                textAlign = TextAlign.Center,
                fontSize = 24.sp
            )
            Image(
                painter = painterResource(id = R.drawable.desconectar), // Replace with your disconnected icon resource
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
        }
    } else {
        cameraResponse?.let { response ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = response.NombreDispositivo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                bitmap?.let {
                    Box(
                        modifier = Modifier
                            .size(400.dp)
                            .padding(16.dp)
                    ) {
                        DisplayImage(it)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val file = File(downloadsDir, "camera_image_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(file).use { out ->
                                bitmap?.compress(Bitmap.CompressFormat.JPEG, 70, out) // Compresión ajustada
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

fun decodeBase64ToBitmap(base64Image: String, targetWidth: Int, targetHeight: Int): Bitmap {
    val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, this)
        inSampleSize = calculateInSampleSize(this, targetWidth, targetHeight)
        inJustDecodeBounds = false
    }
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

@Composable
fun DisplayImage(bitmap: Bitmap) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.size(400.dp)
    )
}

class IdCounter {
    private var currentId: Int? = null
    private var startTime: Long = 0
    private val repeatCount = AtomicInteger(0)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun receiveId(newId: Int, onCountComplete: (Int) -> Unit) {
        val currentTime = System.currentTimeMillis()

        if (currentId == newId) {
            if (currentTime - startTime >= 10000) {
                onCountComplete(newId)
                resetCounter()
            }
        } else {
            resetCounter()
            currentId = newId
            startTime = currentTime
            scope.launch {
                while (true) {
                    delay(1000)
                    if (currentId == newId) {
                        repeatCount.incrementAndGet()
                        if (repeatCount.get() >= 10) {
                            onCountComplete(newId)
                            resetCounter()
                        }
                    } else {
                        resetCounter()
                        break
                    }
                }
            }
        }
    }

    fun isCameraInactive(): Boolean {
        return repeatCount.get() >= 10
    }

    private fun resetCounter() {
        currentId = null
        startTime = 0
        repeatCount.set(0)
    }
}