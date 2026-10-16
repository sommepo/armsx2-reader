// SPDX-FileCopyrightText: ARMSX2 Reader fork contributors (from the ARMSX2 Reader fork) (from the ARMSX2 Reader fork)
// SPDX-License-Identifier: GPL-3.0+

package com.armsx2.reader

import android.content.Context
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.armsx2.reader.ui.ReaderBadge

/**
 * Which games the reader can read, and their mark in the game list: from what each text profile
 * says of itself in its first lines.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w768dp-h432dp-400dpi")
class ReaderCatalogTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val aceAttorney = "SLPM-60001"
    private val unknown = "SLPM-69999"

    @Before
    fun setUp() = ReaderCatalog.forget()

    @After
    fun tearDown() = ReaderCatalog.forget()

    private fun ownProfile(titleId: String, source: String): File {
        val folder = File(context.getExternalFilesDir(null), "reader/profiles").apply { mkdirs() }
        return File(folder, "$titleId.lua").apply { writeText(source) }
    }

    @Test
    fun aProfileSaysWhatItIsInItsFirstLines() {
        val info = ReaderCatalog.describe(
            unknown,
            """
            -- reader: no hooks
            -- reader: title 作り話 (A Made-up Game)
            --   reader:   reads   Dialogue and choices
            -- reader: version Base game, version 1
            -- reader: something a later reader may know
            -- An ordinary comment about how the game keeps its text.

            local window = 0x1234
            -- reader: title Not this: the comments at the top have ended
            """.trimIndent(),
            own = false
        )
        assertEquals("作り話 (A Made-up Game)", info.title)
        assertEquals("Dialogue and choices", info.reads)
        assertEquals("Base game, version 1", info.version)
        assertFalse(info.own)

        // A profile that says nothing is known by its ID.
        val silent = ReaderCatalog.describe(unknown, "-- A profile of my own.\nlocal x = 1\n", own = true)
        assertEquals(unknown, silent.title)
        assertEquals("", silent.reads)
        assertEquals("", silent.version)
        assertTrue(silent.own)
    }

    @Test
    fun everyBundledProfileSaysItsGameWhatIsReadAndForWhichVersion() {
        val games = ReaderCatalog.games(context)
        // At least the games of the published list (ひぐらしのなく頃に祭 カケラ遊び, Remember11,
        // Ever17, Never7, ドラゴンクエストV, サイレントヒル2, アマガミ): a checkout of one's own has
        // more, those whose scripts need a table that is not published.
        val ids = games.map { it.titleId }.toSet()
        for (id in listOf("SLPM-66913", "SLPM-65550", "SLPM-65421", "SLPS-25256", "SLPM-65555", "SLPM-65051", "SLPS-25918")) {
            assertTrue("$id is not bundled: $ids", id in ids)
        }
        for (game in games) {
            assertTrue("${game.titleId} has no title line", game.title != game.titleId && game.title.isNotBlank())
            assertTrue("${game.titleId} does not say what is read", game.reads.isNotBlank())
            assertTrue("${game.titleId} does not say its version", game.version.isNotBlank())
            assertFalse(game.own)
        }
        assertEquals("by title", games.map { it.title }.sortedWith(String.CASE_INSENSITIVE_ORDER), games.map { it.title })
    }

    @Test
    fun gamesWithoutVoicesGetNoAudioOnTheirCardsUnlessTheUserSaysOtherwise() {
        val never = ReaderSettings.CardAudio.NEVER
        // No bundled game is without voices so far. A profile says so with a line of its
        // own, here one of the user's.
        val withoutVoices = emptySet<String>()
        ownProfile(aceAttorney, "-- reader: title 作り話\n-- reader: audio never\n")
        ReaderCatalog.forget()
        for (game in ReaderCatalog.games(context).filterNot { it.own }) {
            val said = ReaderCatalog.cardAudio(context, game.titleId)
            assertEquals(game.titleId, if (game.titleId in withoutVoices) never else null, said)
        }
        assertEquals(never, ReaderCatalog.cardAudio(context, aceAttorney.lowercase()))
        val settings = ReaderSettings(context)
        // The profile's word goes before the general choice and after the user's own for the game.
        assertEquals(ReaderSettings.CardAudio.ASK, settings.cardAudioFor("SLPM-65000", null))
        assertEquals(never, settings.cardAudioFor(aceAttorney, never))
        settings.setCardAudioOf(aceAttorney, ReaderSettings.CardAudio.ALWAYS)
        assertEquals(ReaderSettings.CardAudio.ALWAYS, settings.cardAudioFor(aceAttorney, never))
        settings.setCardAudioOf(aceAttorney, null)
        // A profile of the user's own for a game says it the same way.
        ownProfile("SLPM-65000", "-- reader: title X\n-- reader: audio Never\n")
        ReaderCatalog.forget()
        assertEquals(never, ReaderCatalog.cardAudio(context, "SLPM-65000"))
    }

    @Test
    fun aGameIsCoveredWhenItHasAProfileBundledOrTheUsersOwn() {
        assertTrue("a bundled game", ReaderCatalog.covers(context, "SLPM-66913"))
        assertFalse(ReaderCatalog.covers(context, aceAttorney))
        assertFalse(ReaderCatalog.covers(context, unknown))
        assertFalse("a game without a title ID", ReaderCatalog.covers(context, "0"))
        assertNull(ReaderCatalog.titleIdOf("$aceAttorney.table.bin"))
        assertNull(ReaderCatalog.titleIdOf("notes.lua"))

        val bundled = ReaderCatalog.games(context).size
        ownProfile(unknown, "-- reader: title 作り話\nlocal x = 1\n")
        ownProfile(aceAttorney, "-- reader: title My Ace Attorney profile\nlocal x = 1\n")
        ReaderCatalog.forget()
        assertTrue(ReaderCatalog.covers(context, unknown))
        assertTrue("however the ID is written", ReaderCatalog.covers(context, aceAttorney.lowercase()))
        val games = ReaderCatalog.games(context)
        assertEquals(bundled + 2, games.size)
        assertTrue(games.first { it.titleId == unknown }.own)
        val ace = games.single { it.titleId == aceAttorney }
        assertEquals("My Ace Attorney profile", ace.title)
        assertTrue(ace.own)
    }

    @Test
    fun aReadableGameCarriesTheBadgeInTheGameListAndAnotherDoesNot() {
        val frame = FrameLayout(context)
        val cover = ImageView(context).apply { setBackgroundColor(Color.rgb(0x5B, 0x6E, 0x8C)) }
        frame.addView(cover, FrameLayout.LayoutParams(375, 375))

        ownProfile(aceAttorney, "-- reader: title 作り話\nlocal x = 1\n")
        ReaderCatalog.forget()
        ReaderBadge.mark(cover, aceAttorney)
        assertTrue(ReaderBadge.isOn(cover))
        TestSupport.snapshot(frame, 375, 375, "game-list-badge")

        // A row of the list is used again for another game: the badge goes with the game.
        ReaderBadge.mark(cover, unknown)
        assertFalse(ReaderBadge.isOn(cover))
        ReaderBadge.mark(cover, aceAttorney)
        ReaderBadge.mark(cover, aceAttorney)
        assertTrue(ReaderBadge.isOn(cover))
    }
}
