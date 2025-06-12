/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database.models

import android.util.Log
import edu.gtri.gpssample.constants.LocationType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class Location(
    var uuid : String,
    var creationDate: Long,
    var timeZone: Int,
    var distance : Double,      // not stored in DB!
    var distanceUnits: String,  // not stored in DB!
    var type : LocationType,
    var gpsAccuracy : Int,
    var latitude : Double,
    var longitude : Double,
    var altitude : Double,
    var isLandmark: Boolean,
    var description: String,
    var imageData: String,
    var isMultiFamily : Boolean, // not used since build #106
    var properties : String,
    var enumerationItems: ArrayList<EnumerationItem>)
{
    constructor( latitude: Double, longitude: Double, altitude: Double ) :
            this( UUID.randomUUID().toString(), Date().time, 0, 0.0, "", LocationType.None, 0, latitude, longitude, altitude, false, "","", false, "", ArrayList<EnumerationItem>())

    constructor( timeZone: Int, type: LocationType, gpsAccuracy: Int, latitude: Double, longitude: Double, altitude: Double, isLandmark: Boolean, description: String, properties: String ) :
            this( UUID.randomUUID().toString(), Date().time, timeZone, 0.0, "", type, gpsAccuracy, latitude, longitude, altitude, isLandmark, description,"", false, properties, ArrayList<EnumerationItem>())

    fun equals( other: Location ) : Boolean
    {
        if (this.creationDate > other.creationDate) // new date is newer thant old date, should update
        {
            return false
        }
        else
        {
            return true // no need to update
        }
    }

    fun doesNotEqual( location: Location ): Boolean
    {
        return !this.equals( location )
    }

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : Location
        {
            return Json.decodeFromString<Location>( string )
        }
    }
}