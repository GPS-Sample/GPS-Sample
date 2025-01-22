/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

enum class ClientMode(val format : String) {
    None("None"),
    Configuration("Configuration Client"),
    EnumerationTeam("Enumeration Team Client"),
    CollectionTeam( "Connection Team Client")

}

object ClientModeConverter {

}