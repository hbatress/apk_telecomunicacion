package com.example.myhouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.example.myhouse.ui.theme.MyHouseTheme

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
    Box(
        modifier = modifier
            .background(Color(0xFFecf0f1))
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Text(text = "Welcome to the Add Screen!")
    }
}

@Preview(showBackground = true)
@Composable
fun AddScreenPreview() {
    MyHouseTheme {
        AddScreen()
    }
}