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
import java.util.ArrayList

class SamplingViewModel : ViewModel() {
    private var _currentFragment : Fragment? = null
    private var activity : Activity? = null
    private var _map : GoogleMap? =  null
    private var _currentStudy : MutableLiveData<Study>? = null
    private var _currentEnumerationArea : MutableLiveData<EnumArea>? = null
    private var _currentEnumItemsForSampling : ArrayList<EnumerationItem> = ArrayList()

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

    var currentEnumArea : LiveData<EnumArea>?
        get(){
            return _currentEnumerationArea
        }
        set(value){
            value?.let{enumArea ->
                _currentEnumerationArea = MutableLiveData(enumArea.value)
                _currentStudy?.value?.let{study->
                    enumArea.value?.let{ea->
                        study.sampleAreas.add(ea)
                    }
                }
            }
        }


    fun addPolygon( enumArea: EnumArea, map: GoogleMap)
    {
        val points = ArrayList<LatLng>()

        enumArea.vertices.map {
            points.add( it.toLatLng())
        }

        val polygonOptions = PolygonOptions()
            .clickable(true)
            .addAll( points )

        val polygon = map.addPolygon( polygonOptions )
        polygon.tag = enumArea

    }
    fun getEnumAreaLocations()
    {
        currentEnumArea?.value?.let { enumArea ->
            enumArea.locations = DAO.locationDAO.getLocations(enumArea)
        }
    }
    fun setEnumAreasForMap(map: GoogleMap) : SamplingState
    {

        var minLat = 999999.0
        var minLon = 999999.0
        var maxLat = -999999.0
        var maxLon = -999999.0

        _map = map
       // _currentEnumItemsForSampling.clear()
        map.clear()
        map.uiSettings.isScrollGesturesEnabled = true

        currentEnumArea?.value?.let{ enumArea->

            addPolygon( enumArea, map )
            // maybe a faster way to build the bounding box?
            for (i in 0 until enumArea.vertices.size)
            {
                val pos = enumArea.vertices[i].toLatLng()
                minLat =  if (pos.latitude < minLat) pos.latitude else  minLat
                minLon =  if (pos.longitude < minLon) pos.longitude else  minLon
                maxLat =  if (pos.latitude > maxLat) pos.latitude else  maxLat
                maxLon =  if (pos.longitude > maxLon) pos.longitude else maxLon
            }
            val latLngBounds = LatLngBounds(LatLng(minLat, minLon), LatLng(maxLat,maxLon))
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,10))


            for (location in enumArea.locations)
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
                        for (enumItem in location.enumerationItems) {

                            // add item for sampling

                            if(!_currentEnumItemsForSampling.contains(enumItem))
                            {
                                _currentEnumItemsForSampling.add(enumItem)
                            }

                            icon = when(enumItem.samplingState)
                            {
                                SamplingState.None -> BitmapDescriptorFactory.fromResource(R.drawable.home_black)
                                SamplingState.NotSampled -> BitmapDescriptorFactory.fromResource(R.drawable.home_grey)
                                SamplingState.Sampled -> BitmapDescriptorFactory.fromResource(R.drawable.home_green)
                                SamplingState.Resampled -> BitmapDescriptorFactory.fromResource(R.drawable.home_red)
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
        return SamplingState.None
    }
    fun beginSampling(view : View) : SamplingState
    {
        fixEnumData()
        // reset list
        val removeList : ArrayList<EnumerationItem> = ArrayList()

        currentStudy?.value?.let { study ->


            for(filter in study.filters)
            {
                Log.d("XXXXXX", filter.name )
                for(rule in filter.filterRules)
                {

                    Log.d("XXXX", "THE NAME  CONNECTOR ${rule.connector.format}")
                }

            }

            for(enumItem in _currentEnumItemsForSampling)
            {
                enumItem.samplingState = SamplingState.None
                // find and remove items that are not valid
                if(enumItem.enumerationState == EnumerationState.Incomplete)
                {
                    removeList.add(enumItem)
                }
                if(enumItem.enumerationState == EnumerationState.Enumerated)
                {
                   // for(filter in )
                        for(fieldData in enumItem.fieldDataList)
                        {
                            Log.d("XXX", "field data name ${fieldData.name}")

                            // fieldData.
                        }
                }

            }

            // just do random sampling as a test
            currentEnumArea?.value?.let { enumArea ->
                val sampledIndices: ArrayList<Int> = ArrayList()
                for (i in 0 until study.sampleSize) {

                    var rnds = (0 until _currentEnumItemsForSampling.size).random()
                    while(sampledIndices.contains(rnds))
                    {
                        rnds = (0 until _currentEnumItemsForSampling.size).random()
                    }
                    sampledIndices.add(rnds)
                    _currentEnumItemsForSampling[rnds]?.samplingState = SamplingState.Sampled
                }


            }

        }

        _map?.let{map->
            setEnumAreasForMap(map)
        }
        return SamplingState.None
    }

    fun fixEnumData()
    {
        currentStudy?.value?.let{study->

            for(i in 0 until _currentEnumItemsForSampling.size)
            {
                val enumItem = _currentEnumItemsForSampling[i]
                enumItem.fieldDataList[0].name = study.fields[0].name
                enumItem.fieldDataList[0].type = study.fields[0].type
                enumItem.fieldDataList[1].name = study.fields[1].name
                enumItem.fieldDataList[1].type = study.fields[1].type
            }
        }

    }

//    fun samplingInfo(view : View)
//    {
//        currentStudy?.value?.let { study ->
//            print("study.samplingMethod.name()  ${study.samplingMethod.name}")
//        }
//        print("begin sampling")
//    }
}