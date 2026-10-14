// SPDX-FileCopyrightText: ARMSX2 Reader fork contributors
// SPDX-License-Identifier: GPL-3.0+

package com.armsx2.reader

import androidx.compose.runtime.mutableStateOf

/**
 * Going on to the next choice: for someone who has read this far on another copy of the game
 * and wants to give it the same choices. The game runs at full speed with ○ pressed until its
 * profile reports a choice; the text on the way is not read (reader/core/next_choice.h has
 * it). Started from the in-game menu or with the button chosen in the reader's settings; a
 * press of any of the game's buttons stops it.
 */
object NextChoice {
    /** While it runs. Main thread. */
    val going = mutableStateOf(false)

    /** Starts it, or stops it when it runs. Main thread. */
    fun toggle() {
        try {
            ReaderNative.toggleNextChoice()
        } catch (_: UnsatisfiedLinkError) {
            // No emulator behind this (tests).
        }
    }

    /** The emulator says on the game's picture why it stopped (reader/core/emulator.cpp). */
    internal fun onChange(running: Boolean, @Suppress("UNUSED_PARAMETER") end: Int,
                          @Suppress("UNUSED_PARAMETER") passed: Long) {
        going.value = running
    }
}
