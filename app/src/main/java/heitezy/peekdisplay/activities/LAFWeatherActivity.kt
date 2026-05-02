package heitezy.peekdisplay.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.P
import androidx.core.content.edit
import heitezy.peekdisplay.ui.EditTextDialog
import heitezy.peekdisplay.ui.FormatDialog
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceDivider
import heitezy.peekdisplay.ui.PreferenceItem
import heitezy.peekdisplay.ui.SectionHeader
import heitezy.peekdisplay.ui.SwitchPreferenceItem
import heitezy.peekdisplay.ui.WeatherProviderDialog

class LAFWeatherActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                WeatherSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun WeatherSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { P.getPreferences(context) }

    var showPrivacyDialog by remember { mutableStateOf(true) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    var weatherEnabled by remember {
        mutableStateOf(prefs.getBoolean(P.SHOW_WEATHER, P.SHOW_WEATHER_DEFAULT))
    }
    var imperialEnabled by remember {
        mutableStateOf(prefs.getBoolean(P.WEATHER_IMPERIAL, P.WEATHER_IMPERIAL_DEFAULT))
    }
    var weatherLocation by remember {
        mutableStateOf(
            prefs.getString(P.WEATHER_LOCATION, P.WEATHER_LOCATION_DEFAULT)
                ?: P.WEATHER_LOCATION_DEFAULT
        )
    }
    var weatherFormat by remember {
        mutableStateOf(
            prefs.getString(P.WEATHER_FORMAT, P.WEATHER_FORMAT_DEFAULT) ?: P.WEATHER_FORMAT_DEFAULT
        )
    }

    if (showPrivacyDialog) {
        WeatherProviderDialog(
            onConfirm = { showPrivacyDialog = false },
            onDismiss = { onBack() },
            onLearnMore = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, "https://github.com/chubin/wttr.in".toUri())
                )
            }
        )
    }

    if (showFormatDialog) {
        FormatDialog(
            title = stringResource(R.string.pref_look_and_feel_weather_format),
            current = weatherFormat,
            onConfirm = { newFormat ->
                weatherFormat = newFormat
                prefs.edit { putString(P.WEATHER_FORMAT, newFormat) }
                showFormatDialog = false
            },
            onDismiss = { showFormatDialog = false },
            onMore = {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/chubin/wttr.in#one-line-output".toUri()
                    )
                )
            },
        )
    }

    if (showLocationDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_look_and_feel_weather_location),
            current = weatherLocation,
            onConfirm = { newLocation ->
                weatherLocation = newLocation
                prefs.edit { putString(P.WEATHER_LOCATION, newLocation) }
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false },
        )
    }

    PeekScaffold(
        title = stringResource(R.string.pref_look_and_feel_weather),
        onBack = onBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceItem(
                iconRes = R.drawable.ic_info,
                summary = stringResource(R.string.pref_look_and_feel_weather_provider),
            )

            PreferenceDivider()

            SectionHeader(text = stringResource(R.string.pref_look_and_feel_weather))

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_cloud,
                title = stringResource(R.string.pref_look_and_feel_weather_info),
                summary = stringResource(R.string.pref_look_and_feel_weather_show_summary),
                checked = weatherEnabled,
                onCheckedChange = { checked ->
                    weatherEnabled = checked
                    prefs.edit { putBoolean(P.SHOW_WEATHER, checked) }
                },
            )

            PreferenceDivider()

            SectionHeader(text = stringResource(R.string.pref_look_and_feel))

            PreferenceItem(
                iconRes = R.drawable.ic_location,
                title = stringResource(R.string.pref_look_and_feel_weather_location),
                summary = weatherLocation.ifBlank {
                    stringResource(R.string.pref_look_and_feel_weather_location_summary)
                },
                onClick = { showLocationDialog = true },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_thermostat,
                title = stringResource(R.string.pref_look_and_feel_weather_unit),
                summary = stringResource(R.string.pref_look_and_feel_weather_unit_summary),
                checked = imperialEnabled,
                onCheckedChange = { checked ->
                    imperialEnabled = checked
                    prefs.edit { putBoolean(P.WEATHER_IMPERIAL, checked) }
                },
            )

            PreferenceItem(
                iconRes = R.drawable.ic_short_text,
                title = stringResource(R.string.pref_look_and_feel_weather_format),
                summary = stringResource(R.string.pref_look_and_feel_weather_format_summary),
                onClick = { showFormatDialog = true },
            )
        }
    }
}