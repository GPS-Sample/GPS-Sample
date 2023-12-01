package edu.gtri.gpssample.viewmodels

import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.managers.MapboxManager
import java.lang.Integer.min
import java.util.ArrayList
import kotlin.math.roundToInt

class SamplingViewModel : ViewModel()
{
    private lateinit var mapboxManager: MapboxManager
    private lateinit var pointAnnotationManager: PointAnnotationManager

    private var _currentFragment : Fragment? = null
    private var _currentStudy : MutableLiveData<Study>? = null
    private var _currentSampledItemsForSampling : ArrayList<EnumerationItem> = ArrayList()

    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()

    var currentStudy : LiveData<Study>?
        get(){
          return _currentStudy
        }
        set(value){
            _currentStudy = MutableLiveData(value?.value)
        }

    val samplingMethod: String
        get()
        {
            currentStudy?.value?.let{study ->
                Log.d( "xxx", study.samplingMethod.format)
                _currentFragment?.let{fragment ->
                    return SamplingMethodConverter.internationalString(study.samplingMethod, fragment)
                }

                return study.samplingMethod.format
            }
            return ""
        }

    fun setSampleAreasForMap(mapboxManager: MapboxManager, pointAnnotationManager: PointAnnotationManager) : SamplingState
    {
        this.mapboxManager = mapboxManager
        this.pointAnnotationManager = pointAnnotationManager

        for (pointAnnotation in allPointAnnotations)
        {
            pointAnnotationManager.delete( pointAnnotation )
        }

        allPointAnnotations.clear()

        currentStudy?.value?.let { study ->
            for (sampleArea in study.sampleAreas)
            {
                for (location in sampleArea.locations)
                {
                    if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                    {
                        currentStudy?.value?.let{ study->

                            var resourceId: Int
                            var isMultiFamily = false

                            location.isMultiFamily?.let {
                                isMultiFamily = it
                            }

                            if (!isMultiFamily)
                            {
                                val sampledItem = location.enumerationItems[0]

                                if(!_currentSampledItemsForSampling.contains(sampledItem))
                                {
                                    _currentSampledItemsForSampling.add(sampledItem)
                                }

                                resourceId = when(sampledItem.samplingState)
                                {
                                    SamplingState.None       -> R.drawable.home_black
                                    SamplingState.NotSampled -> R.drawable.home_green
                                    SamplingState.Sampled    -> R.drawable.home_blue
                                    SamplingState.Resampled  -> R.drawable.home_blue
                                    SamplingState.Invalid    -> R.drawable.home_red
                                }
                            }
                            else
                            {
                                resourceId = R.drawable.multi_home_green

                                for (sampledItem in location.enumerationItems)
                                {
                                    if(!_currentSampledItemsForSampling.contains(sampledItem))
                                    {
                                        _currentSampledItemsForSampling.add(sampledItem)
                                    }

                                    if (sampledItem.samplingState == SamplingState.Sampled)
                                    {
                                        resourceId = R.drawable.multi_home_blue
                                    }
                                }
                            }

                            val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                            val pointAnnotation = mapboxManager.addMarker( point, resourceId )

                            pointAnnotation?.let { pointAnnotation ->
                                allPointAnnotations.add( pointAnnotation )
                            }
                        }
                    }
                }
            }
        }

        return SamplingState.None
    }

    fun validateRule(rule : Rule, fieldData : FieldData) : Boolean
    {
        var validRule = false
        fieldData.field?.let{field->
            when (field.type) {
                FieldType.Number -> {
                    // we allow for all of the rule operators
                    // convert string to int
                    try {

                        val ruleNumber = rule.value.toDouble()
                        fieldData.numberValue?.let { number ->
                            rule?.operator?.let { operator ->
                                when (operator) {
                                    Operator.Equal -> {

                                        val epsilon = 0.000001
                                        validRule =
                                            Math.abs(number - ruleNumber) < epsilon

                                    }
                                    Operator.GreaterThan -> {
                                        validRule = (number > ruleNumber)
                                    }
                                    Operator.GreaterThanOrEqual -> {
                                        validRule = (number >= ruleNumber)
                                    }
                                    Operator.LessThanOrEqual -> {
                                        validRule = (number <= ruleNumber)
                                    }
                                    Operator.LessThan -> {
                                        validRule = (number < ruleNumber)
                                    }
                                    Operator.NotEqual -> {
                                        validRule = (number != ruleNumber)
                                    }
                                    else->{
                                        validRule = false
                                    }
                                }
                            }
                        }
                    }catch (ex : Exception){
                        Log.d("XXXXXX", ex.toString())
                    }

                }
                else -> {
                    validRule = fieldData.textValue?.equals(rule.value) ?: false
                }
            }
        }

        return validRule
    }
    fun beginSampling(view : View) : SamplingState
    {
        val validSamples : ArrayList<EnumerationItem> = ArrayList()

        currentStudy?.value?.let { study ->
            for (sampleItem in _currentSampledItemsForSampling)
            {
                sampleItem.samplingState = SamplingState.NotSampled

                // find and remove items that are not valid
                if (sampleItem.enumerationState == EnumerationState.Enumerated)
                {
                    // add in filters
                    var validSample = false
                    for (filter in study.filters)
                    {
                        var validRule = false
                        var validFilterOperator = false
                        filter.rule?.let{rule ->
                            rule.field?.let{field ->
                                Log.d("XXXXXXXX", "the field ${field.name}")
                                for (fieldData in sampleItem.fieldDataList)
                                {
                                    Log.d("XXXXX", "field name ${field.name} == fielddata name ${fieldData.field?.name}")
                                    if( fieldData.field?.name.equals( field.name))
                                    {
                                        validRule = validateRule(rule, fieldData)
                                    }
                                }
                            }
                            var filterOperator : FilterOperator? = rule.filterOperator
                            while(filterOperator != null)
                            {
                                var validNextRule = false

                                // check each rule
                                filterOperator?.let{fo ->
                                    fo.rule?.let{nextRule ->
                                        // check next rule
                                        nextRule.field?.let{field ->
                                            Log.d("XXXXXXXX", "the field ${field.name}")
                                            for (fieldData in sampleItem.fieldDataList)
                                            {
                                                if( fieldData.field?.name.equals( field.name))
                                                {
                                                    validNextRule = validateRule(nextRule, fieldData)
                                                    // check the operator
                                                    if(validNextRule)
                                                    {
                                                        when(fo.conenctor)
                                                        {
                                                            Connector.AND->{
                                                                validFilterOperator = (validRule && validNextRule)
                                                            }
                                                            Connector.OR->{
                                                                validFilterOperator = (validRule || validNextRule)
                                                            }
                                                            Connector.NOT->{
                                                                validFilterOperator = (validRule && !validNextRule)
                                                            }
                                                            else->{
                                                                validFilterOperator = false
                                                            }
                                                        }
                                                    }
                                                    if(!validNextRule || !validFilterOperator)
                                                    {
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        // check constraint
                                        filterOperator = nextRule.filterOperator
                                    }
                                }?:run{
                                    filterOperator = null
                                }

                            }
                        }
                        validSample = validRule && validFilterOperator
                        if(!validSample)
                        {
                            break;
                        }
                    }
                    if(validSample)
                    {
                        validSamples.add(sampleItem)
                    }

                }
            }

            currentStudy?.value?.let { study ->
                var sampleSize = 0

                when (study.sampleType)
                {
                    SampleType.NumberHouseholds ->
                    {
                        sampleSize =  min(study.sampleSize,validSamples.size)
                    }
                    SampleType.PercentHouseholds ->
                    {
                        sampleSize = (study.sampleSize.toDouble() / 100.0 * validSamples.size.toDouble()).roundToInt()
                    }
                    SampleType.PercentTotal ->
                    {
                        sampleSize = (study.sampleSize.toDouble() / 100.0 * study.totalPopulationSize.toDouble()).roundToInt()
                    }
                    else -> {}
                }

                if (sampleSize > 0)
                {
                    val sampledIndices = ArrayList<Int>()

                    for (i in 0 until sampleSize)
                    {
                        var rnds = (0 until validSamples.size).random()

                        while(sampledIndices.contains(rnds))
                        {
                            rnds = (0 until validSamples.size).random()
                        }

                        sampledIndices.add(rnds)
                        validSamples[rnds].samplingState = SamplingState.Sampled
                    }
                }
            }
        }

        setSampleAreasForMap(mapboxManager,pointAnnotationManager)

        return SamplingState.None
    }
}