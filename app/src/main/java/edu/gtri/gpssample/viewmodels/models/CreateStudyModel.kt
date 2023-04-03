package edu.gtri.gpssample.viewmodels.models

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import androidx.databinding.Bindable
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.constants.SamplingMethod
import edu.gtri.gpssample.constants.SamplingMethodConverter
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Study
import java.util.*

class CreateStudyModel {
    private var _samplingMethodPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _samplingTypePosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _currentStudy : MutableLiveData<Study>? = null

    private var _samplingTypes : ObservableArrayList<String> = ObservableArrayList()
    private var _samplingTypesVisible : Boolean = false

    var sampleTypesVisibility : ObservableBoolean = ObservableBoolean(true)//MutableLiveData<Int> = MutableLiveData(View.GONE)

    val samplingMethodPosition : MutableLiveData<Int>
        get() = _samplingMethodPosition
    val samplingTypePosition : MutableLiveData<Int>
        get() = _samplingTypePosition

    val samplingMethods : Array<String>
        get() = SamplingMethodConverter.array

    val sampleTypes : ObservableArrayList<String>
        get() = _samplingTypes//SampleTypeConverter.array

    var currentStudy : LiveData<Study>? = _currentStudy

    var currentSampleSize : String
        get(){
            currentStudy?.value?.let{study ->
                return study.sampleSize.toString()
            }
            return ""
        }
        set(value){
            currentStudy?.value?.let{study ->
                value.toIntOrNull()?.let {size ->
                    study.sampleSize = size
                } ?: run{ study.sampleSize = 0
                }
            }
        }

    fun onSamplingMethodSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position < samplingMethods.size)
        {
            val samplingMethod : String = samplingMethods[position]


            _currentStudy?.value?.let {study ->
                study.samplingMethod = SamplingMethodConverter.fromString(samplingMethod)


                _samplingTypes.clear()
                when(samplingMethod)
                {
                    SamplingMethod.SimpleRandom.format -> {
                        _samplingTypes.addAll(SampleTypeConverter.array)
                        sampleTypesVisibility.set(true)
                    }
                    SamplingMethod.Cluster.format -> {
                        _samplingTypes.addAll(SampleTypeConverter.array)
                        sampleTypesVisibility.set(true)
                    }
                    else -> {sampleTypesVisibility.set(false)}

                }
                //
            }
        }
    }

    fun onSampleTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position > sampleTypes.size)
        {
            val sampleType : String = SampleTypeConverter.array[position]
            _currentStudy?.value?.let {
                it.sampleType = SampleTypeConverter.fromString(sampleType)
            }
        }

    }

    fun createNewStudy()
    {
        val newStudy = Study(
            UUID.randomUUID().toString(),
            "",
            "",
            SamplingMethod.None,
            0,
            SampleType.None
        )
        _currentStudy = MutableLiveData(newStudy)
        currentStudy = _currentStudy
    }

    fun setStudy(study : Study)
    {
        _currentStudy = MutableLiveData(study)
        currentStudy = _currentStudy
        _samplingMethodPosition.value = SamplingMethodConverter.toArrayPosition( study.samplingMethod)
        _samplingTypePosition.value = SampleTypeConverter.toArrayPosition(study.sampleType)
    }

    fun addStudy(configuration : Config?)
    {
        currentStudy?.value?.let{study ->
            configuration?.let { config ->
                if(!config.studies.contains(study))
                {
                    config.studies.add(study)
                }
            }
        }
    }

    fun removeStudy(study: Study?, configuration : Config?)
    {
        study?.let { study ->
            configuration?.let{config ->
                config.studies.remove(study)

            }
            DAO.studyDAO.deleteStudy(study)
        }
    }

    fun deleteCurrentStudy( configuration: Config?) : Boolean
    {
        _currentStudy?.value?.let{study ->
            DAO.studyDAO.deleteStudy(study)
            configuration?.let { config->
                config.studies.remove(study)
            }
            _currentStudy = null
            return true
        }
        return false
    }
}