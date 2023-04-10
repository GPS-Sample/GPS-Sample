package edu.gtri.gpssample.viewmodels.models

import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Study
import java.util.*

class CreateFieldModel {

    private var _fieldTypePosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _currentField : MutableLiveData<Field>? = null

    var currentField : LiveData<Field>? = _currentField

    val fieldTypePosition : MutableLiveData<Int>
        get() = _fieldTypePosition

    val fieldTypes : Array<String>
        get() = FieldTypeConverter.array

    fun createNewField()
    {
        val newField = Field( UUID.randomUUID().toString(), "", FieldType.None, false, false, false, false, false, "", "", "", "" )
        _currentField = MutableLiveData(newField)
        currentField = _currentField
    }
    fun addField(currentStudy : Study)
    {
        currentStudy?.let{study ->
            currentField?.value?.let { field ->
                if(!study.fields.contains(field))
                {
                    study.fields.add(field)
                }
            }
        }
    }

    fun setSelectedField(field : Field)
    {
        _currentField = MutableLiveData(field)
        currentField = _currentField
    }


    fun deleteSelectedField(study : Study)
    {
        _currentField?.value?.let{field ->
            study.fields.remove(field)
            DAO.fieldDAO.deleteField(field)
        }
        _currentField = null
    }

    fun onFieldTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        Log.d("TEST","xxxxx")
        if(position < FieldTypeConverter.array.size)
        {
            val fieldType : String = FieldTypeConverter.array[position]
            _currentField?.value?.let {
                it.type = FieldTypeConverter.fromString( fieldType)
            }
        }
    }
    fun onFieldPIISelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        Log.d("xxx", "PII SELECTED CHANGED $isChecked")
        currentField?.value?.let{field ->
            field.pii = isChecked
        }
    }

    fun onFieldRequiredSelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        Log.d("xxx", "Required SELECTED CHANGED $isChecked")
        currentField?.value?.let{field ->
            field.required = isChecked
        }
    }
    fun onFieldIntegerOnlySelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.integerOnly = isChecked
        }
        Log.d("xxx", "Required SELECTED CHANGED $isChecked")
    }
    fun onFieldDateSelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.date = isChecked
        }
        Log.d("xxx", "Required SELECTED CHANGED $isChecked")
    }
    fun onFieldTimeSelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.time = isChecked
        }
        Log.d("xxx", "Required SELECTED CHANGED $isChecked")
    }
}