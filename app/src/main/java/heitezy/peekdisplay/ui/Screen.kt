package heitezy.peekdisplay.ui


import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import heitezy.peekdisplay.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeekScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    titleCentered: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (titleCentered) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(title)
                        }
                    } else {
                        Text(title)
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = actions,
            )
        },
        content = content,
    )
}

@Composable
fun SetupScreenContent(
    @DrawableRes iconRes: Int,
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    iconContentDescription: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = iconContentDescription,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterHorizontally),
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun HorizontalLayoutPicker(
    items: List<Pair<Int, String>>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEachIndexed { index, (drawableRes, label) ->
            LayoutPickerTile(
                painter = painterResource(drawableRes),
                label = label,
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) },
            )
        }
    }
}

@Composable
private fun LayoutPickerTile(
    painter: Painter,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(104.dp)
            .selectable(selected = selected, onClick = onClick),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = if (selected) 4.dp else 0.dp,
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor
            ),
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
        ) {
            Image(
                painter = painter,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}