/*
 * Copyright (c) 2025 Sam Crow
 *
 * This file is part of JRBPSurvey.
 *
 * JRBPSurvey is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * JRBPSurvey is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey.about

import android.os.Bundle
import android.text.Spanned
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import org.samcrow.ridgesurvey.BuildConfig
import org.samcrow.ridgesurvey.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setTitle(getString(
            R.string.about_heading, getString(R.string.app_name), BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        ))

        val textView = findViewById<TextView>(R.id.about_text_view)
        textView.text = getFullAboutText()
    }

    private fun getFullAboutText(): Spanned {
        val html = StringBuilder()

        html.append(getString(R.string.about_text_start))
        html.append(getGpl3())
        html.append("<br />")
        html.append(getString(R.string.about_text_appcompat))
        html.append(getApache2())
        html.append("<br />")
        html.append(getString(R.string.about_text_maplibre))
        html.append(getMapLibreBsd())

        return HtmlCompat.fromHtml(html.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    private fun getGpl3(): CharSequence {
        return resources.openRawResource(R.raw.gpl_3).reader().readText()
    }

    private fun getApache2(): CharSequence {
        return resources.openRawResource(R.raw.apache_2).reader().readText()
    }

    private fun getMapLibreBsd(): CharSequence {
        return resources.openRawResource(R.raw.maplibre_bsd).reader().readText()
    }
}