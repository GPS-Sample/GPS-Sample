package edu.gtri.gpssample.constants

enum class DateFormat(val format : String) {
    None("None"),
    DayMonthYear("dd/mm/yyyy"),
    MonthDayYear("mm/dd/yyyy"),
    YearMonthDay("yyyy/mm/dd"),
}

object DateFormatConverter
{
    val array : Array<String> = Array(4){ i ->
        when(i)
        {
            0 -> DateFormat.DayMonthYear.format
            1 -> DateFormat.MonthDayYear.format
            2 -> DateFormat.YearMonthDay.format
            else -> String()
        }
    }

    fun toIndex(dateFormat : DateFormat) : Int
    {
        return when(dateFormat)
        {
            DateFormat.DayMonthYear -> 1
            DateFormat.MonthDayYear -> 2
            DateFormat.YearMonthDay -> 3
            else -> 0
        }
    }

    fun fromIndex( index : Int) : DateFormat
    {
        return when(index)
        {
            1 -> DateFormat.DayMonthYear
            2 -> DateFormat.MonthDayYear
            3 -> DateFormat.YearMonthDay
            else -> DateFormat.None
        }
    }
    fun fromString( type : String) : DateFormat
    {
        return when(type)
        {
            DateFormat.DayMonthYear.format -> DateFormat.DayMonthYear
            DateFormat.MonthDayYear.format -> DateFormat.MonthDayYear
            DateFormat.YearMonthDay.format -> DateFormat.YearMonthDay
            else -> DateFormat.None
        }
    }

}