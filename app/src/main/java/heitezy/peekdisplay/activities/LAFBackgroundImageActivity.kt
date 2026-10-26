package heitezy.peekdisplay.activities

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.helpers.P
import java.io.File
import java.io.FileOutputStream
import java.lang.Integer.min
import heitezy.peekdisplay.ui.HorizontalLayoutPicker
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceDivider

private const val MAXIMUM_BACKGROUND_RESOLUTION = 1080
private const val BACKGROUND_QUALITY = 60
private data class BgEntry(val key: String, val drawableRes: Int)

private val BG_ENTRIES = listOf(
    BgEntry(P.BACKGROUND_IMAGE_NONE,           R.drawable.ic_close),
    BgEntry(P.BACKGROUND_IMAGE_DANIEL_OLAH_1,  R.drawable.unsplash_daniel_olah_1),
    BgEntry(P.BACKGROUND_IMAGE_DANIEL_OLAH_2,  R.drawable.unsplash_daniel_olah_2),
    BgEntry(P.BACKGROUND_IMAGE_DANIEL_OLAH_3,  R.drawable.unsplash_daniel_olah_3),
    BgEntry(P.BACKGROUND_IMAGE_DANIEL_OLAH_4,  R.drawable.unsplash_daniel_olah_4),
    BgEntry(P.BACKGROUND_IMAGE_DANIEL_OLAH_5,  R.drawable.unsplash_daniel_olah_5),
    BgEntry(P.BACKGROUND_IMAGE_DANIEL_OLAH_6,  R.drawable.unsplash_daniel_olah_6),
    BgEntry(P.BACKGROUND_IMAGE_DANIEL_OLAH_7,  R.drawable.unsplash_daniel_olah_7),
    BgEntry(P.BACKGROUND_IMAGE_DANIEL_OLAH_8,  R.drawable.unsplash_daniel_olah_8),
    BgEntry(P.BACKGROUND_IMAGE_FILIP_BAOTIC_1, R.drawable.unsplash_filip_baotic_1),
    BgEntry(P.BACKGROUND_IMAGE_TYLER_LASTOVICH_1, R.drawable.unsplash_tyler_lastovich_1),
    BgEntry(P.BACKGROUND_IMAGE_TYLER_LASTOVICH_2, R.drawable.unsplash_tyler_lastovich_2),
    BgEntry(P.BACKGROUND_IMAGE_TYLER_LASTOVICH_3, R.drawable.unsplash_tyler_lastovich_3),
    BgEntry(P.BACKGROUND_IMAGE_CUSTOM,         R.drawable.ic_color_draw_over_other_apps),
)

private val CUSTOM_INDEX = BG_ENTRIES.indexOfFirst { it.key == P.BACKGROUND_IMAGE_CUSTOM }

private fun keyToIndex(key: String) =
    BG_ENTRIES.indexOfFirst { it.key == key }.coerceAtLeast(0)

private fun indexToKey(index: Int) =
    BG_ENTRIES.getOrNull(index)?.key ?: P.BACKGROUND_IMAGE_DEFAULT

class LAFBackgroundImageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                BackgroundImageScreen(onBack = {
                    AlwaysOn.finish()
                    finish()
                })
            }
        }
    }
}

@Composable
private fun BackgroundImageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val labels = context.resources
        .getStringArray(R.array.pref_ao_background_image_array_display)

    val prefs = remember { P.getP(context) }
    val savedKey by prefs.getStringFlow(P.BACKGROUND_IMAGE, P.BACKGROUND_IMAGE_DEFAULT)
        .collectAsState(initial = prefs.getString(P.BACKGROUND_IMAGE, P.BACKGROUND_IMAGE_DEFAULT))
    val fullScreenBackground by prefs.getBooleanFlow(P.FULL_SCREEN_BACKGROUND, P.FULL_SCREEN_BACKGROUND_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.FULL_SCREEN_BACKGROUND, P.FULL_SCREEN_BACKGROUND_DEFAULT))
    
    var selectedIndex by remember { mutableIntStateOf(keyToIndex(savedKey)) }

    LaunchedEffect(savedKey) {
        selectedIndex = keyToIndex(savedKey)
    }

    var customBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        val pathOrEncoded = prefs.getString(P.CUSTOM_BACKGROUND, "")
        val isFullScreen = prefs.getBoolean(P.FULL_SCREEN_BACKGROUND, P.FULL_SCREEN_BACKGROUND_DEFAULT)
        val reqDim = if (isFullScreen) 2340 else 1080

        if (pathOrEncoded.isNotEmpty()) {
            val file = File(pathOrEncoded)
            if (file.exists() && file.isFile) {
                customBitmap = Global.decodeSampledBitmapFromFile(pathOrEncoded, 1080, reqDim)?.asImageBitmap()
            } else {
                try {
                    val bytes = Base64.decode(pathOrEncoded, 0)
                    customBitmap = Global.decodeSampledBitmapFromByteArray(bytes, 0, bytes.size, 1080, reqDim)?.asImageBitmap()
                } catch (_: Exception) {
                }
            }
        }
    }

    // Photo picker launcher
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val thread = Thread {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@Thread
                val isFullScreen = prefs.getBoolean(P.FULL_SCREEN_BACKGROUND, P.FULL_SCREEN_BACKGROUND_DEFAULT)
                val targetWidth = 1080
                val targetHeight = if (isFullScreen) 2340 else 1080

                var bitmap = Global.decodeSampledBitmapFromStream(inputStream, targetWidth, targetHeight) ?: return@Thread

                if (!isFullScreen) {
                    val size = min(bitmap.width, bitmap.height)
                    bitmap = Bitmap.createBitmap(
                        bitmap,
                        (bitmap.width - size) / 2,
                        (bitmap.height - size) / 2,
                        size, size,
                    )
                    if (bitmap.width > MAXIMUM_BACKGROUND_RESOLUTION) {
                        bitmap = bitmap.scale(MAXIMUM_BACKGROUND_RESOLUTION, MAXIMUM_BACKGROUND_RESOLUTION)
                    }
                } else {
                    val maxDim = 2340
                    if (bitmap.width > targetWidth || bitmap.height > maxDim) {
                        val scale = minOf(targetWidth.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                        val newWidth = (bitmap.width * scale).toInt()
                        val newHeight = (bitmap.height * scale).toInt()
                        bitmap = bitmap.scale(newWidth, newHeight)
                    }
                }

                val file = File(context.filesDir, "custom_background.jpg")
                try {
                    val out = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, BACKGROUND_QUALITY, out)
                    out.flush()
                    out.close()
                    
                    val path = file.absolutePath
                    prefs.edit { putString(P.CUSTOM_BACKGROUND, path) }

                    val ib = bitmap.asImageBitmap()
                    Handler(Looper.getMainLooper()).post {
                        customBitmap = ib
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            thread.start()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            prefs.edit {
                putString(P.BACKGROUND_IMAGE, indexToKey(selectedIndex))
            }
        }
    }

    PeekScaffold(
        title = stringResource(R.string.pref_ao_background_image),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            val previewBitmap: ImageBitmap? =
                if (selectedIndex == CUSTOM_INDEX) customBitmap else null

            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    contentScale = if (fullScreenBackground) ContentScale.Crop else ContentScale.Fit,
                )
            } else if (selectedIndex != CUSTOM_INDEX) {
                Image(
                    painter = painterResource(
                        BG_ENTRIES[selectedIndex].drawableRes
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    contentScale = if (fullScreenBackground) ContentScale.Crop else ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                ) { }
            }

            PreferenceDivider()

            HorizontalLayoutPicker(
                items = BG_ENTRIES.mapIndexed { i, entry ->
                    entry.drawableRes to labels.getOrElse(i) { "" }
                },
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    selectedIndex = index
                    if (index == CUSTOM_INDEX) {
                        pickMedia.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                    AlwaysOn.finish()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
