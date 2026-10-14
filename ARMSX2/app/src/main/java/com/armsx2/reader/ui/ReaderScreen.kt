// SPDX-FileCopyrightText: ARMSX2 Reader fork contributors (from the PPSSPP Reader fork)
// SPDX-License-Identifier: GPL-3.0+

package com.armsx2.reader.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.armsx2.R

/**
 * Base for the reader's own screens (statistics, dictionaries, Anki setup, backlog, settings):
 * the app theme, a toolbar with a back arrow and a content area kept clear of the system bars.
 */
abstract class ReaderScreen : AppCompatActivity() {
    protected lateinit var toolbar: MaterialToolbar
        private set

    /** Root below the toolbar; subclasses add their content here. */
    protected lateinit var content: LinearLayout
        private set

    protected abstract val titleRes: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            setTheme(R.style.Theme_Reader)
        } catch (_: UnsatisfiedLinkError) {
            // The emulator's settings are in its native library; without it (tests off the
            // device) the theme of the manifest stands.
        }
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface))
        }
        // Laid out as a page of the console's System Configuration: the page's name in its
        // light blue over a thin line, the page, and along the bottom what Back does.
        val accent = MaterialColors.getColor(root, com.google.android.material.R.attr.colorPrimary)
        val line = MaterialColors.getColor(root, com.google.android.material.R.attr.colorOutline)
        toolbar = MaterialToolbar(this).apply {
            setTitle(titleRes)
            setTitleTextColor(accent)
            setNavigationIcon(R.drawable.ic_reader_x_back)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(MATCH, WRAP))
        root.addView(View(this).apply { setBackgroundColor(line) },
            LinearLayout.LayoutParams(MATCH, 1).apply { marginStart = dp(16f); marginEnd = dp(16f) })
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(TextView(this).apply {
            // The console's circle, in its red, and what it does.
            text = android.text.SpannableString("\u25CB  " + getString(R.string.reader_ps2_back)).apply {
                setSpan(android.text.style.ForegroundColorSpan(0xFFFF6E6E.toInt()), 0, 1, 0)
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(MaterialColors.getColor(root, com.google.android.material.R.attr.colorOnSurface))
            gravity = android.view.Gravity.END
            setPadding(dp(24f), dp(8f), dp(28f), dp(10f))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(MATCH, WRAP))
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
    }

    protected fun dp(value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

    /** A vertically scrolling column with standard padding, added to [content]. */
    protected fun scrollingColumn(): LinearLayout {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(8f), dp(16f), dp(24f))
        }
        val scroll = ScrollView(this)
        scroll.addView(column, ViewGroup.LayoutParams(MATCH, WRAP))
        content.addView(scroll, LinearLayout.LayoutParams(MATCH, MATCH))
        return column
    }

    protected fun heading(text: CharSequence) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary))
        setPadding(0, dp(18f), 0, dp(6f))
    }

    protected fun body(text: CharSequence, sizeSp: Float = 15f) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
        setLineSpacing(0f, 1.15f)
    }

    protected fun note(text: CharSequence) = body(text, 13f).apply {
        setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
    }

    protected fun spacer(heightDp: Float) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH, dp(heightDp))
    }

    protected companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
