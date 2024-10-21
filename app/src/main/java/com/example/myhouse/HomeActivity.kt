package com.example.myhouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myhouse.ui.theme.MyHouseTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // Ensure content extends into system bars
        setContent {
            MyHouseTheme {
                BaseLayout(showFooter = true, currentPage = "Inicio") { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        HomeScreen(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFecf0f1)) // Set the background color
            .fillMaxSize()
    ) {
        Text(text = "Welcome to the Home Screen!")
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MyHouseTheme {
        HomeScreen()
    }
}