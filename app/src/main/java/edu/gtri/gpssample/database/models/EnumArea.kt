package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable

@Serializable
data class EnumArea (
    var id : Int? = null,
    var config_id: Int,
    var name: String,
    var vertices: ArrayList<LatLon>)
{
    constructor(id: Int, config_id: Int, name: String) : this(id, config_id, name, ArrayList<LatLon>())
    constructor(config_id: Int, name: String, vertices: ArrayList<LatLon>) : this(null, config_id, name, vertices)
}