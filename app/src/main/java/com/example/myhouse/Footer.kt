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
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.10f) // Ensure the Column occupies 10% of the height
            .background(Color.White)
            .border(2.dp, Color.Gray, RoundedCornerShape(16.dp)) // Add border with rounded corners
            .padding(bottom = bottomInset) // Only apply bottom padding for system bars
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
                }
            )
            FooterItem(
                iconResId = R.drawable.ic_add,
                text = "Agregar",
                isSelected = currentPage == "Agregar",
                onClick = {
                    if (currentPage != "Agregar") {
                        context.startActivity(Intent(context, AddActivity::class.java))
                    }
                }
            )
            FooterItem(
                iconResId = R.drawable.ic_settings,
                text = "Ajustes",
                isSelected = currentPage == "Ajustes",
                onClick = {
                    if (currentPage != "Ajustes") {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                }
            )
        }
    }
}

@Composable
fun FooterItem(iconResId: Int, text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center, // Center content vertically
        modifier = Modifier
            .fillMaxHeight() // Ensure the Column fills the available height
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