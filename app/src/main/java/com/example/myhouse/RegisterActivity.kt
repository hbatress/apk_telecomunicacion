package com.example.myhouse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myhouse.ui.theme.MyHouseTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyHouseTheme {
                BaseLayout(showFooter = false, currentPage = "Register") { innerPadding ->
                    RegisterScreen(
                        modifier = Modifier.padding(innerPadding),
                        onRegisterSuccess = {
                            val intent = Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(modifier: Modifier = Modifier, onRegisterSuccess: () -> Unit) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }
    val confirmPasswordVisible = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Correo") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Contraseña") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                    Icon(imageVector = image, contentDescription = if (passwordVisible.value) "Ocultar contraseña" else "Mostrar contraseña")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        OutlinedTextField(
            value = confirmPassword.value,
            onValueChange = { confirmPassword.value = it },
            label = { Text("Confirmar Contraseña") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            visualTransformation = if (confirmPasswordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (confirmPasswordVisible.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible.value = !confirmPasswordVisible.value }) {
                    Icon(imageVector = image, contentDescription = if (confirmPasswordVisible.value) "Ocultar contraseña" else "Mostrar contraseña")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Button(
            onClick = {
                if (email.value.isNotEmpty() && password.value.isNotEmpty() && confirmPassword.value.isNotEmpty()) {
                    if (isValidEmail(email.value)) {
                        if (password.value == confirmPassword.value) {
                            val request = RegisterRequest(email.value, password.value)
                            RetrofitClient.instance.register(request).enqueue(object : Callback<RegisterResponse> {
                                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                                    Log.d("RegisterRequest", "Request: ${call.request().url()}")
                                    Log.d("RegisterResponse", "Response: ${response.body()}")
                                    Log.d("RegisterResponse", "Response Code: ${response.code()}")
                                    Log.d("RegisterResponse", "Response Message: ${response.message()}")
                                    if (response.isSuccessful) {
                                        val message = response.body()?.message
                                        val id = response.body()?.id
                                        if (message == "Usuario creado correctamente" && id != null) {
                                            saveUserIdToCache(context, id.toString())
                                            Toast.makeText(context, "Cuenta creada", Toast.LENGTH_SHORT).show()
                                            onRegisterSuccess()
                                        } else {
                                            Toast.makeText(context, message ?: "Error desconocido", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, response.body()?.message ?: "Error al crear la cuenta", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                                    Log.e("RegisterRequest", "Request failed: ${t.message}")
                                    Toast.makeText(context, "Error en la solicitud", Toast.LENGTH_SHORT).show()
                                }
                            })
                        } else {
                            Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Por favor, ingrese un correo electrónico válido", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(text = "Registrar", color = Color.White)
        }
    }
}