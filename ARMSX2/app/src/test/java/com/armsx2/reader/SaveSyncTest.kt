// SPDX-FileCopyrightText: ARMSX2 Reader fork contributors
// SPDX-License-Identifier: GPL-3.0+

package com.armsx2.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What the save sync page reads of the native side's state (reader_jni.cpp, saveSyncState). */
@RunWith(RobolectricTestRunner::class)
class SaveSyncTest {
    @Test
    fun theStateIsReadWithItsConflicts() {
        val state = SaveSync.parse(
            """{"on":true,"generation":7,"status":"Never7: fetched from the PC","running":"SLPS-25256",""" +
                """"conflicts":[{"title":"534C505300025256","disc":"SLPS-25256","name":"Never7","message":"Both changed \"since\""}]}"""
        )!!
        assertTrue(state.on)
        assertEquals(7L, state.generation)
        assertEquals("Never7: fetched from the PC", state.status)
        assertEquals("SLPS-25256", state.running)
        assertEquals(1, state.conflicts.size)
        assertEquals("534C505300025256", state.conflicts[0].title)
        assertEquals("Never7", state.conflicts[0].name)
        assertEquals("Both changed \"since\"", state.conflicts[0].message)
    }

    @Test
    fun nothingStartedIsNoState() {
        assertNull(SaveSync.parse("{}"))
        assertNull(SaveSync.parse("not json"))
        assertFalse(SaveSync.parse("""{"on":false,"generation":0,"status":"","running":"","conflicts":[]}""")!!.on)
    }

    @Test
    fun withoutTheNativeSideNothingBreaks() {
        // Off the device there is no native library: the page shows "off" and nothing throws.
        assertNull(SaveSync.state())
        SaveSync.syncNow()
        SaveSync.resolve("534C505300025256", keepMine = true)
    }
}
