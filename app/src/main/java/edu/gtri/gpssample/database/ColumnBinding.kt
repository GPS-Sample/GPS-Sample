package edu.gtri.gpssample.database

import android.database.sqlite.SQLiteStatement

data class ColumnBinding<T>(
    val name: String,
    val type: String,
    val value: (T) -> Any?
)
{
    companion object
    {
        fun <T> generateColumnDefs( columnBindings: List<ColumnBinding<T>> ) : String
        {
            return columnBindings.joinToString(", ") { "${it.name} ${it.type}" }
        }
    }
}