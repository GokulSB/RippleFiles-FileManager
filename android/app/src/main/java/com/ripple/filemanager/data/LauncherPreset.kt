package com.ripple.filemanager.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.ripple.filemanager.R

enum class LauncherPreset(
    val id: String,
    val labelRes: Int,
    val aliasName: String
) {
    DEFAULT("default", R.string.app_name_preset_default, "com.ripple.filemanager.alias.LauncherAliasDefault"),
    FILES("files", R.string.app_name_preset_files, "com.ripple.filemanager.alias.LauncherAliasFiles"),
    FILE_MANAGER("file_manager", R.string.app_name_preset_file_manager, "com.ripple.filemanager.alias.LauncherAliasFileManager");

    companion object {
        fun fromId(id: String?): LauncherPreset = values().firstOrNull { it.id == id } ?: DEFAULT

        fun applyPreset(context: Context, chosen: LauncherPreset) {
            val pm = context.packageManager
            // 1. Enable the chosen alias
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(context, chosen.aliasName),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                android.util.Log.e("LauncherPreset", "Error enabling chosen alias ${chosen.aliasName}", e)
            }

            // 2. Disable other aliases
            values().filter { it != chosen }.forEach { preset ->
                try {
                    pm.setComponentEnabledSetting(
                        ComponentName(context, preset.aliasName),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e: Exception) {
                    android.util.Log.e("LauncherPreset", "Error disabling alias ${preset.aliasName}", e)
                }
            }
        }
    }
}
