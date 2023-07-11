package edu.gtri.gpssample.viewmodels

import android.app.Activity
import android.util.Log
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

            _currentStudy?.value = value?.value
            _currentStudy?.postValue(value?.value)
            Log.d("xxxxxxxx", "setting the study ${_currentStudy?.value?.name}")
        }

    var test : MutableLiveData<String> = MutableLiveData("TEST 123")
}