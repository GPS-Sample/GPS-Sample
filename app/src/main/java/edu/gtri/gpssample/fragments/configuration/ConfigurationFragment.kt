package edu.gtri.gpssample.fragments.configuration

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
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
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.HotspotMode
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentConfigurationBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import java.io.File
import java.io.FileWriter
import java.util.*

class ConfigurationFragment : Fragment(),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    InputDialog.InputDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private var map : GoogleMap? = null
    private lateinit var studiesAdapter: StudiesAdapter
    private lateinit var enumerationAreasAdapter: ManageEnumerationAreasAdapter

    private val kDeleteTag = 1
    private val kExportTag = 2
    private val kTaskTag = 3

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        val networkVm : NetworkViewModel by activityViewModels()

        sharedViewModel = vm
        sharedNetworkViewModel = networkVm

        sharedNetworkViewModel.currentFragment = this
        sharedViewModel.currentFragment = this

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
        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            configurationFragment = this@ConfigurationFragment
        }

        binding.editImageView.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this configuration?", "No", "Yes", kDeleteTag, this)
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.importButton.setOnClickListener {
            ConfirmationDialog( activity, "Export Configuration", "Select an export method", "QR Code", "File System", kExportTag, this)
        }

        binding.exportButton.setOnClickListener {
            ConfirmationDialog( activity, "Export Configuration", "Select an export method", "QR Code", "File System", kExportTag, this)
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

        // set the first study as selected.  TODO: save the id of the selected study
        sharedViewModel.currentConfiguration?.value?.let { config ->

            val enumAreas = DAO.enumAreaDAO.getEnumAreas(config)
            enumerationAreasAdapter.updateEnumAreas(enumAreas)

            if(config.studies.count() > 0)
            {
                Log.d( "xxx", "selected study with ID ${config.studies[0].id}")
                sharedViewModel.createStudyModel.setStudy(config.studies[0])
            }
        }
        map?.let { map ->
            onMapReady(map)
        }
    }

    private fun didSelectStudy(study: Study)
    {
        sharedViewModel.createStudyModel.setStudy(study)
    }

    override fun onMapClick(p0: LatLng) {
        val bundle = Bundle()
        bundle.putBoolean( Keys.kEditMode.toString(), false )
        findNavController().navigate(R.id.action_navigate_to_CreateEnumerationAreaFragment, bundle)
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map?.let{googleMap ->
            googleMap.clear()
            googleMap.setOnMapClickListener(this)
            googleMap.uiSettings.isScrollGesturesEnabled = false

            // put the enums
            sharedViewModel.currentConfiguration?.value?.let {config ->
                val enumAreas = DAO.enumAreaDAO.getEnumAreas( config )

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

                    val enumDataList = DAO.enumDataDAO.getEnumData(enumArea)

                    for (enumData in enumDataList)
                    {
                        var icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)

                        if (enumData.incomplete)
                        {
                            icon = BitmapDescriptorFactory.fromResource(R.drawable.home_red)
                        }
                        else if (enumData.valid)
                        {
                            icon = BitmapDescriptorFactory.fromResource(R.drawable.home_green)
                        }

                        if (enumData.isLocation)
                            icon = BitmapDescriptorFactory.fromResource(R.drawable.location_blue)

                        googleMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(enumData.latitude, enumData.longitude))
                                .icon(icon)
                        )
                    }
                }

                // HACK HACK
                val atl = LatLng( 33.774881, -84.396341 )
                val srb = LatLng(30.335603,-86.165004 )
                val demo = LatLng( 33.982973122594785, -84.31252665817738 )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom( demo, 13.5f))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectLeftButton(tag: Any?)
    {
        tag?.let {
            when(tag)
            {
                kExportTag -> {
                    // Launch connection screen
                    view?.let{view ->
                        val user = (activity!!.application as MainApplication).user
                        user?.let { user ->

                            //TODO: fix this! compare should be the enum
                            when(user.role)
                            {
                                Role.Supervisor.toString() ->
                                {
                                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Supervisor)
                                }
                                Role.Admin.toString() ->
                                {
                                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Admin)
                                }
                            }
                        }

                        sharedViewModel?.currentConfiguration?.value?.let{
                            sharedNetworkViewModel.setCurrentConfig(it)
                        }

                        sharedNetworkViewModel.createHotspot(view)
                    }
                }

                kTaskTag -> {
                    findNavController().navigate( R.id.action_navigate_to_ManageEnumerationTeamsFragment )
                }
                else -> {}
            }
        }
    }

    override fun didSelectRightButton(tag: Any?)
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            tag?.let { tag ->

                when(tag)
                {
                    kDeleteTag -> {
                        sharedViewModel.deleteConfig(config)
                        findNavController().popBackStack()
                    }

                    kExportTag -> {
                        InputDialog( activity!!, "Enter a file name for the export", config.name, null, this@ConfigurationFragment )
                    }

                    kTaskTag -> {
                        findNavController().navigate( R.id.action_navigate_to_CreateSampleFragment )
                    }
                    else -> {}
                }
            }
        }
    }

    override fun didEnterText( name: String, tag: Any? )
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            DAO.configDAO.updateAllLists( config )

            val packedConfig = config.pack()
            Log.d( "xxx", packedConfig )

            val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
            val file = File(root, "$name.${Date().time}.json")
            val writer = FileWriter(file)
            writer.append(packedConfig)
            writer.flush()
            writer.close()

            Toast.makeText(activity!!.applicationContext, "The configuration has been saved to the Documents directory.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun didSelectEnumArea(enumArea: EnumArea)
    {
        sharedViewModel.enumAreaViewModel.setCurrentEnumArea(enumArea)
        ConfirmationDialog( activity, "Enumeration Area ${enumArea.name}", "Select a task", "Enumeration", "Collection", kTaskTag, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1023 && resultCode == Activity.RESULT_OK)
        {
            val uri = data?.data

            uri?.let { uri ->

                try
                {
                    val inputStream = activity!!.getContentResolver().openInputStream(uri)

                    inputStream?.let {  inputStream ->
                        val text = inputStream.bufferedReader().readText()

                        Log.d( "xxx", text )

                        val enumArea = EnumArea.unpack( text )

                        enumArea?.let { enumArea ->
                            for (enumData in enumArea.enumDataList)
                            {
                                DAO.enumDataDAO.importEnumData( enumData )
                            }
                        } ?: Toast.makeText(activity!!.applicationContext, "Oops! The import failed.  Please try again.", Toast.LENGTH_SHORT).show()

                        map?.let { map ->
                            onMapReady(map)
                        }
                    }
                }
                catch( ex: java.lang.Exception )
                {
                    Toast.makeText(activity!!.applicationContext, "Oops! The import failed.  Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}