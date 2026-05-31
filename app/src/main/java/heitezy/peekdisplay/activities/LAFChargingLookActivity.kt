package heitezy.peekdisplay.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceDivider
import heitezy.peekdisplay.ui.HorizontalLayoutPicker


private val STYLES = listOf(
    P.CHARGING_STYLE_CIRCLE to R.drawable.charging_circle,
    P.CHARGING_STYLE_FLASH  to R.drawable.charging_flash,
    P.CHARGING_STYLE_IOS    to R.drawable.charging_ios,
)

private fun styleToIndex(style: String) =
    STYLES.indexOfFirst { it.first == style }.coerceAtLeast(0)

private fun indexToStyle(index: Int) =
    STYLES.getOrNull(index)?.first ?: P.CHARGING_STYLE_DEFAULT

class LAFChargingLookActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                ChargingLookScreen(onBack = { finish() })
            }
        }
    }
}
@Composable
private fun ChargingLookScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val labels = context.resources
        .getStringArray(R.array.pref_look_and_feel_charging_array_display)

    val prefs = remember { P.getP(context) }
    val savedStyle by prefs.getStringFlow(P.CHARGING_STYLE, P.CHARGING_STYLE_DEFAULT)
        .collectAsState(initial = prefs.getString(P.CHARGING_STYLE, P.CHARGING_STYLE_DEFAULT))

    var selectedIndex by remember { mutableIntStateOf(styleToIndex(savedStyle)) }

    LaunchedEffect(savedStyle) {
        selectedIndex = styleToIndex(savedStyle)
    }

    DisposableEffect(Unit) {
        onDispose {
            prefs.edit {
                putString(P.CHARGING_STYLE, indexToStyle(selectedIndex))
            }
        }
    }

    PeekScaffold(
        title = stringResource(R.string.pref_look_and_feel_charging),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Image(
                painter = painterResource(STYLES[selectedIndex].second),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            )

            PreferenceDivider()

            HorizontalLayoutPicker(
                items = STYLES.mapIndexed { i, (_, drawableRes) ->
                    drawableRes to labels.getOrElse(i) { "" }
                },
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
