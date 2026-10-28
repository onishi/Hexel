package com.hexel.launcher.apps

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle

/** One launchable app. Hexel shows one tile per package (its first launcher activity). */
class AppEntry(internal val info: LauncherActivityInfo) {
    val packageName: String = info.componentName.packageName
    val label: String = info.label?.toString().orEmpty().ifBlank { packageName }
    val component: ComponentName = info.componentName
    val user: UserHandle = info.user
}
