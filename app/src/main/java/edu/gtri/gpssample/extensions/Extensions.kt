/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.extensions

import java.text.DateFormat
import java.util.Date

fun Int.toBoolean() = this == 1

fun Boolean.toInt() = if (this) 1 else 0


fun Date.toLocalizedDateTimeString() : String
{
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT ).format(this )
}
