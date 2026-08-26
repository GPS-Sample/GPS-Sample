package edu.gtri.gpssample.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import edu.gtri.gpssample.R

private val WorkSans = FontFamily(
    Font(
        resId = R.font.work_sans_medium,
        weight = FontWeight.Medium
    )
)

private val GPSSampleTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Medium
    ),
    bodySmall = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Medium
    ),
    titleLarge = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Medium
    ),
    labelLarge = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Medium
    )
)

@Composable
fun GPSSampleComposeTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()

    val colors = if (darkTheme) {
        darkColorScheme(
            primary = colorResource(R.color.primary_high),
            onPrimary = colorResource(R.color.white),

            secondary = colorResource(R.color.secondary),
            onSecondary = colorResource(R.color.black),

            background = colorResource(R.color.background),
            onBackground = colorResource(R.color.white),

            surface = colorResource(R.color.surface_dark),
            onSurface = colorResource(R.color.white)
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.primary_low),
            onPrimary = colorResource(R.color.white),

            secondary = colorResource(R.color.secondary),
            onSecondary = colorResource(R.color.black),

            background = colorResource(R.color.background),
            onBackground = colorResource(R.color.primary_textcolor),

            surface = colorResource(R.color.white),
            onSurface = colorResource(R.color.primary_textcolor)
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = GPSSampleTypography,
        content = content
    )
}