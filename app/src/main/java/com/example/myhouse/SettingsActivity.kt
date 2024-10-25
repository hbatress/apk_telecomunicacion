package com.example.myhouse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.myhouse.ui.theme.MyHouseTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MyHouseTheme {
                BaseLayout(showFooter = true, currentPage = "Ajustes") { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        SettingsScreen(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Manejar el botón de retroceso usando OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@SettingsActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userInfo = remember { mutableStateOf<UserInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userId = getUserIdFromCache(context)?.toIntOrNull()
            if (userId != null) {
                userInfo.value = RetrofitClient.instance.getUserInfo(userId)
            }
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFFecf0f1))
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Primera fila con el contenido existente
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                val userIcon: Painter = painterResource(id = R.drawable.ic_user) // Asegúrate de que este drawable exista
                Image(
                    painter = userIcon,
                    contentDescription = "User Icon",
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                userInfo.value?.let { user ->
                    Text(text = "Correo: ${user.correo}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Contraseña: ${user.contrasena}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Cantidad de dispositivos: ${user.cantidad_dispositivos}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                } ?: run {
                    Text(text = "Cargando información del usuario...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Limpiar caché y datos del usuario
                        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().clear().apply()
                        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                        // Redirigir a la página de inicio de sesión
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "Cerrar Sesión", color = Color.White)
                }
            }
        }
    }
}