package edu.gtri.gpssample.database

import android.database.sqlite.SQLiteStatement
import edu.gtri.gpssample.extensions.bind

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

        fun <T> createUpsertSQL( table: String, columns: List<ColumnBinding<T>>): String
        {
            val names = listOf(DAO.COLUMN_UUID) + columns.map { it.name }

            val values = names.joinToString(", ") { "?" }

            val updates = columns.joinToString(", ") {
                "${it.name} = excluded.${it.name}"
            }

            return """
        INSERT INTO $table (
            ${names.joinToString(", ")}
        )
        VALUES (
            $values
        )
        ON CONFLICT(${DAO.COLUMN_UUID})
        DO UPDATE SET
            $updates
        WHERE excluded.${DAO.COLUMN_VERSION} <> ${table}.${DAO.COLUMN_VERSION}
    """.trimIndent()
        }

        fun <T> bindStatement( stmt: SQLiteStatement, uuid: String, columns: List<ColumnBinding<T>>, item: T)
        {
            stmt.clearBindings()

            stmt.bindString(1, uuid)

            columns.forEachIndexed { index, column ->
                stmt.bind(index + 2, column.value(item))
            }
        }
    }
}