package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable

@Serializable
data class FieldData (
    var id : Int? = null,
    var fieldId : Int,
    var enumDataId : Int,
    var response1 : String,
    var response2 : String,
    var response3 : String,
    var response4 : String)
{
    constructor( fieldId: Int, enumDataId: Int, response1: String, response2: String, response3: String, response4: String ) :
            this( null, fieldId, enumDataId, response1, response2, response3, response4 )
}