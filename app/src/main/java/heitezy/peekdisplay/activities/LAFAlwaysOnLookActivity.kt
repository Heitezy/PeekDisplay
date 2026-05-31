package heitezy.peekdisplay.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.ui.HorizontalLayoutPicker
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceDivider


private val THEMES = listOf(
    P.USER_THEME_MOTO        to R.drawable.always_on_moto,
    P.USER_THEME_GOOGLE      to R.drawable.always_on_google,
    P.USER_THEME_ONEPLUS     to R.drawable.always_on_oneplus,
    P.USER_THEME_SAMSUNG     to R.drawable.always_on_samsung,
    P.USER_THEME_SAMSUNG2    to R.drawable.always_on_samsung2,
    P.USER_THEME_SAMSUNG3    to R.drawable.always_on_samsung3,
    P.USER_THEME_80S         to R.drawable.always_on_80s,
    P.USER_THEME_FAST        to R.drawable.always_on_fast,
    P.USER_THEME_FLOWER      to R.drawable.always_on_flower,
    P.USER_THEME_GAME        to R.drawable.always_on_game,
    P.USER_THEME_HANDWRITTEN to R.drawable.always_on_handwritten,
    P.USER_THEME_JUNGLE      to R.drawable.always_on_jungle,
    P.USER_THEME_WESTERN     to R.drawable.always_on_western,
    P.USER_THEME_ANALOG      to R.drawable.always_on_analog,
)

private fun themeToIndex(theme: String) =
    THEMES.indexOfFirst { it.first == theme }.coerceAtLeast(0)

private fun indexToTheme(index: Int) =
    THEMES.getOrNull(index)?.first ?: P.USER_THEME_DEFAULT

class LAFAlwaysOnLookActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                AlwaysOnLookScreen(onBack = { finish() })
            }
        }
    }
}
@Composable
private fun AlwaysOnLookScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val labels = context.resources
        .getStringArray(R.array.pref_look_and_feel_ao_array_display)

    val prefs = remember { P.getP(context) }
    val savedTheme by prefs.getStringFlow(P.USER_THEME, P.USER_THEME_DEFAULT)
        .collectAsState(initial = prefs.getString(P.USER_THEME, P.USER_THEME_DEFAULT))

    var selectedIndex by remember { mutableIntStateOf(themeToIndex(savedTheme)) }

    LaunchedEffect(savedTheme) {
        selectedIndex = themeToIndex(savedTheme)
    }

    DisposableEffect(Unit) {
        onDispose {
            prefs.edit {
                putString(P.USER_THEME, indexToTheme(selectedIndex))
            }
        }
    }

    PeekScaffold(
        title = stringResource(R.string.pref_look_and_feel_ao),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Image(
                painter = painterResource(THEMES[selectedIndex].second),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            )

            PreferenceDivider()

            HorizontalLayoutPicker(
                items = THEMES.mapIndexed { i, (_, drawableRes) ->
                    drawableRes to labels.getOrElse(i) { "" }
                },
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    selectedIndex = index
                    AlwaysOn.finish()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}