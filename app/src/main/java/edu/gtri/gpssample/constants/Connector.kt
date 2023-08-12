package edu.gtri.gpssample.constants

enum class Connector(val format : String) {
    NONE("NONE"),
    AND("AND"),
    OR("OR"),
    NOT("NOT"),
}

object ConnectorConverter
{

    // array indicies start at zero and we want to exclude the NONE option
    val array : Array<String> = Array(3) { i ->
        when (i) {

            0 -> Connector.AND.format
            1 -> Connector.OR.format
            2 -> Connector.NOT.format
            else -> String()
        }
    }

    fun toIndex(conenctor : Connector) : Int
    {
        return when(conenctor)
        {
            Connector.AND -> 1
            Connector.OR -> 2
            Connector.NOT -> 3
            else -> 0
        }
    }
    fun fromIndex( index : Int) : Connector
    {
        return when(index)
        {
            1 -> Connector.AND
            2 -> Connector.OR
            3 -> Connector.NOT

            else -> Connector.NONE
        }
    }

    fun fromString( type : String) : Connector
    {
        return when(type)
        {
            Connector.NONE.format -> Connector.NONE
            Connector.AND.format -> Connector.AND
            Connector.OR.format ->  Connector.OR
            Connector.NOT.format -> Connector.NOT
            else -> Connector.NONE
        }
    }
}