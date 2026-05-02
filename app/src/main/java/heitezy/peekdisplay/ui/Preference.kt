package heitezy.peekdisplay.ui


import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp


@Composable
fun PreferenceItem(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    title: String? = null,
    summary: String? = null,
    enabled: Boolean = true,
    hasPermission: Boolean = true,
    permissionDeniedSummary: String? = null,
    onPermissionRequired: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    widget: @Composable (() -> Unit)? = null,
) {
    val isEnabled = enabled && hasPermission

    val finalSummary: String? = if (!hasPermission) {
        permissionDeniedSummary ?: summary
    } else {
        summary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null || !hasPermission)
                    Modifier.clickable(
                        enabled = enabled,
                        onClick = {
                            if (!hasPermission) onPermissionRequired?.invoke()
                            else onClick?.invoke()
                        }
                    )
                else
                    Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isEnabled)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            )
            Spacer(modifier = Modifier.width(32.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
            finalSummary?.let { textValue ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = textValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )
            }
        }
        if (widget != null) {
            Spacer(modifier = Modifier.width(16.dp))
            widget()
        }
    }
}

@Composable
fun SwitchPreferenceItem(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    summaryOn: String? = null,
    summaryOff: String? = null,
    enabled: Boolean = true,
    hasPermission: Boolean = true,
    permissionDeniedSummary: String? = null,
    onPermissionRequired: (() -> Unit)? = null,
) {
    val isEnabled = enabled && hasPermission

    val resolvedSummary = when {
        !hasPermission -> permissionDeniedSummary ?: summary
        checked        -> summaryOn ?: summary
        else           -> summaryOff ?: summary
    }

    PreferenceItem(
        modifier = modifier,
        iconRes = iconRes,
        title = title,
        summary = resolvedSummary,
        enabled = isEnabled,
        hasPermission = hasPermission,
        permissionDeniedSummary = permissionDeniedSummary,
        onPermissionRequired = onPermissionRequired,
        onClick = { onCheckedChange(!checked) },
        widget = {
            Switch(
                checked = checked,
                onCheckedChange = {
                    if (hasPermission) onCheckedChange(it)
                    else onPermissionRequired?.invoke()
                },
                enabled = enabled && hasPermission,
            )
        },
    )
}

@Composable
fun SeekBarPreferenceItem(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    summaryProvider: @Composable (Int) -> String,
    enabled: Boolean = true,
    hasPermission: Boolean = true,
    permissionDeniedSummary: String? = null,
    onPermissionRequired: (() -> Unit)? = null,
) {
    var sliderPosition by remember(value) { mutableFloatStateOf(value.toFloat()) }

    Column(modifier = modifier.fillMaxWidth()) {
        PreferenceItem(
            iconRes = iconRes,
            title = title,
            summary = summaryProvider(sliderPosition.toInt()),
            enabled = enabled,
            hasPermission = hasPermission,
            permissionDeniedSummary = permissionDeniedSummary,
            onPermissionRequired = onPermissionRequired,
        )
        Slider(
            value = sliderPosition,
            onValueChange = { if(hasPermission) sliderPosition = it },
            onValueChangeFinished = { if(hasPermission) onValueChange(sliderPosition.toInt()) },
            valueRange = valueRange,
            enabled = enabled && hasPermission,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (iconRes != null) 72.dp else 16.dp,
                    end = 16.dp,
                    bottom = 10.dp,
                ),
        )
    }
}

@Composable
fun ColorPreferenceItem(
    @DrawableRes iconRes: Int,
    title: String,
    summary: String? = null,
    color: Color,
    enabled: Boolean = true,
    hasPermission: Boolean = true,
    permissionDeniedSummary: String? = null,
    onPermissionRequired: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    PreferenceItem(
        iconRes = iconRes,
        title = title,
        summary = summary,
        enabled = enabled,
        hasPermission = hasPermission,
        permissionDeniedSummary = permissionDeniedSummary,
        onPermissionRequired = onPermissionRequired,
        onClick = onClick,
        widget = {
            val borderColor = MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(width = 1.5.dp, color = borderColor, shape = CircleShape),
            )
        },
    )
}

@Composable
fun PreferenceDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier.padding(vertical = 8.dp))
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
fun HelpCard(
    title: String,
    body: String,
    leftButtonText: String? = null,
    rightButtonText: String? = null,
    onClickLeft: (() -> Unit)? = null,
    onClickRight: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (leftButtonText != null && onClickLeft != null) {
                FilledTonalButton(onClick = onClickLeft) {
                    Text(leftButtonText)
                }
            }
            if (rightButtonText != null && onClickRight != null) {
                FilledTonalButton(onClick = onClickRight) {
                    Text(rightButtonText)
                }
            }
        }
    }
}