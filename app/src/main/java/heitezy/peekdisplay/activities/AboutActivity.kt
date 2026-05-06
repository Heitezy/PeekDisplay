package heitezy.peekdisplay.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import heitezy.peekdisplay.BuildConfig
import heitezy.peekdisplay.R
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceDivider
import heitezy.peekdisplay.ui.PreferenceItem

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                AboutScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        private const val REPOSITORY = "Heitezy/PeekDisplay"
        const val BRANCH = "main"
        const val REPOSITORY_URL = "https://github.com/$REPOSITORY"
        const val DONATE_URL =
            "https://www.paypal.com/donate/?hosted_button_id=TLTPDERG5X4VA"
        const val PRIVACY_POLICY_URL =
            "https://docs.github.com/en/github/site-policy/github-privacy-statement"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var showIconsDialog by remember { mutableStateOf(false) }
    var pendingExternalUrl by remember { mutableStateOf<String?>(null) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    // Icons dialog
    if (showIconsDialog) {
        val iconsArray = context.resources
            .getStringArray(R.array.about_icons_array)
        AlertDialog(
            onDismissRequest = { showIconsDialog = false },
            title = { Text(stringResource(R.string.about_icons)) },
            text = {
                Column {
                    iconsArray.forEachIndexed { index, item ->
                        Text(
                            text = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showIconsDialog = false
                                    openUrl(
                                        when (index) {
                                            0 -> "https://icons8.com/"
                                            1 -> "https://fonts.google.com/icons?selected=Material+Icons"
                                            else -> "about:blank"
                                        }
                                    )
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    // External link privacy dialog
    pendingExternalUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingExternalUrl = null },
            title = { Text(stringResource(R.string.about_privacy)) },
            text = { Text(stringResource(R.string.about_privacy_desc)) },
            confirmButton = {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            pendingExternalUrl = null
                            openUrl(AboutActivity.PRIVACY_POLICY_URL)
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(stringResource(R.string.about_privacy_policy))
                    }
                    TextButton(onClick = { pendingExternalUrl = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(onClick = {
                        pendingExternalUrl = null
                        openUrl(url)
                    }) { Text(stringResource(android.R.string.ok)) }
                }
            },
            dismissButton = null
        )
    }

    PeekScaffold(
        title = stringResource(R.string.about),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // App header tile
            PreferenceItem(
                iconRes = R.drawable.ic_always_on_accent,
                title = stringResource(R.string.app_name),
                summary = stringResource(R.string.app_description)
            )

            PreferenceDivider()

            // App version
            PreferenceItem(
                iconRes = R.drawable.ic_about_info,
                title = stringResource(R.string.about_app_version),
                summary = stringResource(
                    R.string.about_app_version_desc,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                )
            )

            // GitHub
            PreferenceItem(
                iconRes = R.drawable.ic_about_github,
                title = stringResource(R.string.about_github),
                summary = AboutActivity.REPOSITORY_URL,
                onClick = { pendingExternalUrl = AboutActivity.REPOSITORY_URL }
            )

            // License
            PreferenceItem(
                iconRes = R.drawable.ic_about_github,
                title = stringResource(R.string.about_license),
                summary = stringResource(R.string.about_license_desc),
                onClick = {
                    pendingExternalUrl =
                        "${AboutActivity.REPOSITORY_URL}/blob/${AboutActivity.BRANCH}/LICENSE"
                }
            )

            // Contributors
            PreferenceItem(
                iconRes = R.drawable.ic_about_contributor,
                title = stringResource(R.string.about_contributors),
                summary = stringResource(R.string.about_contributors_desc),
                onClick = {
                    pendingExternalUrl =
                        "${AboutActivity.REPOSITORY_URL}/graphs/contributors"
                }
            )

            // Libraries
            PreferenceItem(
                iconRes = R.drawable.ic_about_library,
                title = stringResource(R.string.about_libraries),
                summary = stringResource(R.string.about_libraries_desc),
                onClick = {
                    context.startActivity(
                        Intent(context, LibraryActivity::class.java)
                    )
                }
            )

            // Icons
            PreferenceItem(
                iconRes = R.drawable.ic_about_palette,
                title = stringResource(R.string.about_icons),
                summary = stringResource(R.string.about_icons_desc),
                onClick = { showIconsDialog = true }
            )

            PreferenceDivider()

            // Donate
            PreferenceItem(
                iconRes = R.drawable.ic_about_donate,
                title = stringResource(R.string.donate),
                summary = stringResource(R.string.donate_summary),
                onClick = { openUrl(AboutActivity.DONATE_URL) }
            )
        }
    }
}