/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.extensions

fun Int.toBoolean() = this == 1

fun Boolean.toInt() = if (this) 1 else 0
