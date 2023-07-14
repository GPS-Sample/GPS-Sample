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

    private var _currentStudy : MutableLiveData<Study>? = null
    private var _currentEnumerationArea : MutableLiveData<EnumArea>? = null

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
               // _currentEnumerationArea?.postValue(value?.value)
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

    fun setEnumAreasForMap(map: GoogleMap) : SamplingState
    {
        var minLat = 999999.0
        var minLon = 999999.0
        var maxLat = -999999.0
        var maxLon = -999999.0

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

            enumArea.locations = DAO.locationDAO.getLocations(enumArea)
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
                    for (enumItem in location.enumerationItems) {
                        var icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)

                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(location.latitude, location.longitude))
                                .icon(icon)
                        )
                    }
                }
            }
        }
        return SamplingState.None
    }
    fun beginSampling(view : View) : SamplingState
    {
        currentStudy?.value?.let { study ->
           print("study.samplingMethod.name()  ${study.samplingMethod.name}")
        }
        print("begin sampling")



        return SamplingState.None
    }

//    fun samplingInfo(view : View)
//    {
//        currentStudy?.value?.let { study ->
//            print("study.samplingMethod.name()  ${study.samplingMethod.name}")
//        }
//        print("begin sampling")
//    }
}