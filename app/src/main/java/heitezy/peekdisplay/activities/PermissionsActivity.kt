package heitezy.peekdisplay.activities

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.PreferenceScreenHelper
import heitezy.peekdisplay.helpers.Root

class PermissionsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Handle window insets for the main container
        val rootView = findViewById<FrameLayout>(R.id.settings)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            view.setPadding(
                insets.left,
                insets.top,
                insets.right,
                insets.bottom
            )

            WindowInsetsCompat.CONSUMED
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, PreferencePermissions())
            .commit()
    }

    class PreferencePermissions : PreferenceFragmentCompat() {
        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            addPreferencesFromResource(R.xml.pref_permissions)
            PreferenceScreenHelper.linkPreferenceToActivity(
                this,
                "ignore_battery_optimizations",
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            )
            findPreference<Preference>("device_admin")?.setOnPreferenceClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_device_admin, null, false)
                val editText = dialogView.findViewById<EditText>(R.id.editText)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.device_admin)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (editText.text.toString().trim() == "19") {
                            startActivity(
                                Intent().apply {
                                    component =
                                        ComponentName(
                                            "com.android.settings",
                                            $$"com.android.settings.Settings$DeviceAdminSettingsActivity",
                                        )
                                },
                            )
                        } else {
                            Toast.makeText(
                                requireContext(),
                                R.string.dialog_device_admin_error,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
                true
            }
            findPreference<Preference>("root_mode")?.setOnPreferenceClickListener {
                if (!Root.request()) {
                    val toast =
                        Toast.makeText(context, R.string.setup_root_failed, Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    (it as SwitchPreference).isChecked = false
                }
                true
            }
        }
    }
}
