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
import com.google.android.gms.maps.model.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.LatLon
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentCreateConfigurationBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.ArrayList

class CreateConfigurationFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
    ConfirmationDialog.ConfirmationDialogDelegate {

    private var _binding: FragmentCreateConfigurationBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var map : GoogleMap
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
        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createConfigurationFragment = this@CreateConfigurationFragment
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.cancelButton.setOnClickListener {
//            sharedViewModel.currentConfiguration?.value?.let { config ->
//                sharedViewModel.deleteConfig( config )
//            }
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            if (binding.configNameEditText.text.toString().isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.minGpsPrecisionEditText.text.toString().isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.desired_gps_position), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedViewModel.currentConfiguration?.value?.let {config ->
                sharedViewModel.updateConfiguration()
                findNavController().popBackStack()
            }
        }

        val mapFragment =  childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.addStudyButton.setOnClickListener{
            sharedViewModel.createStudyModel.createNewStudy()
            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment)
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

        if (this::map.isInitialized)
        {
            addPolygons()
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }

    private fun didSelectStudy(study: Study)
    {
        sharedViewModel.createStudyModel.setStudy(study)
        findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment)
    }
    private fun shouldDeleteStudy(study: Study)
    {
        selectedStudy = study
        ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.delete_study_message),
            resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
    }

    override fun onMapClick(p0: LatLng) {
        val bundle = Bundle()
        bundle.putBoolean( Keys.kEditMode.toString(), true )
        findNavController().navigate(R.id.action_navigate_to_CreateEnumerationAreaFragment, bundle)
    }
    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.setOnMapClickListener(this)
        map.uiSettings.isScrollGesturesEnabled = false

        addPolygons()
    }

    fun addPolygons()
    {
        map.clear()
        sharedViewModel.currentConfiguration?.value?.let { config ->

            val enumVerts = ArrayList<LatLon>()
            for (enumArea in config.enumAreas)
            {
                val points = ArrayList<LatLng>()

                enumArea.vertices.map {
                    enumVerts.add(it)
                    points.add( it.toLatLng())
                }

                val polygon = PolygonOptions()
                    .clickable(false)
                    .addAll( points )

                map.addPolygon(polygon)
            }

            val latLngBounds = GeoUtils.findGeobounds(enumVerts)
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,10))
        }
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        sharedViewModel.removeStudy(selectedStudy)
        manageStudiesAdapter.updateStudies(sharedViewModel.currentConfiguration?.value?.studies)
    }
}