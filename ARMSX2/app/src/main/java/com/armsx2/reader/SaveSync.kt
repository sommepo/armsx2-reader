// SPDX-FileCopyrightText: ARMSX2 Reader fork contributors
// SPDX-License-Identifier: GPL-3.0+

package com.armsx2.reader

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONObject

/**
 * Save sync: each game's memory card kept the same as on the user's save sync service, as PCSX2
 * Reader keeps it (one card a game, `memcards/reader-saves/<serial>/card.ps2`, named to the
 * service by its serial). The work is native (cpp/reader/save_sync, PCSX2 Reader's, and the
 * card's part in cpp/reader/core/emulator.cpp); this hands it the settings of the page and
 * reads what it has to say.
 */
object SaveSync {
    const val KEY_ENABLED = "reader_savesync"

    data class Conflict(val title: String, val disc: String, val name: String, val message: String)

    data class State(
        val on: Boolean,
        val status: String,
        val running: String,
        val conflicts: List<Conflict>,
        val generation: Long,
    )

    /** Whether this build has save sync at all (a published one has not). */
    val built get() = com.armsx2.BuildConfig.READER_SAVE_SYNC

    /** Hands the page's settings to the native side: when the app starts and after a change. */
    fun apply(context: Context) {
        if (!built) return
        val settings = ReaderSettings(context)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val addresses = settings.statsServer.split(',')
            .map { it.trim().removeSuffix("/") }
            .filter { it.isNotEmpty() }
            .joinToString(",")
        val code = settings.statsCode
        val enabled = preferences.getBoolean(KEY_ENABLED, true) && addresses.isNotEmpty() && code.isNotEmpty()
        try {
            ReaderNative.saveSyncConfigure(
                enabled, addresses.toByteArray(), code.toByteArray(), settings.statsDevice.toByteArray()
            )
        } catch (_: UnsatisfiedLinkError) {
        }
    }

    /** What the native side has to say now; null when there is nothing (off, or no native side). */
    fun state(): State? {
        if (!built) return null
        val json = try {
            String(ReaderNative.saveSyncState(), Charsets.UTF_8)
        } catch (_: UnsatisfiedLinkError) {
            return null
        }
        return parse(json)
    }

    fun parse(json: String): State? = try {
        val root = JSONObject(json)
        if (!root.has("on")) {
            null
        } else {
            val list = root.optJSONArray("conflicts")
            val conflicts = (0 until (list?.length() ?: 0)).map { i ->
                val c = list!!.getJSONObject(i)
                Conflict(c.optString("title"), c.optString("disc"), c.optString("name"), c.optString("message"))
            }
            State(
                root.optBoolean("on"), root.optString("status"), root.optString("running"),
                conflicts, root.optLong("generation")
            )
        }
    } catch (_: Exception) {
        null
    }

    /** Asks the service whether it is there and the code right, and brings every game in line. */
    fun syncNow() {
        try {
            ReaderNative.saveSyncCheck()
        } catch (_: UnsatisfiedLinkError) {
        }
    }

    /** Answers a conflict: this device's save goes up, or the other device's comes down. */
    fun resolve(title: String, keepMine: Boolean) {
        try {
            ReaderNative.saveSyncResolve(title.toByteArray(), keepMine)
        } catch (_: UnsatisfiedLinkError) {
        }
    }
}
