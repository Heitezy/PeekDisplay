package heitezy.peekdisplay.activities

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.services.NotificationService
import org.json.JSONArray
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceDivider
import heitezy.peekdisplay.ui.PreferenceItem
import heitezy.peekdisplay.ui.SectionHeader

class LAFFilterNotificationsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                FilterNotificationsScreen(onBack = {
                    AlwaysOn.finish()
                    finish()
                })
            }
        }
    }
}

private fun resolveAppLabel(pm: PackageManager, pkg: String): String =
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationLabel(
                pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(
                    PackageManager.GET_META_DATA.toLong()
                ))
            ) as String
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationLabel(
                pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
            ) as String
        }
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w(Global.LOG_TAG, e.toString())
        pkg
    }

@Composable
private fun FilterNotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    val prefs = remember { P.getP(context) }

    val blockedNotificationsFlow by prefs.getStringFlow("blocked_notifications", "[]")
        .collectAsState(initial = prefs.getString("blocked_notifications", "[]"))

    var blockedList by remember {
        val arr = JSONArray(blockedNotificationsFlow)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        mutableStateOf(list.toList())
    }

    LaunchedEffect(blockedNotificationsFlow) {
        val arr = JSONArray(blockedNotificationsFlow)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        blockedList = list.toList()
    }

    val activePackages = remember {
        val seen = mutableSetOf<String>()
        NotificationService.detailed
            .map { it.packageName }
            .filter { seen.add(it) }
    }

    fun persist(list: List<String>) {
        val arr = JSONArray().apply { list.forEach { put(it) } }
        prefs.edit { putString("blocked_notifications", arr.toString()) }
        NotificationService.activeService?.refreshNotifications()
    }

    PeekScaffold(
        title = stringResource(R.string.pref_look_and_feel_filter_notifications),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                PreferenceItem(
                    iconRes = R.drawable.ic_info,
                    summary = stringResource(R.string.pref_look_and_feel_filter_notifications_explanation))
            }

            item {
                SectionHeader(stringResource(R.string.pref_look_and_feel_filter_notifications_blocked))
            }

            if (blockedList.isEmpty()) {
                item {
                    PreferenceItem(
                        iconRes = R.drawable.ic_notification,
                        title = stringResource(R.string.pref_look_and_feel_filter_notifications_empty),
                        summary = stringResource(R.string.pref_look_and_feel_filter_notifications_empty_summary),
                    )
                }
            } else {
                items(blockedList, key = { it }) { pkg ->
                    PreferenceItem(
                        iconRes = R.drawable.ic_notification,
                        title = resolveAppLabel(pm, pkg),
                        summary = pkg,
                        widget = {
                            IconButton(onClick = {
                                val newList = blockedList - pkg
                                blockedList = newList
                                persist(newList)
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(android.R.string.cancel),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
            }

            item { PreferenceDivider() }

            item {
                SectionHeader(stringResource(R.string.pref_look_and_feel_filter_notifications_shown))
            }

            if (activePackages.isEmpty()) {
                item {
                    PreferenceItem(
                        iconRes = R.drawable.ic_notification,
                        title = stringResource(R.string.pref_look_and_feel_filter_notifications_empty),
                        summary = stringResource(R.string.pref_look_and_feel_filter_notifications_empty_summary),
                    )
                }
            } else {
                items(activePackages, key = { "shown_$it" }) { pkg ->
                    val isAlreadyBlocked = pkg in blockedList
                    PreferenceItem(
                        iconRes = R.drawable.ic_notification,
                        title = resolveAppLabel(pm, pkg),
                        summary = pkg,
                        widget = {
                            IconButton(
                                enabled = !isAlreadyBlocked,
                                onClick = {
                                    if (!isAlreadyBlocked) {
                                        val newList = blockedList + pkg
                                        blockedList = newList
                                        persist(newList)
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = stringResource(android.R.string.ok),
                                    tint = if (isAlreadyBlocked)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    else
                                        MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
