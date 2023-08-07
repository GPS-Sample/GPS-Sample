package edu.gtri.gpssample.constants

enum class Connector(val format : String) {

    None("none"),
    AND("AND"),
    OR("OR"),
    NOT("NOT"),
}

object ConnectorConverter
{

    // array indicies start at zero and we want to exclude the NONE option
    val array : Array<String> = Array(4) { i ->
        when (i) {
            0 -> Connector.None.format
            1 -> Connector.AND.format
            2 -> Connector.OR.format
            3 -> Connector.NOT.format
            else -> String()
        }
    }

    fun toIndex(conenctor : Connector) : Int
    {
        return when(conenctor)
        {
            Connector.None -> 0
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
            0 -> Connector.None
            1 -> Connector.AND
            2 -> Connector.OR
            3 -> Connector.NOT

            else -> Connector.None
        }
    }

    fun fromString( type : String) : Connector
    {
        return when(type)
        {
            Connector.None.format -> Connector.None
            Connector.AND.format -> Connector.AND
            Connector.OR.format ->  Connector.OR
            Connector.NOT.format -> Connector.NOT
            else -> Connector.None
        }
    }
}