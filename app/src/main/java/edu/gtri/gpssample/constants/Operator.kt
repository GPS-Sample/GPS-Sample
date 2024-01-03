package edu.gtri.gpssample.constants

enum class Operator(val format : String) {
    Equal("Equal"),
    NotEqual("NotEqual"),
    LessThan("LessThan"),
    GreaterThan("GreaterThan"),
    LessThanOrEqual("LessThanOrEqual"),
    GreaterThanOrEqual("GreaterThanOrEqual"),
}


object OperatorConverter
{
    val array : Array<String> = Array(6){ i ->
        when(i)
        {
            0 -> Operator.Equal.format
            1 -> Operator.NotEqual.format
            2 -> Operator.LessThan.format
            3 -> Operator.GreaterThan.format
            4 -> Operator.LessThanOrEqual.format
            5 -> Operator.GreaterThanOrEqual.format
            else -> String()
        }
    }

    fun toIndex(operator : Operator) : Int
    {
        return when(operator)
        {
            Operator.Equal -> 0
            Operator.NotEqual -> 1
            Operator.LessThan -> 2
            Operator.GreaterThan -> 3
            Operator.LessThanOrEqual -> 4
            Operator.GreaterThanOrEqual -> 5
            else -> 0
        }
    }

    fun fromIndex( index : Int) : Operator
    {
        return when(index)
        {
            0 -> Operator.Equal
            1 -> Operator.NotEqual
            2 -> Operator.LessThan
            3 -> Operator.GreaterThan
            4 -> Operator.LessThanOrEqual
            5 -> Operator.GreaterThanOrEqual
            else -> Operator.Equal
        }
    }

    fun fromString( type : String) : Operator
    {
        return when(type)
        {
            Operator.Equal.format -> Operator.Equal
            Operator.NotEqual.format -> Operator.NotEqual
            Operator.LessThan.format -> Operator.LessThan
            Operator.GreaterThan.format -> Operator.GreaterThan
            Operator.LessThanOrEqual.format -> Operator.LessThanOrEqual
            Operator.GreaterThanOrEqual.format -> Operator.GreaterThanOrEqual
            else -> Operator.Equal
        }
    }
}