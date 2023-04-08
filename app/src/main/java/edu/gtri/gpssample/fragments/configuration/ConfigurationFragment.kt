package edu.gtri.gpssample.fragments.configuration

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentConfigurationBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class ConfigurationFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapClickListener {
    private var quickStart = false
    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : ConfigurationViewModel
    private var map : GoogleMap? = null
    private lateinit var studiesAdapter: StudiesAdapter
    private lateinit var enumerationAreasAdapter: ManageEnumerationAreasAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        studiesAdapter = StudiesAdapter(listOf<Study>())
        studiesAdapter.didSelectStudy = this::didSelectStudy



        enumerationAreasAdapter = ManageEnumerationAreasAdapter( listOf<EnumArea>() )
        enumerationAreasAdapter.didSelectEnumArea = this::didSelectEnumArea
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        val bundle = Bundle()
        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            configurationFragment = this@ConfigurationFragment
        }

        val quick_start = arguments?.getBoolean( Keys.kQuickStart.toString(), false )

        quick_start?.let {
            quickStart = it
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.doneButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
        }
        val mapFragment =  childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.studiesRecycler.itemAnimator = DefaultItemAnimator()
        binding.studiesRecycler.adapter = studiesAdapter
        binding.studiesRecycler.layoutManager = LinearLayoutManager(activity )

        binding.enumAreasRecycler.itemAnimator = DefaultItemAnimator()
        binding.enumAreasRecycler.adapter = enumerationAreasAdapter
        binding.enumAreasRecycler.layoutManager = LinearLayoutManager(activity )
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ConfigurationFragment.value.toString() + ": " + this.javaClass.simpleName
        studiesAdapter.updateStudies(sharedViewModel.currentConfiguration?.value?.studies)
        enumerationAreasAdapter.updateEnumAreas(sharedViewModel.currentConfiguration?.value?.enumAreas)

        // set the first study as selected.  TODO: save the id of the selected study
        sharedViewModel.currentConfiguration?.value?.let { config ->
            if(config.studies.count() > 0)
            {
                config.currentStudy = config.studies[0]

            }
        }


    }
    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }

    private fun didSelectStudy(study: Study)
    {
        val bundle = Bundle()

        findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
    }
    override fun onMapClick(p0: LatLng) {
        // Your code here to make it look like the map is clicked on touch
    }
    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map?.let{googleMap ->
            googleMap.setOnMapClickListener(this)
            googleMap.uiSettings.isScrollGesturesEnabled = false

            // put the enums
            sharedViewModel.currentConfiguration?.value?.let {config ->
                val enumAreas = DAO.enumAreaDAO.getEnumAreas( config.id!! )

                for (enumArea in enumAreas)
                {
                    googleMap.addPolyline(
                        PolylineOptions()
                            .clickable(true)
                            .add(
                                LatLng( enumArea.topLeft.latitude, enumArea.topLeft.longitude ),
                                LatLng( enumArea.topRight.latitude, enumArea.topRight.longitude ),
                                LatLng( enumArea.botRight.latitude, enumArea.botRight.longitude ),
                                LatLng( enumArea.botLeft.latitude, enumArea.botLeft.longitude ),
                                LatLng( enumArea.topLeft.latitude, enumArea.topLeft.longitude ),
                            )
                    )
                }

                // HACK HACK
                val srb = LatLng(30.330603,-86.165004 )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom( srb, 13.5f))
            }
        }
    }

    private fun didSelectEnumArea(enumArea: EnumArea)
    {
        val bundle = Bundle()
        sharedViewModel.currentConfiguration?.value?.currentStudy?.let{study ->

            bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
            bundle.putInt( Keys.kEnumArea_id.toString(), enumArea.id!! )
            findNavController().navigate( R.id.action_navigate_to_ManageEnumerationAreaFragment, bundle )
        }

    }
}