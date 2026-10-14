// SPDX-FileCopyrightText: ARMSX2 Reader fork contributors
// SPDX-License-Identifier: GPL-3.0+

package com.armsx2.reader

import java.io.File
import java.nio.file.Files
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Game scripts from the published list: reading the list, how a script compares with what the
 * app has, installing, updating and removing. The same cases as PCSX2 Reader's
 * reader/desktop/tests/script_index.cpp, with made-up scripts.
 */
@RunWith(RobolectricTestRunner::class)
class ScriptStoreTest {
    private val madeUp = "-- reader: no hooks\n-- reader: title 作り話\n-- reader: reads The message window\n" +
        "-- reader: version SLPM-69999\nlocal x = 1\n"
    private val older = madeUp.toByteArray()
    private val newer = (madeUp + "local y = 2\n").toByteArray()
    private val table = "あいうえお\n".toByteArray()

    private fun file(name: String, bytes: ByteArray) =
        JSONObject().put("file", name).put("sha256", ScriptStore.sha256(bytes)).put("size", bytes.size)

    private fun list(script: ByteArray, withTable: Boolean = false, api: Int = 1): ByteArray {
        val entry = file("SLPM-69999.lua", script).put("id", "SLPM-69999").put("title", "作り話")
            .put("reads", "The message window").put("version", "SLPM-69999").put("api", api)
        if (withTable) entry.put("data", JSONArray().put(file("SLPM-69999.charset", table)))
        return JSONObject().put("format", 1).put("system", "ps2").put("scripts", JSONArray().put(entry))
            .toString().toByteArray()
    }

    private fun entryOf(json: ByteArray) = ScriptStore.parseIndex(json).single()

    private fun folder() = ScriptStore.Folder(File(Files.createTempDirectory("scripts").toFile(), "profiles"))

    @Test
    fun thePublishedListIsRead() {
        val entry = entryOf(list(older, withTable = true))
        assertEquals("SLPM-69999", entry.id)
        assertEquals("作り話", entry.title)
        assertEquals("SLPM-69999.lua", entry.script.file)
        assertEquals(ScriptStore.sha256(older), entry.script.sha256)
        assertEquals(listOf("SLPM-69999.charset"), entry.data.map { it.file })
        assertThrows(ScriptStore.ScriptException::class.java) { ScriptStore.parseIndex("not json".toByteArray()) }
        // A format from a later app is refused, not half read.
        assertThrows(ScriptStore.ScriptException::class.java) {
            ScriptStore.parseIndex("""{"format": 2, "scripts": []}""".toByteArray())
        }
        // An entry whose file is a path, or not its game's script, is passed over.
        val root = JSONObject(String(list(older)))
        val wrong = root.getJSONArray("scripts").getJSONObject(0)
        root.put("scripts", JSONArray()
            .put(JSONObject(wrong.toString()).put("file", "../SLPM-69999.lua"))
            .put(JSONObject(wrong.toString()).put("id", "SLPM-69998")))
        assertTrue(ScriptStore.parseIndex(root.toString().toByteArray()).isEmpty())
    }

    @Test
    fun whichFilesAScriptMayBeOrAskFor() {
        assertEquals("SLPM-65555", ScriptStore.idOfFile("SLPM-65555.lua"))
        assertEquals("SLPM-65555", ScriptStore.idOfFile("SLPM-65555.table.bin"))
        assertNull(ScriptStore.idOfFile(".installed.json"))
        assertNull(ScriptStore.idOfFile("SLPM-65555..lua"))
        assertNull(ScriptStore.idOfFile("../SLPM-65555.lua"))
        assertNull(ScriptStore.idOfFile("notes.txt"))
    }

    @Test
    fun theApiAScriptNeeds() {
        assertEquals(1, ScriptStore.apiOf(madeUp))
        assertEquals(3, ScriptStore.apiOf("-- reader: title x\n-- reader: api 3\nlocal x\n"))
        assertEquals(1, ScriptStore.apiOf("local x\n-- reader: api 3\n"))
    }

    @Test
    fun howTheListsScriptComparesWithWhatTheAppHas() {
        val entry = entryOf(list(newer))
        val oldSha = ScriptStore.sha256(older)
        val own = (madeUp + "-- mine\n").toByteArray()
        assertEquals(ScriptStore.State.NOT_INSTALLED, ScriptStore.stateOf(entry, null, null, ""))
        assertEquals(ScriptStore.State.INSTALLED, ScriptStore.stateOf(entry, newer, null, ""))
        assertEquals(ScriptStore.State.INSTALLED, ScriptStore.stateOf(entry, null, newer, ""))
        assertEquals(ScriptStore.State.UPDATE, ScriptStore.stateOf(entry, null, older, ""))
        assertEquals(ScriptStore.State.UPDATE, ScriptStore.stateOf(entry, older, null, oldSha))
        assertEquals(ScriptStore.State.OWN, ScriptStore.stateOf(entry, older, null, ""))
        assertEquals(ScriptStore.State.OWN, ScriptStore.stateOf(entry, own, null, oldSha))
        assertEquals(ScriptStore.State.OWN, ScriptStore.stateOf(entry, own, newer, ""))
        assertEquals(ScriptStore.State.TOO_NEW,
            ScriptStore.stateOf(entryOf(list(older, api = ScriptStore.API + 1)), null, null, ""))
    }

    @Test
    fun installingFromTheListUpdatingRemoving() {
        val folder = folder()
        val entry = entryOf(list(older, withTable = true))
        // A file that is not the one the list names is refused, and nothing is written.
        assertThrows(ScriptStore.ScriptException::class.java) {
            folder.install(entry, mapOf("SLPM-69999.lua" to newer, "SLPM-69999.charset" to table))
        }
        assertNull(folder.script("SLPM-69999"))
        assertThrows(ScriptStore.ScriptException::class.java) { folder.install(entry, mapOf("SLPM-69999.lua" to older)) }

        folder.install(entry, mapOf("SLPM-69999.lua" to older, "SLPM-69999.charset" to table))
        assertTrue(folder.script("SLPM-69999")!!.contentEquals(older))
        assertTrue(File(folder.dir, "SLPM-69999.charset").isFile)
        assertEquals(ScriptStore.sha256(older), folder.recorded("SLPM-69999"))

        val update = entryOf(list(newer))
        assertEquals(ScriptStore.State.UPDATE,
            ScriptStore.stateOf(update, folder.script(update.id), null, folder.recorded(update.id)))
        folder.install(update, mapOf("SLPM-69999.lua" to newer))
        assertTrue(folder.script("SLPM-69999")!!.contentEquals(newer))

        folder.install(entry, mapOf("SLPM-69999.lua" to older, "SLPM-69999.charset" to table))
        assertTrue(folder.remove("SLPM-69999"))
        assertNull(folder.script("SLPM-69999"))
        assertFalse(File(folder.dir, "SLPM-69999.charset").exists())
        assertEquals("", folder.recorded("SLPM-69999"))
        assertFalse(folder.remove("SLPM-69999"))
    }

    @Test
    fun installingFilesTheUserChose() {
        val folder = folder()
        folder.install(entryOf(list(newer)), mapOf("SLPM-69999.lua" to newer))
        val (ids, problems) = folder.installFiles(listOf(
            "SLPM-69999.lua" to older,
            "SLPM-69999.charset" to table,
            "my script.lua" to older,
            "SLPM-69998.lua" to byteArrayOf(0xff.toByte(), 0xfe.toByte(), 0, 1),
        ))
        assertEquals(listOf("SLPM-69999"), ids)
        assertTrue(problems.joinToString(), problems.any { it.startsWith("my script.lua") })
        assertTrue(problems.joinToString(), problems.any { it.startsWith("SLPM-69998.lua") })
        assertTrue(folder.script("SLPM-69999")!!.contentEquals(older))
        assertTrue(File(folder.dir, "SLPM-69999.charset").isFile)
        assertFalse(File(folder.dir, "SLPM-69998.lua").exists())
        assertEquals("", folder.recorded("SLPM-69999"))
    }

    @Test
    fun aFileOfTheListIsFetchedFromBesideIt() {
        assertEquals(
            "https://example.invalid/repo/reader/profiles/SLPM-65555.lua",
            ScriptStore.urlOf("SLPM-65555.lua", "https://example.invalid/repo/reader/profiles/index.json")
        )
    }
}
