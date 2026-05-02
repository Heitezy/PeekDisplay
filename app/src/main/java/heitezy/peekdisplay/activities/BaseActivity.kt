package heitezy.peekdisplay.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.elevation.SurfaceColors

abstract class BaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val color = SurfaceColors.SURFACE_2.getColor(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = color,
                darkScrim = color,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = color,
                darkScrim = color,
            )
        )
    }

    @Composable
    fun BaseContent(content: @Composable () -> Unit) {
        val context = LocalContext.current
        val darkTheme = isSystemInDarkTheme()
        val colorScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(
                context
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
            darkTheme -> darkColorScheme()
            else -> lightColorScheme()
        }
        MaterialTheme(colorScheme = colorScheme) {
            content()
        }
    }
}