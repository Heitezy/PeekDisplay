package heitezy.peekdisplay.activities

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.SwitchPreferenceItem

class LAFBrightnessActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                BrightnessScreen(
                    onBack = {
                        AlwaysOn.finish()
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun BrightnessScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val window = activity?.window
    val prefs = remember { P.getP(context) }

    val forceBrightnessFlow by prefs.getBooleanFlow(P.FORCE_BRIGHTNESS, P.FORCE_BRIGHTNESS_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.FORCE_BRIGHTNESS, P.FORCE_BRIGHTNESS_DEFAULT))
    
    val brightnessValueFlow by prefs.getIntFlow(P.FORCE_BRIGHTNESS_VALUE, P.FORCE_BRIGHTNESS_VALUE_DEFAULT)
        .collectAsState(initial = prefs.getInt(P.FORCE_BRIGHTNESS_VALUE, P.FORCE_BRIGHTNESS_VALUE_DEFAULT))

    var forceBrightness by remember { mutableStateOf(forceBrightnessFlow) }
    var brightnessValue by remember { mutableFloatStateOf(brightnessValueFlow.toFloat()) }

    LaunchedEffect(forceBrightnessFlow, brightnessValueFlow) {
        forceBrightness = forceBrightnessFlow
        brightnessValue = brightnessValueFlow.toFloat()
    }

    DisposableEffect(Unit) {
        onDispose {
            prefs.edit {
                putBoolean(P.FORCE_BRIGHTNESS, forceBrightness)
                    .putInt(P.FORCE_BRIGHTNESS_VALUE, brightnessValue.toInt())
            }
        }
    }

    PeekScaffold(
        title = stringResource(R.string.pref_ao_force_brightness_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.pref_ao_force_brightness_example),
                    modifier = Modifier.padding(16.dp),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            SwitchPreferenceItem(
                title = stringResource(R.string.pref_ao_force_brightness),
                checked = forceBrightness,
                onCheckedChange = { forceBrightness = it },
            )

            Slider(
                value = brightnessValue,
                onValueChange = {
                    brightnessValue = it
                    val attributes = window?.attributes
                    attributes?.screenBrightness = (brightnessValue / 100)
                    window?.attributes = attributes
                },
                valueRange = 0f..100f,
                steps = 0,
                enabled = forceBrightness,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 0.dp, 16.dp, 16.dp),
            )
        }
    }
}
