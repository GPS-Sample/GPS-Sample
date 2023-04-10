package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable

@Serializable
data class FieldData (
    val id : Int? = null,
    val enumDataId : Int,
    val fieldId : Int,
    var response : String)
{
    constructor( enumDataId: Int, fieldId: Int, response: String ) : this( null, enumDataId, fieldId, response )
}