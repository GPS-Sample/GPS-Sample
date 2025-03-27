/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

enum class Keys( val value: String ) {
    kRole("Role"),
    kSSID("SSID"),
    kPass("Pass"),
    kError("Error"),
    kPayload("Payload"),
    kRequest("Request"),
    kEditMode("EditMode"),
    kUserName("UserName"),
    kIpAddress("IpAddress"),
    kMapStyle("MAPBOX_STREETS"),
    kMBTilesPath("MBTilesPath" ),
    kIsOnBoarding("IsOnBoarding"),
    kTermsAccepted("TermsAccepted"),
    kCollectionMode("CollectionMode"),
    kStartSubaddress( "StartSubaddress"),
    kIsMultiHousehold("IsMultiHousehold"),
    kGpsAccuracyIsGood("GpsAccuracyIsGood"),
    kGpsLocationIsGood("GpsLocationIsGood"),
    kLaunchSurveyRequest("LaunchSurveyRequest"),
    kAdditionalInfoRequest("AdditionalInfoRequest"),
    kFragmentResultListener("FragmentResultListener")
}