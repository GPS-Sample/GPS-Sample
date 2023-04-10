package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable

@Serializable
data class FieldData (
    var id : Int? = null,
    var enumDataId : Int,
    var fieldId : Int,
    var response : String)
{
    constructor( enumDataId: Int, fieldId: Int, response: String ) : this( null, enumDataId, fieldId, response )
}