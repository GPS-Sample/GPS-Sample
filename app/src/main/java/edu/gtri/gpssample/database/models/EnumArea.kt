package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class EnumArea (
    var id : Int? = null,
    var config_id: Int,
    var name: String,
    var topLeft: LatLng,
    var topRight: LatLng,
    var botRight: LatLng,
    var botLeft: LatLng)
{
    constructor(config_id: Int, name: String, topLeft: LatLng, topRight: LatLng, botRight: LatLng, botLeft: LatLng) :
            this(null, config_id, name, topLeft, topRight, botRight, botLeft )
}