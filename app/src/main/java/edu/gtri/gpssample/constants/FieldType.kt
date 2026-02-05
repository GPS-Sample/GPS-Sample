/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

enum class FieldType (val format : String) {
    Text("Text"),
    Number("Number"),
    Date("Date"),
    Checkbox("Checkbox"),
    Dropdown("Dropdown"),
    Note("Note" ),
    None("None")
}

object FieldTypeConverter
{
    val array : Array<String> = Array(6) { i ->
        when (i) {
            0 -> FieldType.Text.format
            1 -> FieldType.Number.format
            2 -> FieldType.Date.format
            3 -> FieldType.Checkbox.format
            4 -> FieldType.Dropdown.format
            5 -> FieldType.Note.format
            else -> String()
        }
    }

    fun toIndex(fieldType : FieldType) : Int
    {
        return when(fieldType)
        {
            FieldType.Text -> 0
            FieldType.Number -> 1
            FieldType.Date -> 2
            FieldType.Checkbox -> 3
            FieldType.Dropdown -> 4
            FieldType.Note -> 5
            FieldType.None -> 6
        }
    }
    fun fromIndex( index : Int) : FieldType
    {
        return when(index)
        {
            0 -> FieldType.Text
            1 -> FieldType.Number
            2 -> FieldType.Date
            3 -> FieldType.Checkbox
            4 -> FieldType.Dropdown
            5 -> FieldType.Note
            6 -> FieldType.None
            else -> FieldType.Text
        }
    }

    fun fromString( type : String) : FieldType
    {
        return when(type)
        {
            FieldType.Text.format -> FieldType.Text
            FieldType.Number.format -> FieldType.Number
            FieldType.Date.format -> FieldType.Date
            FieldType.Checkbox.format -> FieldType.Checkbox
            FieldType.Dropdown.format -> FieldType.Dropdown
            FieldType.Note.format -> FieldType.Note
            FieldType.None.format -> FieldType.None
            else -> FieldType.None
        }
    }
}