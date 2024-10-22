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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myhouse.ui.theme.MyHouseTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyHouseTheme {
                BaseLayout(showFooter = false, currentPage = "Login") { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLoginSuccess = {
                            startActivity(Intent(this, HomeActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier, onLoginSuccess: () -> Unit) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
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
                .padding(bottom = 16.dp),
            visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                    Icon(imageVector = image, contentDescription = if (passwordVisible.value) "Ocultar contraseña" else "Mostrar contraseña")
                }
            }
        )
        Button(
            onClick = {
                if (email.value.isNotEmpty() && password.value.isNotEmpty()) {
                    if (isValidEmail(email.value)) {
                        val request = LoginRequest(email.value, password.value)
                        RetrofitClient.instance.login(request).enqueue(object : Callback<LoginResponse> {
                            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {

                                Log.d("LoginResponse", "Response: ${response.body()}")
                                Log.d("LoginResponse", "Response Code: ${response.code()}")
                                Log.d("LoginResponse", "Response Message: ${response.message()}")
                                if (response.isSuccessful) {
                                    val message = response.body()?.message
                                    val id = response.body()?.id
                                    if (message == "Usuario correcto" && id != null) {
                                        saveUserIdToCache(context, id.toString())
                                        Toast.makeText(context, "Verificado", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        Toast.makeText(context, message ?: "Error desconocido", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, response.body()?.message ?: "usuario o contraseña incorrecto", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                Log.e("LoginRequest", "Request failed: ${t.message}")
                                Toast.makeText(context, "Error en la solicitud", Toast.LENGTH_SHORT).show()
                            }
                        })
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
            Text(text = "Ingresar", color = Color.White)
        }
        Button(
            onClick = {
                context.startActivity(Intent(context, RegisterActivity::class.java))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Crear cuenta", color = Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MyHouseTheme {
        LoginScreen(onLoginSuccess = {})
    }
}