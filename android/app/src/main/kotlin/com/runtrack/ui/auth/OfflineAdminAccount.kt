package com.runtrack.ui.auth

import com.runtrack.BuildConfig

/** Local development access only. This account has no server token or server privileges. */
object OfflineAdminAccount {
    const val USERNAME = "admin"
    const val PASSWORD = "admin1234"
    val enabled: Boolean get() = BuildConfig.DEBUG

    fun matches(username: String, password: String, enabled: Boolean = this.enabled): Boolean =
        enabled && username.trim() == USERNAME && password == PASSWORD

    fun isReserved(username: String): Boolean = enabled && username.trim() == USERNAME
}
