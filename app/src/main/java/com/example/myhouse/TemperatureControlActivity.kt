package com.example.myhouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myhouse.ui.theme.MyHouseTheme

class TemperatureControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceId = intent.getIntExtra("DEVICE_ID", -1)
        val userId = getUserIdFromCache(this)?.toIntOrNull()
        setContent {
            MyHouseTheme {
                TemperatureControlScreen(deviceId, userId)
            }
        }
    }
}

@Composable
fun TemperatureControlScreen(deviceId: Int, userId: Int?) {
    Text(text = "Temperature Control ID: $deviceId, User ID: $userId")
}

@Preview(showBackground = true)
@Composable
fun TemperatureControlScreenPreview() {
    MyHouseTheme {
        TemperatureControlScreen(deviceId = 1, userId = 123)
    }
}