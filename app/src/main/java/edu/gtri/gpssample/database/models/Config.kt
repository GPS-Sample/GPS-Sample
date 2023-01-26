package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.TimeFormat

class Config
{
    var id: Int = -1
    var name: String = ""
    var dateFormat: DateFormat = DateFormat.DayMonthYear
    var timeFormat: TimeFormat = TimeFormat.twelveHour
    var distanceFormat: DistanceFormat = DistanceFormat.Feet
    var minGpsPrecision: Int = 0
}