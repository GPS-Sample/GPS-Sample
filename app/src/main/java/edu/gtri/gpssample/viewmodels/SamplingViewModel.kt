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

class SamplingViewModel : ViewModel()
{
    private lateinit var mapboxManager: MapboxManager
    private lateinit var pointAnnotationManager: PointAnnotationManager

    private var _currentFragment : Fragment? = null
    private var _currentStudy : MutableLiveData<Study>? = null
    private var _currentSampleArea : MutableLiveData<SampleArea>? = null
    private var _currentSampledItemsForSampling : ArrayList<EnumerationItem> = ArrayList()

    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()

    var currentStudy : LiveData<Study>?
        get(){
          return _currentStudy
        }
        set(value){
            _currentStudy = MutableLiveData(value?.value)
        }

    var currentSampleArea : LiveData<SampleArea>?
        get(){
            return _currentSampleArea
        }
        set(value){
            value?.let{sampleArea ->
                _currentSampleArea = MutableLiveData(sampleArea.value)
                _currentStudy?.value?.let{ study->
                    if (study.sampleArea == null)  // is this check necessary / correct?
                    {
                        sampleArea.value?.let{ sampleArea->
                            study.sampleArea = sampleArea
                        }
                    }
                }
            }
        }

    fun setCurrentSampleArea( sampleArea: SampleArea )
    {
        _currentSampleArea = MutableLiveData( sampleArea )
        currentSampleArea = _currentSampleArea
    }

    val samplingMethod: String
        get()
        {
            currentStudy?.value?.let{study ->
                _currentFragment?.let{fragment ->
                    return SamplingMethodConverter.internationalString(study.samplingMethod, fragment)
                }

                return study.samplingMethod.format
            }
            return ""
        }

    fun createSampleArea(fromEnumArea: EnumArea)
    {
        val sampleArea = SampleArea(fromEnumArea)
        _currentSampleArea = MutableLiveData(sampleArea)
        _currentStudy?.value?.let { study ->
            study.sampleArea = sampleArea
        }
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

        _currentSampleArea?.value?.let{ sampleArea->

            for (location in sampleArea.locations)
            {
                if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                {
                    currentStudy?.value?.let{ study->

                        // assuming only 1 enumeration item per location, for now...
                        val sampledItem = location.enumerationItems[0]

                        if(!_currentSampledItemsForSampling.contains(sampledItem))
                        {
                            _currentSampledItemsForSampling.add(sampledItem)
                        }

                        Log.d( "xxx", sampledItem.samplingState.format )

                        val color = when(sampledItem.samplingState)
                        {
                            SamplingState.None       -> R.drawable.home_black
                            SamplingState.NotSampled -> R.drawable.home_grey
                            SamplingState.Sampled    -> R.drawable.home_green
                            SamplingState.Resampled  -> R.drawable.home_green
                            SamplingState.Invalid    -> R.drawable.home_red
                        }

                        val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                        val pointAnnotation = mapboxManager.addMarker( point, color )

                        pointAnnotation?.let { pointAnnotation ->
                            allPointAnnotations.add( pointAnnotation )
                        }
                    }
                }
            }
        }

        return SamplingState.None
    }

    fun beginSampling(view : View) : SamplingState
    {
        fixEnumData()
        // reset list
        val validSamples : ArrayList<EnumerationItem> = ArrayList()

        currentStudy?.value?.let { study ->

/// TEST
            for(filter in study.filters)
            {
                Log.d("XXXXXX", filter.name )
//                for(rule in filter.filterRules)
//                {
//
//                    Log.d("XXXX", "THE NAME  CONNECTOR ${rule.connector.format}")
//                }

            }

            for(enumerationItem in _currentSampledItemsForSampling)
            {
                enumerationItem.samplingState = SamplingState.NotSampled

                // find and remove items that are not valid
                if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                {
                    validSamples.add(enumerationItem)
                }

                // TODO: run through rules and filters, etc..

//                if(sampleItem.enumerationState == EnumerationState.Incomplete)
//                {
//                    enumItem.samplingState = SamplingState.Invalid
//                    removeList.add(enumItem)
//                }
//                if(enumItem.enumerationState == EnumerationState.Enumerated)
//                {
//                   // for(filter in )
//                        for(fieldData in enumItem.fieldDataList)
//                        {
//                           // Log.d("XXX", "field data name ${fieldData.name}  value ${fieldData.numberValue}")
//
//                            // fieldData.
//                        }
//                }

            }

            // remove invalid houses as part of sampling
           // _currentEnumItemsForSampling.removeAll(removeList.toSet())

            // just do random sampling as a test
            currentSampleArea?.value?.let { sampleArea ->
                val sampledIndices: ArrayList<Int> = ArrayList()
                val sampleSize =  min(study.sampleSize,validSamples.size)

                for (i in 0 until sampleSize) {

                    var rnds = (0 until validSamples.size).random()
                    while(sampledIndices.contains(rnds))
                    {
                        rnds = (0 until validSamples.size).random()
                    }
                    sampledIndices.add(rnds)
                    validSamples[rnds]?.samplingState = SamplingState.Sampled
                }
            }

        }

        setSampleAreasForMap(mapboxManager,pointAnnotationManager)

        return SamplingState.None
    }

    fun fixEnumData()
    {
        currentStudy?.value?.let{study ->
            for(sampleItem in _currentSampledItemsForSampling)
            {
//                sampleItem.fieldDataList[0].name = study.fields[0].name
//                sampleItem.fieldDataList[0].type = study.fields[0].type
//
//                sampleItem.fieldDataList[1].name = study.fields[1].name
//                sampleItem.fieldDataList[1].type = study.fields[1].type
            }

            // CHECK
//            for(enumAreaa in study.sampleAreas)
//            {
//                for(location in enumAreaa.locations)
//                {
//                    for(enumItem in location.enumerationItems)
//                    {
//                        for(fieldData in enumItem.fieldDataList)
//                        {
//
//                            Log.d("XXXXXX", "fieldData id ${fieldData.id} name ${fieldData.name} type ${fieldData.type} ${fieldData.numberValue}")
//                        }
//                    }
//                }
//            }

            Log.d("XXXXXXX", "---------------- enumAreas")
//            currentEnumArea?.value?.let{enumArea ->
//                for(location in enumArea.locations)
//                {
//                    for(enumItem in location.enumerationItems)
//                    {
//                        for(fieldData in enumItem.fieldDataList)
//                        {
//
//                            Log.d("XXXXXX", "fieldData id ${fieldData.id} name ${fieldData.name} type ${fieldData.type} ${fieldData.numberValue}")
//                        }
//                    }
//                }
//            }

            Log.d("XXXXXXXXXXX", "------------------- Config enum areas")
//            config?.let{config->
//                for(enumAreaa in config.enumAreas) {
//                    for (location in enumAreaa.locations) {
//                        for (enumItem in location.enumerationItems) {
//                            for (fieldData in enumItem.fieldDataList) {
//                                Log.d(
//                                    "XXXXXX",
//                                    "fieldData id ${fieldData.id} name ${fieldData.name} type ${fieldData.type} ${fieldData.numberValue}"
//                                )
//                            }
//                        }
//                    }
//                }
//
//            }
        }
        Log.d("XXXXXXXXXXX", "-------------------------------")
    }

//    fun samplingInfo(view : View)
//    {
//        currentStudy?.value?.let { study ->
//            print("study.samplingMethod.name()  ${study.samplingMethod.name}")
//        }
//        print("begin sampling")
//    }
}