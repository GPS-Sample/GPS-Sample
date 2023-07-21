package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable

@Serializable
abstract class GeoItem {
    abstract var id : Int?
}