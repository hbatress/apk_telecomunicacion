package com.example.myhouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myhouse.ui.theme.MyHouseTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyHouseTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Text(text = "Welcome to the Home Screen!")
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MyHouseTheme {
        HomeScreen()
    }
}