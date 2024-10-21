package com.example.myhouse

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun BaseLayout(showFooter: Boolean, currentPage: String, content: @Composable (PaddingValues) -> Unit) {
    val view = LocalView.current
    val insets = ViewCompat.getRootWindowInsets(view)
    val topInset = with(LocalDensity.current) { insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.top?.toDp() ?: 0.dp }
    val bottomInset = with(LocalDensity.current) { insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom?.toDp() ?: 0.dp }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(top = topInset, bottom = bottomInset)) {
        Box(modifier = Modifier.weight(1f)) {
            content(PaddingValues())
        }
        if (showFooter) {
            Footer(modifier = Modifier.fillMaxWidth(), currentPage = currentPage)
        }
    }
}