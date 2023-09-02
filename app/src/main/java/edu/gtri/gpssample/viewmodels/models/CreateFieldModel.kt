package edu.gtri.gpssample.viewmodels.models

import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Study
import java.util.*
import kotlin.collections.ArrayDeque

class CreateFieldModel
{
    private var _currentField : MutableLiveData<Field>? = null
    private var _currentFieldBlockUUID : MutableLiveData<String>? = null
    private var _fieldTypePosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _fieldType: MutableLiveData<FieldType> = MutableLiveData( FieldType.Text )
    private var _fieldBlockContainer : MutableLiveData<Boolean> = MutableLiveData(false)

    var tempField : MutableLiveData<Field>? = null
    var currentField : LiveData<Field>? = _currentField
    var currentFieldBlockUUID : LiveData<String>? = _currentFieldBlockUUID
    var fieldType : LiveData<FieldType> = _fieldType

    val fieldBlockContainer : MutableLiveData<Boolean>
        get() = _fieldBlockContainer

    val fieldTypePosition : MutableLiveData<Int>
        get() = _fieldTypePosition

    var fieldTypes : Array<String>? = null

    fun createNewField()
    {
        val newField = Field("", FieldType.Text, false, false, false, false, false, "", "", "", "" )
        _currentField = MutableLiveData(newField)
        currentField = _currentField
    }

    fun createNewField( fieldBlockUUID: String )
    {
        val newField = Field("", FieldType.Text, false, false, false, false, false, "", "", "", "" )
        newField.fieldBlockUUID = fieldBlockUUID
        _currentField = MutableLiveData(newField)
        currentField = _currentField
    }

    fun addField(study : Study)
    {
        currentField?.value?.let { field ->

            if(!study.fields.contains(field))
            {
                study.fields.add(field)
            }
        }
    }

    fun setCurrentFieldBlockUUID( blockUUID: String? )
    {
        _currentFieldBlockUUID = MutableLiveData(blockUUID)
        currentFieldBlockUUID = _currentFieldBlockUUID
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
        if(position < FieldTypeConverter.array.size)
        {
            _fieldTypePosition = MutableLiveData(position)
            val type : String = FieldTypeConverter.array[position]
            tempField?.value?.let {
                it.type = FieldTypeConverter.fromString( type )
                _fieldType.value = it.type
            }
        }
    }

    fun onFieldPIISelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        tempField?.value?.let{field ->
            field.pii = isChecked
        }
    }

    fun onFieldRequiredSelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        tempField?.value?.let{field ->
            field.required = isChecked
        }
    }

    fun onFieldBlockContainerSelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        tempField?.value?.let{field ->
            field.fieldBlockContainer = isChecked
            _fieldBlockContainer.postValue(isChecked)
        }
    }

    fun onFieldIntegerOnlySelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        tempField?.value?.let{field ->
            field.integerOnly = isChecked
        }
    }
    fun onFieldDateSelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        tempField?.value?.let{field ->
            field.date = isChecked
        }
    }
    fun onFieldTimeSelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        tempField?.value?.let{field ->
            field.time = isChecked
        }
    }
}