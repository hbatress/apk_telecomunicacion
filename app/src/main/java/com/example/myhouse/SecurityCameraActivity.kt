package com.example.myhouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myhouse.ui.theme.MyHouseTheme

class SecurityCameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceId = intent.getIntExtra("DEVICE_ID", -1)
        setContent {
            MyHouseTheme {
                SecurityCameraScreen(deviceId)
            }
        }
    }
}

@Composable
fun SecurityCameraScreen(deviceId: Int) {
    Text(text = "Security Camera ID: $deviceId")
}

@Preview(showBackground = true)
@Composable
fun SecurityCameraScreenPreview() {
    MyHouseTheme {
        SecurityCameraScreen(deviceId = 1)
    }
}