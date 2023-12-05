package edu.gtri.gpssample.database.models


import androidx.databinding.BindingAdapter
import edu.gtri.gpssample.constants.Operator
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class Rule(
    var id : Int? = null,
    var uuid : String,
    var field: Field?,
    var name: String,
    var value: String,
    var operator : Operator?,
    var filterOperator: FilterOperator?
    )
{
    constructor( field: Field, name: String, value: String, operator: Operator)
            : this(null, UUID.randomUUID().toString(), field, name, value, operator, null)

    constructor( field: Field, name: String, value: String)
            : this(null, UUID.randomUUID().toString(), field, name, value, null, null)

    constructor(id : Int?, field: Field?, name: String, value: String, operator: Operator, filterOperator: FilterOperator?)
            : this(id, UUID.randomUUID().toString(), field, name, value, operator, filterOperator)

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    fun copy() : Rule?
    {
        val _copy = unpack(pack())

        _copy?.let { _copy ->
            return _copy
        }
        return null
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