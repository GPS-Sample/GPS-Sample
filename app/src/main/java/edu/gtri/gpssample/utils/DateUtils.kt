package edu.gtri.gpssample.utils

import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.TimeFormat
import java.util.*

object DateUtils
{
 fun day( date: Date ): Int
 {
   val calendar = Calendar.getInstance()
   calendar.time = date
   return calendar[Calendar.DAY_OF_MONTH]
 }

 fun month( date: Date ): Int
 {
  val calendar = Calendar.getInstance()
  calendar.time = date
  return calendar[Calendar.MONTH] + 1
 }

 fun year( date: Date ): Int
 {
  val calendar = Calendar.getInstance()
  calendar.time = date
  return calendar[Calendar.YEAR]
 }

 fun hour( date: Date ): Int
 {
  val calendar = Calendar.getInstance()
  calendar.time = date
  return calendar[Calendar.HOUR_OF_DAY]
 }

 fun minute( date: Date ): Int
 {
  val calendar = Calendar.getInstance()
  calendar.time = date
  return calendar[Calendar.MINUTE]
 }

 fun second( date: Date ): Int
 {
  val calendar = Calendar.getInstance()
  calendar.time = date
  return calendar[Calendar.SECOND]
 }

 fun dateString(date: Date, dateFormat: DateFormat): String
 {
  val day: Int = day(date)
  val year: Int = year(date)
  val month: Int = month(date)

  when (dateFormat)
  {
   DateFormat.DayMonthYear -> return String.format("%02d/%02d/%d", day, month, year )
   DateFormat.MonthDayYear -> return String.format("%02d/%02d/%d", month, day, year )
   DateFormat.YearMonthDay -> return String.format("%02d/%02d/%d", year, month, day )
   else -> return "${day}/${month}/${year}"
  }
 }

 fun timeString(date: Date, timeFormat: TimeFormat): String
 {
  var hour = hour(date)
  val minute = minute(date)

  var meridiem = "am"

  if (hour >= 12)
  {
   meridiem = "pm"
  }

  if (timeFormat == TimeFormat.twelveHour && hour > 12)
  {
   hour -= 12;
  }

  when (timeFormat)
  {
   TimeFormat.twelveHour -> return String.format("%d:%02d %s", hour, minute, meridiem)
   TimeFormat.twentyFourHour -> return String.format("%d:%02d", hour, minute)
   else -> return String.format("%d:%02d %s", hour, minute, meridiem)
  }
 }

 fun dateTimeString(date: Date, dateFormat: DateFormat, timeFormat: TimeFormat): String
 {
  return dateString(date,dateFormat) + " " + timeString(date,timeFormat)
 }

 fun clearTime(date: Date): Date
 {
  val calendar = Calendar.getInstance()
  calendar.time = date
  calendar[Calendar.HOUR_OF_DAY] = 0
  calendar[Calendar.MINUTE] = 0
  calendar[Calendar.SECOND] = 0
  calendar[Calendar.MILLISECOND] = 0
  return calendar.time
 }

 fun clearDate(date: Date): Date
 {
  val calendar = Calendar.getInstance()
  calendar.time = date
  calendar[Calendar.YEAR] = 0
  calendar[Calendar.MONTH] = 0
  calendar[Calendar.DAY_OF_MONTH] = 0
  return calendar.time
 }
}