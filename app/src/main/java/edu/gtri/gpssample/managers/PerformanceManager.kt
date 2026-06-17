package edu.gtri.gpssample.managers

import android.util.Log
import java.util.Date

class PerformanceManager
{
    companion object
    {
        private var startTime: Long = 0

        fun startTimer()
        {
            startTime = System.currentTimeMillis() / 1000
        }

        fun elapsedTime() : String
        {
            val duration= System.currentTimeMillis() / 1000 - startTime
            val minutes = duration / 60
            val seconds = duration % 60
            return "%d:%02d".format(minutes,seconds)
        }
    }
}