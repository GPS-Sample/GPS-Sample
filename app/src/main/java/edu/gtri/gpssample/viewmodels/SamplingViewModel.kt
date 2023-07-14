package edu.gtri.gpssample.viewmodels

import android.app.Activity
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.viewmodels.models.*

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
            Log.d("xxxxxxxx", "GETTING the study ${_currentStudy?.value?.name}")
          return _currentStudy
        }
        set(value){

            _currentStudy = MutableLiveData(value?.value)
           // _currentStudy?.postValue(value?.value)
            Log.d("xxxxxxxx", "setting the study ${_currentStudy?.value?.name}")
        }

    var currentEnumArea : LiveData<EnumArea>?
        get(){
            Log.d("xxxxxxxx", "GETTING the study ${_currentStudy?.value?.name}")
            return _currentEnumerationArea
        }
        set(value){

            value?.let{enumArea ->
                _currentEnumerationArea = MutableLiveData(enumArea.value)
               // _currentEnumerationArea?.postValue(value?.value)
            }


        }
    var test : MutableLiveData<String> = MutableLiveData("TEST 123")

    fun beginSampling(view : View)
    {
        currentStudy?.value?.let { study ->
           print("study.samplingMethod.name()  ${study.samplingMethod.name}")
        }
        print("begin sampling")
    }

//    fun samplingInfo(view : View)
//    {
//        currentStudy?.value?.let { study ->
//            print("study.samplingMethod.name()  ${study.samplingMethod.name}")
//        }
//        print("begin sampling")
//    }
}