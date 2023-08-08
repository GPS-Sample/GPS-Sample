package edu.gtri.gpssample.database.models


import androidx.databinding.BindingAdapter
import edu.gtri.gpssample.constants.Operator
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Rule(
    var id : Int? = null,
    var field: Field?,
    var name: String,
    var value: String,
    var operator : Operator?,
    var filterOperator: FilterOperator?
    )
{
    constructor( field: Field, name: String, value: String, operator: Operator)
            : this(null, field, name, value, operator, null)

    constructor( field: Field, name: String, value: String)
            : this(null, field, name, value, null, null)


    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    override fun toString() : String
    {
        return this.name
    }
    companion object
    {
        fun unpack( message: String ) : Rule
        {
            return Json.decodeFromString<Rule>( message )
        }
    }
}