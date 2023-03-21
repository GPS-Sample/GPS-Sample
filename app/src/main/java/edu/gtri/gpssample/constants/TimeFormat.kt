package edu.gtri.gpssample.constants

enum class TimeFormat(val format : String) {
    none("None"),
    twelveHour("12 hour (AM/PM)"),
    twentyFourHour("24 hour"),
}

object TimeFormatConverter
{
    fun toIndex(timeFormat : TimeFormat) : Int
    {
        return when(timeFormat)
        {
            TimeFormat.twelveHour -> 1
            TimeFormat.twentyFourHour -> 2
            else -> 0
        }
    }
    fun fromIndex( index : Int) : TimeFormat
    {
        return when(index)
        {
            1 -> TimeFormat.twelveHour
            2 -> TimeFormat.twentyFourHour
            else -> TimeFormat.none
        }
    }
}