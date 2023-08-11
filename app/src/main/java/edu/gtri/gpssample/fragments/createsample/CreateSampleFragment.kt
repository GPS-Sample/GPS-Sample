package edu.gtri.gpssample.fragments.createsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.databinding.FragmentCreateSampleBinding
import edu.gtri.gpssample.databinding.FragmentHotspotBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import java.util.ArrayList

class CreateSampleFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapClickListener, ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var config: Config
    private lateinit var map: GoogleMap
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var samplingViewModel: SamplingViewModel

    private var _binding: FragmentCreateSampleBinding? = null
    private val binding get() = _binding!!

    private var curEnumArea : EnumArea? = null
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        val samplingVm : SamplingViewModel by activityViewModels()

        sharedViewModel = vm
        sharedViewModel.currentFragment = this

        samplingViewModel = samplingVm
        samplingViewModel.currentFragment = this

        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

        samplingViewModel.config = sharedViewModel.currentConfiguration?.value
    }



    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateSampleBinding.inflate(inflater, container, false)
        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel
            this.sampleViewModel = samplingViewModel

            // Assign the fragment
            createSampleFragment = this@CreateSampleFragment
        }
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.nextButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_ManageCollectionTeamsFragment)
        }

        sharedViewModel.currentConfiguration?.value.let { config ->
            this.config = config!!
            this.config.id?.let {
               // configId = it
            }
        }
        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let{enumArea->
            samplingViewModel.createSampleArea(enumArea)
        }


        val mapFragment =  childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.infoButton.setOnClickListener{
            // pop up dialog fragment
            findNavController().navigate(R.id.action_navigate_to_SamplingInfoDialogFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateSampleFragment.value.toString() + ": " + this.javaClass.simpleName

    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }


    fun addPolygon( enumArea: EnumArea)
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

        map.setOnPolygonClickListener {polygon ->
            val ea = polygon.tag as EnumArea
            ConfirmationDialog( activity, resources.getString(R.string.please_confirm),
                "${resources.getString(R.string.delete_enum_area_message)} ${ea.name}?",
                resources.getString(R.string.no), resources.getString(R.string.yes), polygon, this)
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0


        // re think this
        map.setOnMapClickListener(this)
        samplingViewModel.getSampleAreaLocations()

        // Need to build enum area that is to be sampled.  there can be clusters and they don't
        // need to be near each other.  how do we find a centroid (maybe?) for viewing purposes or
        // more importantly, are we doing ONE EA at a time?  or are we sampling using the sampling
        // method for ALL EAs?


        map.setOnMapClickListener {
//            if (createMode)
//            {
//                val marker = map.addMarker( MarkerOptions().position(it))
//                marker?.let {
//                    vertexMarkers.add( marker )
//                }
//            }
        }

        samplingViewModel.setSampleAreasForMap(p0)

        //for (enumArea in config.enumAreas)
    }

    override fun onMapClick(p0: LatLng) {
        TODO("Not yet implemented")
    }

    override fun didSelectLeftButton(tag: Any?) {
        TODO("Not yet implemented")
    }

    override fun didSelectRightButton(tag: Any?) {
        TODO("Not yet implemented")
    }
}