/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.utils

enum class NetworkConnectionStatus {
    UNKNOWN_STATUS, INITIALIZING, CONNECTING, CONNECTED, PAIRED, NOT_CONNECTED, NOT_PAIRED,
    PAIRED_NOT_AVAILABLE,
    WIFI_NOT_AVAILABLE, PARTIAL_CONNECTION, MISSING_PERMISSIONS
}