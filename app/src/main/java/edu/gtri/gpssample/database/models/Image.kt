@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

@Serializable
data class Image (
    var uuid : String,
    var creationDate : Long,
    var locationUuid: String,
    var data: String )
{
    constructor( locationUuid: String, data: String ) : this(UUID.randomUUID().toString(), Date().time, locationUuid, data )
}
