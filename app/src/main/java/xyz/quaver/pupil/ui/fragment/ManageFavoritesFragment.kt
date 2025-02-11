/*
 *     Pupil, Hitomi.la viewer for Android
 *     Copyright (C) 2020  tom5079
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package xyz.quaver.pupil.ui.fragment

import android.content.Intent
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import okhttp3.*
import xyz.quaver.pupil.R
import xyz.quaver.pupil.client
import xyz.quaver.pupil.util.restore
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

class ManageFavoritesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.manage_favorites_preferences, rootKey)

        initPreferences()
    }

    private fun initPreferences() {
        val context = context ?: return

        findPreference<Preference>("backup")?.setOnPreferenceClickListener {
            val iconSize = (24 * Resources.getSystem().displayMetrics.density).roundToInt()
            val strokeWidth = (3 * Resources.getSystem().displayMetrics.density)

            val icon = object: CircularProgressDrawable(context) {
                override fun getIntrinsicHeight(): Int {
                    return iconSize
                }

                override fun getIntrinsicWidth(): Int {
                    return iconSize
                }
            }

            icon.strokeWidth = strokeWidth
            icon.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.SRC_IN)
            DrawableCompat.setTint(icon, ContextCompat.getColor(context, R.color.colorAccent))
            icon.start()
            it.icon = icon

            val favorites = runCatching {
                Json.parseToJsonElement(File(ContextCompat.getDataDir(context), "favorites.json").readText())
            }.getOrNull()
            val favoriteTags = kotlin.runCatching {
                Json.parseToJsonElement(File(ContextCompat.getDataDir(context), "favorites_tags.json").readText())
            }.getOrNull()

            val request = Request.Builder()
                .url(context.getString(R.string.backup_url))
                .post(
                    FormBody.Builder()
                        .add("f:1", buildJsonObject {
                            favorites?.let {
                                put("favorites", it)
                            }
                            favoriteTags?.let {
                                put("favorite_tags", it)
                            }
                        }.toString())
                        .build()
                ).build()

            client.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val view = view ?: return

                    MainScope().launch {
                        it.icon = null
                    }
                    Snackbar.make(view, R.string.settings_backup_failed, Snackbar.LENGTH_LONG).show()
                }

                override fun onResponse(call: Call, response: Response) {
                    MainScope().launch {
                        it.icon = null
                    }

                    if (response.code() != 200) {
                        response.close()
                        return
                    }

                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, response.body()?.use { it.string() }?.replace("\n", ""))
                    }.let {
                        getContext()?.startActivity(Intent.createChooser(it, getString(R.string.settings_backup_share)))
                    }
                }
            })

            true
        }
        findPreference<Preference>("restore")?.setOnPreferenceClickListener {
            val editText = EditText(context).apply {
                setText(context.getString(R.string.backup_url), TextView.BufferType.EDITABLE)
            }

            AlertDialog.Builder(context)
                .setTitle(R.string.settings_restore_title)
                .setView(editText)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    restore(editText.text.toString(),
                        onFailure = onFailure@{
                            val view = view ?: return@onFailure
                            Snackbar.make(view, R.string.settings_restore_failed, Snackbar.LENGTH_LONG).show()
                        }, onSuccess = onSuccess@{
                            val view = view ?: return@onSuccess
                            Snackbar.make(view, context.getString(R.string.settings_restore_success, it), Snackbar.LENGTH_LONG).show()
                        })
                }.setNegativeButton(android.R.string.cancel) { _, _ ->
                    // Do Nothing
                }.show()

            true
        }
    }

}