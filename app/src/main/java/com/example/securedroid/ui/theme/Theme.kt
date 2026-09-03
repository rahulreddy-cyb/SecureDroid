package com.example.securedroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SecureDroidColorScheme = lightColorScheme(
    primary = Color(0xFF3F51B5),
    secondary = Color(0xFF5C6BC0),
    tertiary = Color(0xFF7986CB),

    background = Color.White,
    surface = Color.White,

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,

    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun SecureDroidTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SecureDroidColorScheme,
        content = content
    )
}