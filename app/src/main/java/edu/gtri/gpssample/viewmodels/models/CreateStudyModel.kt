package edu.gtri.gpssample.viewmodels.models

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import androidx.databinding.Bindable
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.*
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
    private var _samplingMethod: MutableLiveData<SamplingMethod> = MutableLiveData( SamplingMethod.SimpleRandom )

    var samplingMethod : LiveData<SamplingMethod> = _samplingMethod

    var fragment : Fragment? = null
    var sampleTypesVisibility : ObservableBoolean = ObservableBoolean(true)//MutableLiveData<Int> = MutableLiveData(View.GONE)
    var totalPopulationVisibility : ObservableBoolean = ObservableBoolean(true)//MutableLiveData<Int> = MutableLiveData(View.GONE)

    val samplingMethodPosition : MutableLiveData<Int>
        get() = _samplingMethodPosition
    val samplingTypePosition : MutableLiveData<Int>
        get() = _samplingTypePosition

    val samplingMethods : Array<String>
        get(){
            val englishArray = SamplingMethodConverter.array
            fragment?.let { fragment ->

                val array: Array<String> = Array(englishArray.size)
                { i ->
                    when (i) {

                        0 -> fragment.getString(R.string.simple_random)
                        1 -> fragment.getString(R.string.cluster_sampling)
                        2 -> fragment.getString(R.string.subset_overlap)
                        3 -> fragment.getString(R.string.strata_exclusive)
                        else -> String()
                    }
                }
                return array
            }
            return englishArray

        }

    val sampleTypes : Array<String>
        get(){
            val englishArray = SampleTypeConverter.array
            fragment?.let { fragment ->

                val array: Array<String> = Array(englishArray.size)
                { i ->
                    when (i) {

                        0 -> fragment.getString(R.string.numberhouseholds)
                        1 -> fragment.getString(R.string.percenthouseholds)
                        2 -> fragment.getString(R.string.percenttotal)
                        else -> String()
                    }
                }
                return array
            }
            return englishArray

        }

    val fieldList : Array<String>
        get() = getFields()

    val ruleList : Array<String>
        get() = getRules()

    var currentStudy : LiveData<Study>? = _currentStudy

//    var currentSampleSize : MutableLiveData<String> = MutableLiveData("")
//        set(value)
//        {
//            currentStudy?.value?.let{study ->
//
//                value.value?.toIntOrNull()?.let {size ->
//                    if(size > 0)
//                    {
//                        study.sampleSize = size
//                        field = value
//                    }else
//                    {
//                        currentSampleSize.postValue("1")
//                    }
//                } ?: run{ study.sampleSize = 0
//                }
//            }
//
//        }

    var  currentSampleSize : String
        get(){
            currentStudy?.value?.let{study ->
                return study.sampleSize.toString()
            }
            return ""
        }
        set(value){
            currentStudy?.value?.let{study ->
                value.toIntOrNull()?.let {size ->
                    if(size > 0)
                    {
                        study.sampleSize = size
                    }
                } ?: run{ study.sampleSize = 0
                }
            }
        }

    var  totalPopulationSize : String
        get(){
            currentStudy?.value?.let{ study ->
                return study.totalPopulationSize.toString()
            }
            return ""
        }
        set(value) {
            currentStudy?.value?.let{ study ->
                value.toIntOrNull()?.let{ size ->
                    study.totalPopulationSize = size
                }
            }
        }

    constructor()

    fun getFields() : Array<String>
    {
        val fieldList = ArrayList<String>()

        _currentStudy?.value?.fields?.let { fields ->
            for (field in fields)
            {
                fieldList.add( field.name )
            }
        }

        return fieldList.toTypedArray()
    }

    private fun getRules() : Array<String>
    {
        val ruleList = ArrayList<String>()
        _currentStudy?.value?.rules?.let { rules ->
            for (rule in rules)
            {
                ruleList.add( rule.name )
            }
        }

        return ruleList.toTypedArray()
    }

    fun onSamplingMethodSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position < samplingMethods.size)
        {
            _currentStudy?.value?.let {study ->
                study.samplingMethod = SamplingMethodConverter.fromArrayPosition(position)
                _samplingMethod.value = study.samplingMethod

                _samplingTypes.clear()

                when(study.samplingMethod)
                {
                    SamplingMethod.SimpleRandom -> {
                        sampleTypesVisibility.set(true)
                    }
                    SamplingMethod.Cluster -> {
                        sampleTypesVisibility.set(true)
                    }
                    else -> {sampleTypesVisibility.set(false)}
                }
            }
        }
    }

    fun onSampleTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position < sampleTypes.size)
        {
            val sampleType : String = SampleTypeConverter.array[position]
            _currentStudy?.value?.let { study ->
                study.sampleType = SampleTypeConverter.fromString(sampleType)

                if (study.sampleType == SampleType.PercentTotal)
                {
                    totalPopulationVisibility.set(true)
                }
                else
                {
                    totalPopulationVisibility.set(false)
                }
            }
        }
    }

    fun createNewStudy()
    {
        val study = Study(
            "",
            SamplingMethod.None,
            0,
            SampleType.None
        )
        _currentStudy = MutableLiveData(study)
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
        currentStudy?.value?.let{ study ->
            configuration?.let { config ->
                if(!config.studies.contains(study))
                {
                    config.studies.add(study)

                }
                DAO.studyDAO.createOrUpdateStudy(study)
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