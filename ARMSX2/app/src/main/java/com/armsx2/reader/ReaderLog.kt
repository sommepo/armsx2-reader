// SPDX-FileCopyrightText: ARMSX2 Reader fork contributors (from the PPSSPP Reader fork)
// SPDX-License-Identifier: GPL-3.0+

package com.armsx2.reader

/** The reader's lines in Android's log, under one tag. */
object ReaderLog {
    private const val TAG = "ARMSX2Reader"

    fun info(message: String) = write(android.util.Log.INFO, message)

    fun warning(message: String) = write(android.util.Log.WARN, message)

    fun error(message: String) = write(android.util.Log.ERROR, message)

    private fun write(priority: Int, message: String) {
        try {
            android.util.Log.println(priority, TAG, message)
        } catch (_: RuntimeException) {
            // Off the device (plain JVM tests) there is no log to write to.
        }
    }
}
