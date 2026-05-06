package heitezy.peekdisplay.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import heitezy.peekdisplay.R
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceItem

class LibraryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                LibraryScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun LibraryScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val libraries = remember { context.resources.getStringArray(R.array.about_libraries) }
    val licenses  = remember { context.resources.getStringArray(R.array.about_libraries_licenses) }
    require(libraries.size == licenses.size) {
        "Library array size does not match license array size."
    }

    PeekScaffold(
        title = stringResource(R.string.about_libraries),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            (libraries.toList()).forEachIndexed { index, library ->
                PreferenceItem(
                    iconRes = R.drawable.ic_about_library,
                    title = library,
                    summary = licenses[index],
                )
            }
        }
    }
}