package edu.gtri.gpssample.viewmodels

import android.app.Activity
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.models.*
import java.lang.Integer.min
import java.util.ArrayList

class SamplingViewModel : ViewModel() {
    private var _currentFragment : Fragment? = null
    private var activity : Activity? = null
    private var _map : GoogleMap? =  null
    private var _currentStudy : MutableLiveData<Study>? = null
    private var _currentSampleArea : MutableLiveData<SampleArea>? = null
    private var _currentSampledItemsForSampling : ArrayList<SampledItem> = ArrayList()


    var config : Config? = null

    var currentFragment : Fragment?
        get() = _currentFragment
        set(value){
            _currentFragment = value
            _currentFragment?.let {fragment ->

                activity = fragment.activity
            }
        }
    var currentStudy : LiveData<Study>?
        get(){
          return _currentStudy
        }
        set(value){

            _currentStudy = MutableLiveData(value?.value)
           // _currentStudy?.postValue(value?.value)
        }

    var currentSampleArea : LiveData<SampleArea>?
        get(){
            return _currentSampleArea
        }
        set(value){
            value?.let{sampleArea ->
                _currentSampleArea = MutableLiveData(sampleArea.value)
                _currentStudy?.value?.let{study->
                    sampleArea.value?.let{ea->
                        study.sampleAreas.add(ea)
                    }
                }
            }
        }


    fun createSampleArea(fromEnumArea: EnumArea)
    {
        val sampleArea = SampleArea(fromEnumArea)
        _currentSampleArea = MutableLiveData(sampleArea)
    }
    fun addPolygon( sampleArea: SampleArea, map: GoogleMap)
    {
        val points = ArrayList<LatLng>()

        sampleArea.vertices.map {
            points.add( it.toLatLng())
        }

        val polygonOptions = PolygonOptions()
            .clickable(true)
            .addAll( points )

        val polygon = map.addPolygon( polygonOptions )
        polygon.tag = sampleArea

    }
    fun getSampleAreaLocations()
    {
        // TODO:  build sample locations from enum locations.  they're different
//        currentSampleArea?.value?.let { sampleArea ->
//            sampleArea.locations = DAO.locationDAO.getLocations(sampleArea)
//        }
    }
    fun setSampleAreasForMap(map: GoogleMap) : SamplingState
    {

        var minLat = 999999.0
        var minLon = 999999.0
        var maxLat = -999999.0
        var maxLon = -999999.0

        _map = map
       // _currentEnumItemsForSampling.clear()
        map.clear()
        map.uiSettings.isScrollGesturesEnabled = true

        currentSampleArea?.value?.let{ sampleArea->

            addPolygon( sampleArea, map )
            // maybe a faster way to build the bounding box?
            for (i in 0 until sampleArea.vertices.size)
            {
                val pos = sampleArea.vertices[i].toLatLng()
                minLat =  if (pos.latitude < minLat) pos.latitude else  minLat
                minLon =  if (pos.longitude < minLon) pos.longitude else  minLon
                maxLat =  if (pos.latitude > maxLat) pos.latitude else  maxLat
                maxLon =  if (pos.longitude > maxLon) pos.longitude else maxLon
            }
            val latLngBounds = LatLngBounds(LatLng(minLat, minLon), LatLng(maxLat,maxLon))
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,10))


            for (location in sampleArea.locations)
            {
                if (location.isLandmark)
                {
                    val icon = BitmapDescriptorFactory.fromResource(R.drawable.location_blue)

                    map.addMarker( MarkerOptions()
                        .position( LatLng( location.latitude, location.longitude ))
                        .icon( icon )
                    )
                }
                else
                {
                    var icon : BitmapDescriptor? = null
                    currentStudy?.value?.let{study->
                        for (item in location.items) {

                            val sampledItem = item as? SampledItem
                            // add item for sampling
                            sampledItem?.let{sampledItem ->
                                if(!_currentSampledItemsForSampling.contains(sampledItem))
                                {
                                    _currentSampledItemsForSampling.add(sampledItem)
                                }

                                icon = when(sampledItem.samplingState)
                                {
                                    SamplingState.None       -> BitmapDescriptorFactory.fromResource(R.drawable.home_black)
                                    SamplingState.NotSampled -> BitmapDescriptorFactory.fromResource(R.drawable.home_grey)
                                    SamplingState.Sampled    -> BitmapDescriptorFactory.fromResource(R.drawable.home_green)
                                    SamplingState.Resampled  -> BitmapDescriptorFactory.fromResource(R.drawable.home_green)
                                    SamplingState.Invalid    -> BitmapDescriptorFactory.fromResource(R.drawable.home_red)
                                }
                                map.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(location.latitude, location.longitude))
                                        .icon(icon)
                                )

                            }
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
        val validSamples : ArrayList<SampledItem> = ArrayList()

        currentStudy?.value?.let { study ->

/// TEST
            for(filter in study.filters)
            {
                Log.d("XXXXXX", filter.name )
                for(rule in filter.filterRules)
                {

                    Log.d("XXXX", "THE NAME  CONNECTOR ${rule.connector.format}")
                }

            }


            for(sampleItem in _currentSampledItemsForSampling)
            {
                sampleItem.samplingState = SamplingState.NotSampled

                if(sampleItem.enumItem.enumerationState == EnumerationState.Enumerated)
                {
                    validSamples.add(sampleItem)
                }
                // find and remove items that are not valid
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

        _map?.let{map->
            setSampleAreasForMap(map)
        }
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