/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels.models

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

class CreateFieldModel
{
    private var _parentField : MutableLiveData<Field>? = null
    private var _currentField : MutableLiveData<Field>? = null
    private var _fieldTypePosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _fieldType: MutableLiveData<FieldType> = MutableLiveData( FieldType.Text )

    var currentField : LiveData<Field>? = _currentField
    var parentField : LiveData<Field>? = _parentField
    var fieldType : LiveData<FieldType> = _fieldType

    val fieldTypePosition : MutableLiveData<Int>
        get() = _fieldTypePosition

    var fieldTypes : Array<String>? = null

    fun setParentField(field : Field)
    {
        _parentField = MutableLiveData(field)
        parentField = _parentField
    }

    fun setCurrentField(field : Field)
    {
        _currentField = MutableLiveData(field)
        currentField = _currentField
    }

    fun deleteCurrentField(study : Study)
    {
        _currentField?.value?.let { currentField ->
            if (study.fields.contains( currentField ))
            {
                study.fields.remove( currentField )
            }

            for (field in study.fields)
            {
                field.fields?.let { fields ->
                    if (fields.contains( currentField ))
                    {
                        fields.remove( currentField )
                    }
                }
            }

            DAO.fieldDAO.deleteField( currentField )

            // renumber all fields

            for (i in 1..study.fields.size)
            {
                study.fields[i-1].index = i
                study.fields[i-1].fields?.let { fields ->
                    for (j in 1..fields.size)
                    {
                        fields[j-1].index = j
                    }
                }
            }
        }

        _currentField = null
    }

    fun onFieldTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position < FieldTypeConverter.array.size)
        {
            _fieldTypePosition = MutableLiveData(position)
            val type : String = FieldTypeConverter.array[position]
            currentField?.value?.let {
                it.type = FieldTypeConverter.fromString( type )
                _fieldType.value = it.type
            }
        }
    }

    fun onFieldPIISelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.pii = isChecked
        }
    }

    fun onFieldRequiredSelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.required = isChecked
        }
    }

    fun onFieldIntegerOnlySelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.integerOnly = isChecked
        }
    }

    fun onFieldDateSelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.date = isChecked
        }
    }

    fun onFieldTimeSelected(buttonView : CompoundButton, isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.time = isChecked
        }
    }
}