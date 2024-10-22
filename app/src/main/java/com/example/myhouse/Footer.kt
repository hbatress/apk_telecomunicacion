package com.example.myhouse

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun Footer(modifier: Modifier = Modifier, currentPage: String) {
    val view = LocalView.current
    val insets = ViewCompat.getRootWindowInsets(view)
    val bottomInset = with(LocalDensity.current) { insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom?.toDp() ?: 0.dp }
    val navigationBars = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())

    // Detecta si el dispositivo tiene botones de navegación en lugar de gestos
    val hasNavigationButtons = navigationBars?.bottom != 0

    // Define la altura del footer, ajustando cuando hay botones de navegación
    val footerHeight = if (hasNavigationButtons) 0.13f else 0.10f

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(footerHeight) // Ajusta la altura del footer dinámicamente
            .background(Color.White)
            .border(2.dp, Color.Gray, RoundedCornerShape(16.dp)) // Agrega borde con esquinas redondeadas
            .padding(bottom = bottomInset) // Solo aplica padding inferior para barras del sistema
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FooterItem(
                iconResId = R.drawable.ic_home,
                text = "Inicio",
                isSelected = currentPage == "Inicio",
                onClick = {
                    if (currentPage != "Inicio") {
                        context.startActivity(Intent(context, HomeActivity::class.java))
                    }
                },
                modifier = Modifier.weight(1f)
            )
            FooterItem(
                iconResId = R.drawable.ic_add,
                text = "Agregar",
                isSelected = currentPage == "Agregar",
                onClick = {
                    if (currentPage != "Agregar") {
                        context.startActivity(Intent(context, AddActivity::class.java))
                    }
                },
                modifier = Modifier.weight(1f)
            )
            FooterItem(
                iconResId = R.drawable.ic_settings,
                text = "Ajustes",
                isSelected = currentPage == "Ajustes",
                onClick = {
                    if (currentPage != "Ajustes") {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FooterItem(iconResId: Int, text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center, // Centrar el contenido verticalmente
        modifier = modifier
            .fillMaxHeight() // Asegura que la columna llene la altura disponible
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = text,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            color = if (isSelected) Color.Cyan else Color.Blue,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
