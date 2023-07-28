package edu.gtri.gpssample.constants

enum class LocationType(val format : String) {
    None("None"),
    Sample("Sample"),
    Enumeration("Enumeration"),

}

object LocationTypeConverter
{
    val array : Array<String> = Array(4){ i ->
        when(i)
        {
            0 -> LocationType.Sample.format
            1 -> LocationType.Enumeration.format
            else -> String()
        }
    }

    fun toIndex(locationType : LocationType) : Int
    {
        return when(locationType)
        {
            LocationType.Sample -> 1
            LocationType.Enumeration -> 2
            else -> 0
        }
    }

    fun fromIndex( index : Int) : LocationType
    {
        return when(index)
        {
            1 -> LocationType.Sample
            2 -> LocationType.Enumeration

            else -> LocationType.None
        }
    }

    fun toArrayPosition(locationType: LocationType) : Int
    {
        return when(locationType)
        {
            LocationType.Sample -> 0
            LocationType.Enumeration -> 1
            else -> 0
        }
    }
    fun fromString( type : String) : LocationType
    {
        return when(type)
        {
            LocationType.Sample.format -> LocationType.Sample
            LocationType.Enumeration.format -> LocationType.Enumeration

            else -> LocationType.None
        }
    }
}