package heitezy.peekdisplay.ui


import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import heitezy.peekdisplay.R


@Composable
fun FormatDialog(
    title: String,
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onMore: () -> Unit,
    validate: ((String) -> Boolean)? = null
) {
    var text by remember { mutableStateOf(current) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = false
                    },
                    isError = error,
                    singleLine = true,
                    label = { Text(stringResource(R.string.pref_ao_date_format_dialog_text)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error) {
                    Text(
                        text = stringResource(R.string.pref_ao_date_format_illegal),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onMore,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(stringResource(R.string.pref_ao_date_format_dialog_neutral))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(onClick = {
                    val isValid = validate?.invoke(text) ?: true

                    if (isValid) {
                        onConfirm(text)
                    } else {
                        error = true
                    }
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun PermissionDialog(
    @DrawableRes iconRes: Int,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
fun DeviceAdminDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.device_admin)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.dialog_device_admin_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.dialog_device_admin_result)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Unspecified,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Unspecified,
                        platformImeOptions = null,
                        showKeyboardOnFocus = null,
                        hintLocales = null
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
fun EditTextDialog(
    title: String,
    current: String,
    label: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    validate: ((String) -> Boolean)? = null,
) {
    var text by remember { mutableStateOf(current) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = false
                },
                isError = error,
                singleLine = true,
                label = label?.let { l -> { Text(l) } },
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = keyboardType
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (validate?.invoke(text) != false) onConfirm(text)
                else error = true
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
fun RadioButtonDialog(
    title: String,
    entries: List<String>,
    entryValues: List<String>,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf(selectedValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                entries.forEachIndexed { index, label ->
                    val value = entryValues[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = current == value,
                                onClick = { current = value },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = current == value, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onValueSelected(current)
                onDismiss()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    showAlpha: Boolean,
    title: String,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentColor by remember { mutableIntStateOf(initialColor) }
    var isCustomMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isCustomMode) {
                    CustomHSVPicker(
                        color = Color(currentColor),
                        onColorChanged = { currentColor = it.toArgb() }
                    )

                    // Hex Input and Preview
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier
                            .size(60.dp, 30.dp)
                            .background(Color(initialColor))
                            .border(1.dp, Color.Black))
                        Text("→")
                        Box(modifier = Modifier
                            .size(60.dp, 30.dp)
                            .background(Color(currentColor))
                            .border(1.dp, Color.Black))
                    }

                    var hexText by remember { mutableStateOf(String.format("%08X", currentColor)) }
                    val hexFromCurrent = String.format("%08X", currentColor)
                    try {
                        if (parseColorString(hexText) != currentColor) hexText = hexFromCurrent
                    } catch (_: Exception) {
                        hexText = hexFromCurrent
                    }

                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }
                            hexText = filtered.take(8)
                            try {
                                currentColor = parseColorString(hexText)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.wrapContentWidth(),
                        label = { Text("Hex " + stringResource(R.string.colorpicker_color)) },
                        prefix = { Text("#") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(currentColor))
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )

                    if (showAlpha) {
                        Column {
                            Text(
                                text = stringResource(R.string.colorpicker_opacity)
                                        + ": ${(Color(currentColor).alpha * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = Color(currentColor).alpha,
                                onValueChange = { newAlpha ->
                                    currentColor = Color(currentColor)
                                        .copy(alpha = newAlpha)
                                        .toArgb()
                                },
                                valueRange = 0f..1f
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presetColors = listOf(
                            0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(),
                            0xFF673AB7.toInt(), 0xFF3F51B5.toInt(), 0xFF2196F3.toInt(),
                            0xFF03A9F4.toInt(), 0xFF00BCD4.toInt(), 0xFF009688.toInt(),
                            0xFF4CAF50.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt()
                        )

                        presetColors.forEach { colorInt ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(
                                        width = if (currentColor == colorInt) 3.dp else 1.dp,
                                        color = if (currentColor == colorInt)
                                            MaterialTheme.colorScheme.primary
                                        else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { currentColor = colorInt }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { isCustomMode = !isCustomMode },
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        if (isCustomMode) stringResource(R.string.colorpicker_presets) else stringResource(
                            R.string.colorpicker_custom
                        )
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(onClick = { onColorSelected(currentColor) }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun WeatherProviderDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onLearnMore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_privacy)) },
        text  = { Text(stringResource(R.string.pref_look_and_feel_weather_provider)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onLearnMore) {
                    Text(stringResource(R.string.pref_look_and_feel_weather_provider_name))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        },
    )
}

@Composable
fun MultiSelectDialog(
    title: String,
    entries: List<String>,
    entryValues: List<String>,
    selectedValues: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf(selectedValues) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                entries.forEachIndexed { index, label ->
                    val value = entryValues[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = value in current,
                            onCheckedChange = { checked ->
                                current = if (checked) current + value else current - value
                            },
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current); onDismiss() }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun AdvancedTimePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                toggle()
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainedTimePickerDialog(
    startTime: String,
    endTime: String,
    is24Hour: Boolean,
    onConfirm: (start: String, end: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(TimePickerStep.START) }
    var pickedStart by rememberSaveable { mutableStateOf(startTime) }
    var showDial by rememberSaveable { mutableStateOf(true) }

    val startState = rememberTimePickerState(
        initialHour = parseHour(startTime),
        initialMinute = parseMinute(startTime),
        is24Hour = is24Hour,
    )
    val endState = rememberTimePickerState(
        initialHour = parseHour(endTime),
        initialMinute = parseMinute(endTime),
        is24Hour = is24Hour,
    )

    if (step == TimePickerStep.NONE) return

    val title = if (step == TimePickerStep.START)
        stringResource(R.string.pref_look_and_feel_rules_time)
    else
        stringResource(R.string.pref_look_and_feel_rules_time)

    val activeState = if (step == TimePickerStep.START) startState else endState

    AdvancedTimePickerDialog(
        title = title,
        onDismiss = {
            if (step == TimePickerStep.END) step = TimePickerStep.START
            else onDismiss()
        },
        onConfirm = {
            if (step == TimePickerStep.START) {
                pickedStart = formatTime(startState.hour, startState.minute)
                step = TimePickerStep.END
            } else {
                onConfirm(pickedStart, formatTime(endState.hour, endState.minute))
                step = TimePickerStep.NONE
            }
        },
        toggle = {
            IconButton(onClick = { showDial = !showDial }) {
                Icon(
                    painter = painterResource(id = if (showDial) R.drawable.ic_date else R.drawable.ic_clock),
                    contentDescription = null
                )
            }
        },
    ) {
        if (showDial) {
            TimePicker(state = activeState)
        } else {
            TimeInput(state = activeState)
        }
    }
}

private enum class TimePickerStep { START, END, NONE }

private fun formatTime(hours: Int, minutes: Int): String =
    "$hours:${minutes.toString().padStart(2, '0')}"

private fun parseHour(time: String): Int = time.substringBefore(":").toIntOrNull() ?: 0
private fun parseMinute(time: String): Int = time.substringAfter(":").toIntOrNull() ?: 0

private fun parseColorString(colorString: String): Int {
    val s = colorString.removePrefix("#")
    val a: Int; val r: Int; val g: Int; val b: Int
    when (s.length) {
        0    -> { a = 255; r = 0;    g = 0;                 b = 0 }
        1, 2 -> { a = 255; r = 0;    g = 0;                 b = s.toInt(16) }
        3    -> { a = 255; r = s.substring(0, 1).toInt(16); g = s.substring(1, 2).toInt(16); b = s.substring(2, 3).toInt(16) }
        4    -> { a = 255; r = 0;    g = s.substring(0, 2).toInt(16);                        b = s.substring(2, 4).toInt(16) }
        5    -> { a = 255; r = s.substring(0, 1).toInt(16); g = s.substring(1, 3).toInt(16); b = s.substring(3, 5).toInt(16) }
        6    -> { a = 255; r = s.substring(0, 2).toInt(16); g = s.substring(2, 4).toInt(16); b = s.substring(4, 6).toInt(16) }
        7    -> { a = s.substring(0, 1).toInt(16);           r = s.substring(1, 3).toInt(16); g = s.substring(3, 5).toInt(16); b = s.substring(5, 7).toInt(16) }
        8    -> { a = s.substring(0, 2).toInt(16);           r = s.substring(2, 4).toInt(16); g = s.substring(4, 6).toInt(16); b = s.substring(6, 8).toInt(16) }
        else -> throw NumberFormatException("Invalid hex color length: ${s.length}")
    }
    return android.graphics.Color.argb(a, r, g, b)
}