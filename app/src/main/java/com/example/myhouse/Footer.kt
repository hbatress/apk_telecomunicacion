package com.example.myhouse

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun Footer(modifier: Modifier = Modifier) {
    val view = LocalView.current
    val insets = ViewCompat.getRootWindowInsets(view)
    val bottomInset = with(LocalDensity.current) { insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom?.toDp() ?: 0.dp }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomInset + 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FooterItem(iconResId = R.drawable.ic_home, text = "Inicio")
        FooterItem(iconResId = R.drawable.ic_add, text = "Agregar")
        FooterItem(iconResId = R.drawable.ic_settings, text = "Ajustes")
    }
}

@Composable
fun FooterItem(iconResId: Int, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(id = iconResId), contentDescription = text)
        Text(text = text)
    }
}