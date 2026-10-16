// SPDX-FileCopyrightText: ARMSX2 Reader fork contributors (from the PPSSPP Reader fork)
// SPDX-License-Identifier: GPL-3.0+

package com.armsx2.reader.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.LinearLayout
import androidx.fragment.app.FragmentContainerView
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import java.util.concurrent.Executors
import com.armsx2.R
import com.armsx2.reader.ReaderCatalog
import com.armsx2.reader.ReaderSession
import com.armsx2.reader.ReaderSettings
import com.armsx2.reader.SaveSync
import com.armsx2.reader.stats.ReadingRules
import com.armsx2.reader.stats.SessionSummary

/**
 * The reader's own page of the app: statistics and the supported games first, then one entry
 * per section of settings. A section opens as a page of its own; Back returns to the first.
 */
class ReaderSettingsActivity : ReaderScreen() {
    override val titleRes = R.string.reader_title

    /** A page of settings: the entry of the first page that opens it, its heading, its content. */
    enum class Section(val key: String, val title: Int, val preferences: Int) {
        LOOKUP("reader_section_lookup", R.string.reader_pref_category_screen_text, R.xml.reader_prefs_lookup),
        CURSOR("reader_section_cursor", R.string.reader_pref_category_cursor, R.xml.reader_prefs_cursor),
        PANEL("reader_section_panel", R.string.reader_pref_category_panel, R.xml.reader_prefs_panel),
        CARDS("reader_section_cards", R.string.reader_pref_category_mining, R.xml.reader_prefs_cards),
        STATS("reader_section_stats", R.string.reader_pref_category_stats, R.xml.reader_prefs_stats),
        SAVE_SYNC("reader_section_savesync", R.string.reader_pref_category_savesync, R.xml.reader_prefs_savesync),
        OUTPUT("reader_section_output", R.string.reader_pref_category_output, R.xml.reader_prefs_output)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FragmentContainerView(this).apply { id = R.id.reader_fragment_container }
        content.addView(container, LinearLayout.LayoutParams(MATCH, MATCH))
        if (savedInstanceState == null) {
            // Asked for one section (the app's setup screen asks for Save sync): its page
            // alone, and Back leaves.
            val first = directSection?.let { SectionPage.of(it) } ?: FirstPage()
            supportFragmentManager.beginTransaction()
                .replace(R.id.reader_fragment_container, first)
                .commit()
        }
        // The arrow goes where Back goes: from a section to the first page, from there out.
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        supportFragmentManager.addOnBackStackChangedListener { showTitle() }
        showTitle()
    }

    override fun onPause() {
        // The game screen may not be attached to hear preference changes: apply them here too.
        ReaderSession.onSettingsChanged()
        super.onPause()
    }

    /** The section this screen was opened for, when it was opened for one. */
    private val directSection: Section? get() = section(intent?.getStringExtra(SECTION))

    /** The section whose page is open, or null on the first page. */
    val openSection: Section?
        get() = supportFragmentManager.run {
            if (backStackEntryCount == 0) directSection else section(getBackStackEntryAt(backStackEntryCount - 1).name)
        }

    fun open(section: Section) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.reader_fragment_container, SectionPage.of(section))
            .addToBackStack(section.name)
            .commit()
    }

    private fun showTitle() {
        toolbar.setTitle(openSection?.title ?: titleRes)
    }

    /** Statistics and the supported games, then the sections. */
    class FirstPage : PreferenceFragmentCompat() {
        private val worker = Executors.newSingleThreadExecutor()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.reader_preferences, rootKey)
            link("reader_open_stats", ReaderStatsActivity::class.java)
            link("reader_open_games", ReaderGamesActivity::class.java)
            link("reader_open_crt", CrtSettingsActivity::class.java)
            link("reader_open_dictionaries", DictionaryManagerActivity::class.java)
            // A published build has no save sync.
            if (!SaveSync.built) findPreference<Preference>(Section.SAVE_SYNC.key)?.isVisible = false
            for (section in Section.values()) {
                findPreference<Preference>(section.key)?.setOnPreferenceClickListener {
                    (activity as? ReaderSettingsActivity)?.open(section)
                    true
                }
            }
        }

        override fun onResume() {
            super.onResume()
            // What was read today, and how many games can be read, where they are opened from.
            val context = requireContext().applicationContext
            ReaderSession.flush {
                worker.execute {
                    val today = ReaderSession.database(context)
                        .dayTotals(ReadingRules.readingDay(System.currentTimeMillis()))
                    val games = ReaderCatalog.games(context).size
                    activity?.runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        findPreference<Preference>("reader_open_stats")?.summary = if (today.isEmpty) {
                            getString(R.string.reader_statistics_nothing_today)
                        } else {
                            getString(R.string.reader_statistics_today, SessionSummary.format(today))
                        }
                        findPreference<Preference>("reader_open_games")?.summary =
                            resources.getQuantityString(R.plurals.reader_games_count, games, games)
                    }
                }
            }
        }

        override fun onDestroy() {
            worker.shutdown()
            super.onDestroy()
        }

        private fun link(key: String, screen: Class<*>) {
            findPreference<Preference>(key)?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), screen))
                true
            }
        }
    }

    /** One section's settings. */
    class SectionPage : PreferenceFragmentCompat() {
        private val ticks = android.os.Handler(android.os.Looper.getMainLooper())
        private var shownGeneration = -1L
        private val tick = object : Runnable {
            override fun run() {
                showSaveSync()
                ticks.postDelayed(this, 500)
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val section = section(requireArguments().getString(SECTION)) ?: Section.LOOKUP
            // Play by touch's first value depends on the device: kept before its switch shows it.
            ReaderSettings(requireContext()).keepTouchPlayDefault()
            setPreferencesFromResource(section.preferences, rootKey)
            if (section == Section.SAVE_SYNC) bindSaveSync()
            findPreference<Preference>("reader_open_anki")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), AnkiSetupActivity::class.java))
                true
            }
            findPreference<EditTextPreference>("reader_texthook_port")?.apply {
                setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
                setOnPreferenceChangeListener { _, value ->
                    (value as? String)?.toIntOrNull()?.let { it in 1024..65535 } == true
                }
            }
        }

        // ---- Save sync's page ----

        private fun bindSaveSync() {
            val context = requireContext().applicationContext
            for (key in listOf(SaveSync.KEY_ENABLED, ReaderSettings.KEY_STATS_SERVER, ReaderSettings.KEY_STATS_CODE,
                    ReaderSettings.KEY_STATS_DEVICE)) {
                findPreference<Preference>(key)?.setOnPreferenceChangeListener { _, _ ->
                    // Read once the new value is kept.
                    ticks.post { SaveSync.apply(context) }
                    true
                }
            }
            findPreference<EditTextPreference>(ReaderSettings.KEY_STATS_CODE)?.setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            findPreference<Preference>("reader_savesync_status")?.setOnPreferenceClickListener {
                SaveSync.apply(context)
                SaveSync.syncNow()
                true
            }
            showSaveSync()
        }

        private fun showSaveSync() {
            val status = findPreference<Preference>("reader_savesync_status") ?: return
            val state = SaveSync.state()
            status.summary = when {
                state == null || !state.on -> getString(R.string.reader_pref_savesync_off)
                state.status.isNotEmpty() -> state.status
                else -> null
            }
            val conflicts = findPreference<androidx.preference.PreferenceCategory>("reader_savesync_conflicts") ?: return
            val generation = state?.generation ?: -1L
            if (generation == shownGeneration) return
            shownGeneration = generation
            conflicts.removeAll()
            val list = state?.conflicts.orEmpty()
            conflicts.isVisible = list.isNotEmpty()
            for (conflict in list) {
                conflicts.addPreference(Preference(requireContext()).apply {
                    isIconSpaceReserved = false
                    title = conflict.name.ifEmpty { conflict.disc }
                    summary = conflict.message
                    setOnPreferenceClickListener {
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(title)
                            .setMessage(conflict.message)
                            .setPositiveButton(R.string.reader_savesync_keep_mine) { _, _ ->
                                SaveSync.resolve(conflict.title, keepMine = true)
                            }
                            .setNegativeButton(R.string.reader_savesync_take_theirs) { _, _ ->
                                SaveSync.resolve(conflict.title, keepMine = false)
                            }
                            .show()
                        true
                    }
                })
            }
        }

        override fun onResume() {
            super.onResume()
            if (findPreference<Preference>("reader_savesync_status") != null) ticks.post(tick)
        }

        override fun onPause() {
            ticks.removeCallbacks(tick)
            super.onPause()
        }

        companion object {
            fun of(section: Section) = SectionPage().apply {
                arguments = Bundle().apply { putString(SECTION, section.name) }
            }
        }
    }

    companion object {
        private const val SECTION = "section"

        private fun section(name: String?): Section? = Section.values().firstOrNull { it.name == name }

        /** Opens the screen at one section's page instead of its first page. */
        fun intent(context: Context, section: Section): Intent =
            Intent(context, ReaderSettingsActivity::class.java).putExtra(SECTION, section.name)
    }
}
