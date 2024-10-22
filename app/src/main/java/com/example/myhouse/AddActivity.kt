package com.example.myhouse

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.myhouse.ui.theme.MyHouseTheme
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // Ensure content extends into system bars
        setContent {
            MyHouseTheme {
                BaseLayout(showFooter = true, currentPage = "Agregar") { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        AddScreen(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AddScreen(modifier: Modifier = Modifier) {
    var deviceName by remember { mutableStateOf("") }
    var devicePassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val userId = getUserIdFromCache(context)?.toIntOrNull()

    Box(
        modifier = modifier
            .background(Color(0xFFecf0f1))
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AGREGAR NUEVOS DISPOSITIVOS",
                fontSize = 24.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            val image: Painter = painterResource(id = R.drawable.agregar) // Replace with your image resource
            Image(
                painter = image,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("Nombre del dispositivo") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = devicePassword,
                onValueChange = { devicePassword = it },
                label = { Text("Contraseña del dispositivo") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña")
                    }
                }
            )
            Button(
                onClick = {
                    if (deviceName.isNotEmpty() && devicePassword.isNotEmpty() && userId != null) {
                        val request = AddDeviceRequest(userId, deviceName, devicePassword)
                        RetrofitClient.instance.addDevice(request).enqueue(object : Callback<AddDeviceResponse> {
                            override fun onResponse(call: Call<AddDeviceResponse>, response: Response<AddDeviceResponse>) {
                                if (response.isSuccessful) {
                                    val message = response.body()?.message
                                    Log.d("AddDeviceResponse", "Response: $message")
                                    when (message) {
                                        "Dispositivo agregado correctamente" -> {
                                            Toast.makeText(context, "Dispositivo agregado correctamente", Toast.LENGTH_SHORT).show()
                                            deviceName = ""
                                            devicePassword = ""
                                        }
                                        else -> {
                                            Toast.makeText(context, "Error desconocido", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    Log.d("AddDeviceResponse", "Error Body: $errorBody")
                                    if (response.code() == 400 && errorBody?.contains("El dispositivo ya está registrado para este usuario") == true) {
                                        Toast.makeText(context, "El dispositivo ya está registrado para este usuario", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error en la respuesta del servidor", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            override fun onFailure(call: Call<AddDeviceResponse>, t: Throwable) {
                                Log.d("AddDeviceResponse", "Error: ${t.message}")
                                Toast.makeText(context, "Error en la solicitud", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(context, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Agregar Dispositivo", color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddScreenPreview() {
    MyHouseTheme {
        AddScreen()
    }
}