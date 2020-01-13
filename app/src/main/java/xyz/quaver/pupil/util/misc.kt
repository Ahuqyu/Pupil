/*
 *     Pupil, Hitomi.la viewer for Android
 *     Copyright (C) 2019  tom5079
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

package xyz.quaver.pupil.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round

//Android Q+ uses scoped storage thus not requiring permission
fun Context.hasPermission(permission: String) =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

@UseExperimental(ExperimentalStdlibApi::class)
fun String.wordCapitalize() : String {
    val result = ArrayList<String>()

    for (word in this.split(" "))
        result.add(word.capitalize(Locale.getDefault()))

    return result.joinToString(" ")
}


//https://discuss.kotlinlang.org/t/how-do-you-round-a-number-to-n-decimal-places/8843(fvasco)
fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

fun byteToString(byte: Long, precision : Int = 1) : String {

    val suffix = listOf(
        "B",
        "kB",
        "MB",
        "GB",
        "TB" //really?
    )
    var size = byte.toDouble()
    var suffixIndex = 0

    while (size >= 1024) {
        size /= 1024
        suffixIndex++
    }

    return "${size.round(precision)} ${suffix[suffixIndex]}"

}