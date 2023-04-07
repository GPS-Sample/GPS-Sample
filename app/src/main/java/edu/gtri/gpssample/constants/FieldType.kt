package edu.gtri.gpssample.constants
import java.util.*
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class FieldType (val format : String) {
    Text("Text"),
    Number("Number"),
    Date("Date"),
    Checkbox("Checkbox"),
    Dropdown("Dropdown"),
}


object FieldTypeConverter
{


    val array : Array<String> = Array(5) { i ->
        when (i) {
            0 -> FieldType.Text.format
            1 -> FieldType.Number.format
            2 -> FieldType.Date.format
            3 -> FieldType.Checkbox.format
            4 -> FieldType.Dropdown.format
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
            else -> FieldType.Text
        }
    }
}