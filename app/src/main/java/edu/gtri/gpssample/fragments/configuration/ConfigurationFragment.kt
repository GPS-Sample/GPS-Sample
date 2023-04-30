package edu.gtri.gpssample.fragments.configuration

import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.gms.maps.model.PolygonOptions
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentConfigurationBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.fragments.manage_enumeration_areas.ManageEnumerationAreasAdapter
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.io.File
import java.io.FileWriter
import java.util.*

class ConfigurationFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapClickListener, ConfirmationDialog.ConfirmationDialogDelegate
{
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

        binding.editImageView.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this configuration?", 0, this)
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.generateQrButton.setOnClickListener {
        }

        binding.exportButton.setOnClickListener {

            sharedViewModel.currentConfiguration?.value?.let { config ->
                val packedConfig = config.pack()
                Log.d( "xxx", packedConfig )
                val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
                val file = File(root, "config.${Date().time}.json")
                val writer = FileWriter(file)
                writer.append(packedConfig)
                writer.flush()
                writer.close()
            }
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

        sharedViewModel.currentConfiguration?.value?.let { config ->
            enumerationAreasAdapter.updateEnumAreas(DAO.enumAreaDAO.getEnumAreas(config.id!!))
        }

        // set the first study as selected.  TODO: save the id of the selected study
        sharedViewModel.currentConfiguration?.value?.let { config ->
            if(config.studies.count() > 0)
            {
                sharedViewModel.createStudyModel.setStudy(config.studies[0])
            }
        }
    }

    private fun didSelectStudy(study: Study)
    {
//        sharedViewModel.createStudyModel.setStudy(study)
//        findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment)
    }

    override fun onMapClick(p0: LatLng) {
        val bundle = Bundle()
        bundle.putBoolean( Keys.kEditMode.toString(), false )
        findNavController().navigate(R.id.action_navigate_to_DefineEnumerationAreaFragment, bundle)
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
                    val points = ArrayList<LatLng>()

                    enumArea.vertices.map {
                        points.add( it.toLatLng())
                    }

                    val polygon = PolygonOptions()
                        .clickable(false)
                        .addAll( points )

                    googleMap.addPolygon(polygon)
                }

                // HACK HACK
                val atl = LatLng( 33.774881, -84.396341 )
                val srb = LatLng(30.335603,-86.165004 )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom( atl, 13.5f))
            }
        }
    }

    override fun didAnswerNo()
    {
    }

    override fun didAnswerYes( tag: Any? )
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            sharedViewModel.deleteConfig(config)
            findNavController().popBackStack()
        }
    }

    private fun didSelectEnumArea(enumArea: EnumArea)
    {
        sharedViewModel.enumAreaViewModel.setCurrentEnumArea(enumArea)
        findNavController().navigate( R.id.action_navigate_to_ManageEnumerationAreaFragment )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}