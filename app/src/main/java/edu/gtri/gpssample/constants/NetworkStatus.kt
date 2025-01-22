/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

enum class NetworkStatus(val format : String) {
    None("None"),
    NetworkConnected("Network Connected"),
    NetworkCreated("Network Created"),
    NetworkError("Network Error"),
    DataReceived("Data Received"),
    DataReceivedError("Data Received Error"),
    ServerCreated("Server Created"),
    ServerError("Server Error"),
    QRCodeCreated("QRCode Created"),
    QRCodeError("QRCode Error"),
    ClientRegistered("Client Registered"),
    ClientRegisterError("Client Connected"),
    CommandSent("Command Sent"),
    CommandError("Command Error"),


}

object NetworkStatusConverter {

}