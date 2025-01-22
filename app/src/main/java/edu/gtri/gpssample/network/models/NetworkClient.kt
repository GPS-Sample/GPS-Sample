/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.network.models

import java.net.Socket

data class NetworkClient(val dataSocket : Socket, val id : String, val name : String ) {
    var timer : Int = 0
}