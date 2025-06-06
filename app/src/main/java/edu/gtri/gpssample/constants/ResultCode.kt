/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

enum class ResultCode( val value: Int ) {
    GenerateBarcode(1001 ),
    BarcodeScanned( 1002 ),
    ConfigurationCreated( 1003 )
}