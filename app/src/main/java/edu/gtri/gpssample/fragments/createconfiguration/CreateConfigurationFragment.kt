package edu.gtri.gpssample.fragments.createconfiguration

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
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentCreateConfigurationBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.fragments.createstudy.CreateStudyAdapter
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class CreateConfigurationFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
    ConfirmationDialog.ConfirmationDialogDelegate {
    private var quickStart = false
    private var _binding: FragmentCreateConfigurationBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : ConfigurationViewModel
    private var map : GoogleMap? = null
    private lateinit var createStudyAdapter: CreateStudyAdapter
    private lateinit var manageStudiesAdapter: ManageStudiesAdapter
    private var selectedStudy: Study? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        manageStudiesAdapter = ManageStudiesAdapter(listOf<Study>())
        manageStudiesAdapter.didSelectStudy = this::didSelectStudy
        manageStudiesAdapter.shouldDeleteStudy = this::shouldDeleteStudy
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateConfigurationBinding.inflate(inflater, container, false)
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
            createConfigurationFragment = this@CreateConfigurationFragment
        }

        val quick_start = arguments?.getBoolean( Keys.kQuickStart.toString(), false )

        quick_start?.let {
            quickStart = it
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.nextButton.setOnClickListener {

            if (binding.configNameEditText.text.toString().isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.minGpsPrecisionEditText.text.toString().isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "Please enter the minimum desired GPS precision", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            sharedViewModel.currentConfiguration?.value?.let {config ->

                bundle.putBoolean( Keys.kQuickStart.toString(), quickStart )
                bundle.putString( Keys.kConfig_uuid.toString(), sharedViewModel.currentConfiguration!!.value!!.uuid )
                if (config.id == null){
                    sharedViewModel.saveNewConfiguration()

                } else{
                    sharedViewModel.updateConfiguration()
                }

                findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
            }
        }
        val mapFragment =  childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.addStudyButton.setOnClickListener{
            sharedViewModel.createStudyModel.createNewStudy()
            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
        }

        binding.studiesRecycler.itemAnimator = DefaultItemAnimator()
        binding.studiesRecycler.adapter = manageStudiesAdapter
        binding.studiesRecycler.layoutManager = LinearLayoutManager(activity )
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateConfigurationFragment.value.toString() + ": " + this.javaClass.simpleName
        manageStudiesAdapter.updateStudies(sharedViewModel.currentConfiguration?.value?.studies)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }

    private fun didSelectStudy(study: Study)
    {
        val bundle = Bundle()

        //bundle.putString( Keys.kConfig_uuid.toString(), config.uuid )
        //bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
        sharedViewModel.createStudyModel.setStudy(study)
        findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
    }
    private fun shouldDeleteStudy(study: Study)
    {
        selectedStudy = study
        ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this study?", 0, this)
    }

    override fun onMapClick(p0: LatLng) {
        // Your code here to make it look like the map is clicked on touch
        val bundle = Bundle()
        bundle.putBoolean( Keys.kQuickStart.toString(), quickStart )
        bundle.putString( Keys.kConfig_uuid.toString(), sharedViewModel.currentConfiguration!!.value!!.uuid )
        findNavController().navigate(R.id.action_navigate_to_DefineEnumerationAreaFragment, bundle)
    }
    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map?.let{googleMap ->
            googleMap.setOnMapClickListener(this)
            googleMap.uiSettings.isScrollGesturesEnabled = false

            sharedViewModel.currentConfiguration?.value?.let {config ->
                config.id?.let {id ->
                    val enumAreas = DAO.enumAreaDAO.getEnumAreas( id )

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
                    val srb = LatLng(30.335603,-86.165004 )
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom( srb, 13.5f))
                }
            }
        }
    }

    override fun didAnswerNo() {

    }

    override fun didAnswerYes(tag: Int) {
        sharedViewModel.removeStudy(selectedStudy)
        manageStudiesAdapter.updateStudies(sharedViewModel.currentConfiguration?.value?.studies)
    }
}