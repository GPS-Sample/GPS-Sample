package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface GeoItem {
    abstract var id : Int?
}