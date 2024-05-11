package edu.gtri.gpssample.fragments.configuration

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.HotspotMode
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentConfigurationBinding
import edu.gtri.gpssample.dialogs.BusyIndicatorDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InfoDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class ConfigurationFragment : Fragment(),
    ConfirmationDialog.ConfirmationDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapboxManager: MapboxManager
    private lateinit var studiesAdapter: StudiesAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var enumerationAreasAdapter: ConfigurationAdapter

    private val kDeleteTag = 1
    private val kExportTag = 2
    private val kImportTag = 3
    private val kTaskTag =   4

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        val networkVm : NetworkViewModel by activityViewModels()

        sharedViewModel = vm
        sharedNetworkViewModel = networkVm

        sharedNetworkViewModel.currentFragment = this
        sharedViewModel.currentFragment = this

//        setHasOptionsMenu(true)
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

        binding.hideAdditionalInfoImageView.setOnClickListener {
            binding.settingsLayout.visibility = View.GONE
            binding.hideAdditionalInfoImageView.visibility = View.GONE
            binding.showAdditionalInfoImageView.visibility = View.VISIBLE
        }

        binding.showAdditionalInfoImageView.setOnClickListener {
            binding.settingsLayout.visibility = View.VISIBLE
            binding.showAdditionalInfoImageView.visibility = View.GONE
            binding.hideAdditionalInfoImageView.visibility = View.VISIBLE
        }

        binding.editImageView.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.delete_configuration_message),
                resources.getString(R.string.no), resources.getString(R.string.yes), kDeleteTag, this)
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.importButton.setOnClickListener {
            ConfirmationDialog( activity, resources.getString(R.string.import_field_data), resources.getString(R.string.select_import_method_message),
                resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kImportTag, this)
        }

        binding.exportButton.setOnClickListener {
            ConfirmationDialog( activity, resources.getString(R.string.export_configuration), resources.getString(R.string.select_export_message),
                resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kExportTag, this)
        }

        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    refreshMap()
                }
            }
        )

        binding.mapView.getMapboxMap().addOnMapClickListener {
            val bundle = Bundle()
            bundle.putBoolean( Keys.kEditMode.toString(), false )
            findNavController().navigate(R.id.action_navigate_to_CreateEnumerationAreaFragment, bundle)
            return@addOnMapClickListener true
        }

        val pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
        val polygonAnnotationManager = binding.mapView.annotations.createPolygonAnnotationManager()
        val polylineAnnotationManager = binding.mapView.annotations.createPolylineAnnotationManager()
        mapboxManager = MapboxManager( activity!!, pointAnnotationManager, polygonAnnotationManager, polylineAnnotationManager )

        sharedViewModel.currentConfiguration?.value?.let { config ->

            if (!config.proximityWarningIsEnabled)
            {
                binding.proximityWarningTextView.visibility = View.GONE
            }

            binding.configNameTextView.text = config.name

            studiesAdapter = StudiesAdapter(config.studies)
            studiesAdapter.didSelectStudy = this::didSelectStudy

            binding.locationSwitch.isChecked = config.allowManualLocationEntry

            if(config.studies.count() > 0)
            {
                sharedViewModel.createStudyModel.setStudy(config.studies[0])
            }

            enumerationAreasAdapter = ConfigurationAdapter( config.enumAreas )
            enumerationAreasAdapter.didSelectEnumArea = this::didSelectEnumArea
        }

        binding.studiesRecycler.itemAnimator = DefaultItemAnimator()
        binding.studiesRecycler.adapter = studiesAdapter
        binding.studiesRecycler.layoutManager = LinearLayoutManager(activity )

        binding.enumAreasRecycler.itemAnimator = DefaultItemAnimator()
        binding.enumAreasRecycler.adapter = enumerationAreasAdapter
        binding.enumAreasRecycler.layoutManager = LinearLayoutManager(activity )

        updateOverview()
    }

    fun updateOverview()
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->

            var sampledCount = 0
            var enumerationCount = 0
            var surveyedCount = 0
            var eligibleCount = 0

            binding.numberOfEnumerationAreasTextView.text = "${config.enumAreas.size}"

            for (enumerationArea in config.enumAreas)
            {
                for (location in enumerationArea.locations)
                {
                    for (enumItem in location.enumerationItems)
                    {
                        if (enumItem.enumerationState == EnumerationState.Enumerated || enumItem.enumerationState == EnumerationState.Incomplete)
                        {
                            enumerationCount += 1
                        }
                        if (enumItem.enumerationEligibleForSampling)
                        {
                            eligibleCount += 1
                        }
                        if (enumItem.samplingState == SamplingState.Sampled)
                        {
                            sampledCount += 1
                        }
                        if (enumItem.collectionState == CollectionState.Complete)
                        {
                            surveyedCount += 1
                        }
                    }
                }
            }

            val numRemaining = sampledCount - surveyedCount

            binding.numberEnumeratedTextView.text = "$enumerationCount"
            binding.numberEligibleTextView.text = "$eligibleCount"
            binding.numberSampledTextView.text = "$sampledCount"
            binding.numberSurveyedTextView.text = "$surveyedCount"
            binding.numberRemainingTextView.text = "$numRemaining"
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ConfigurationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun refreshMap()
    {
        sharedViewModel.currentConfiguration?.value?.let {config ->

            val enumVerts = ArrayList<LatLon>()

            for (enumArea in config.enumAreas)
            {
                val points = ArrayList<com.mapbox.geojson.Point>()
                val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

                enumArea.vertices.map {
                    enumVerts.add(it)
                    points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
                }

                pointList.add( points )

                mapboxManager.addPolygon( pointList, "#000000", 0.25 )
                mapboxManager.addPolyline( pointList[0], "#ff0000" )

            }

            val latLngBounds = GeoUtils.findGeobounds(enumVerts)
            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
            val cameraPosition = CameraOptions.Builder()
                .zoom(10.0)
                .center(point)
                .build()

            binding.mapView.getMapboxMap().setCamera(cameraPosition)
        }
    }

    private fun didSelectStudy(study: Study)
    {
        sharedViewModel.createStudyModel.setStudy(study)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectLeftButton(tag: Any?)
    {
        tag?.let {
            when(tag)
            {
                kImportTag, kExportTag -> {
                    if (tag == kImportTag)
                    {
                        sharedNetworkViewModel.networkHotspotModel.setTitle(resources.getString(R.string.import_field_data))
                        sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Import )
                    }
                    else
                    {
                        sharedNetworkViewModel.networkHotspotModel.setTitle(resources.getString(R.string.export_configuration))
                        sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Export )
                    }

                    view?.let{view ->
                        sharedViewModel.currentConfiguration?.value?.let{ config ->
                            sharedNetworkViewModel.setCurrentConfig(config)
                            sharedNetworkViewModel.networkHotspotModel.encryptionPassword = config.encryptionPassword
                            sharedNetworkViewModel.createHotspot(view)
                        }
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
//                        sharedViewModel.deleteConfig(config)
                        DAO.deleteAll()
                        findNavController().popBackStack()
                    }

                    kImportTag -> {
                        val intent = Intent()
                            .setType("*/*")
                            .setAction(Intent.ACTION_GET_CONTENT)
                        startActivityForResult(Intent.createChooser(intent, "Select an Enumeration"), 1023)
                    }

                    kExportTag -> {
                        exportToDevice()
                    }

                    kTaskTag -> {
                        findNavController().navigate( R.id.action_navigate_to_CreateSampleFragment )
                    }
                    else -> {}
                }
            }
        }
    }

    fun exportToDevice( )
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            val packedConfig = config.pack()

            val user = (activity!!.application as MainApplication).user

            var userName = user!!.name.replace(" ", "" ).uppercase()

            if (userName.length > 4)
            {
                userName = userName.substring(0,4)
            }

            val role = user.role.toString().substring(0,2).uppercase()

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
            val dateTime = LocalDateTime.now().format(formatter)

            var version = ""
            val versionName = BuildConfig.VERSION_NAME.split( "#" )
            if (versionName.size == 2)
            {
                version = versionName[1]
            }

            val fileName = "C-${role}-${userName}-${dateTime!!}-${version}.json"

            val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS + "/GPSSample")
            root.mkdirs()
            val file = File(root, fileName)
            val writer = FileWriter(file)
            writer.append(packedConfig)
            writer.flush()
            writer.close()

            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved_doc),
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun didSelectEnumArea(enumArea: EnumArea)
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            config.selectedEnumAreaUuid = enumArea.uuid
            sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                config.selectedStudyUuid = study.uuid
                sharedViewModel.enumAreaViewModel.setCurrentEnumArea(enumArea)
                ConfirmationDialog( activity, "", resources.getString(R.string.select_task), resources.getString(R.string.client), resources.getString(R.string.survey), kTaskTag, this)
            } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.no_study_ea), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1023 && resultCode == Activity.RESULT_OK)
        {
            val uri = data?.data

            uri?.let { uri ->
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    try
                    {
                        val inputStream = activity!!.getContentResolver().openInputStream(uri)

                        inputStream?.let {  inputStream ->
                            binding.overlayView.visibility = View.VISIBLE

                            Thread {
                                val text = inputStream.bufferedReader().readText()

                                val enumArea = EnumArea.unpack( text, config.encryptionPassword )

                                if (enumArea == null)
                                {
                                    activity!!.runOnUiThread {
                                        binding.overlayView.visibility = View.GONE
                                        InfoDialog( activity!!, resources.getString(R.string.error), resources.getString(R.string.import_failed), resources.getString(R.string.ok), null, null)
                                    }
                                }
                                else
                                {
                                    DAO.instance().writableDatabase.beginTransaction()

                                    for (location in enumArea.locations)
                                    {
                                        DAO.locationDAO.createOrUpdateLocation(location, enumArea)
                                    }

                                    DAO.instance().writableDatabase.setTransactionSuccessful()
                                    DAO.instance().writableDatabase.endTransaction()

                                    // replace the enumArea from currentConfig with this one
                                    sharedViewModel.replaceEnumArea(enumArea)

                                    activity!!.runOnUiThread {
                                        binding.overlayView.visibility = View.GONE
                                        InfoDialog( activity!!, resources.getString(R.string.success), resources.getString(R.string.import_succeeded), resources.getString(R.string.ok), null, null)
                                    }
                                }
                            }.start()
                        }
                    }
                    catch( ex: java.lang.Exception )
                    {
                        binding.overlayView.visibility = View.GONE
                        InfoDialog( activity!!, resources.getString(R.string.error), resources.getString(R.string.import_failed), resources.getString(R.string.ok), null, null)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_test, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            R.id.enumerate -> enumerateEverything()
            R.id.survey -> surveyEverything()
        }

        return super.onOptionsItemSelected(item)
    }

    fun enumerateEverything()
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->

            var count = 1
            var total = 0
            val busyIndicatorDialog = BusyIndicatorDialog(activity!!, "Enumerating HH's...", this, false)

            for (enumArea in config.enumAreas)
            {
                for (location in enumArea.locations)
                {
                    total += 1
                }
            }

            Thread {
                DAO.instance().writableDatabase.beginTransaction()

                for (enumArea in config.enumAreas)
                {
                    val polygon = ArrayList<LatLon>()

                    var index = 0

                    enumArea.vertices.map {
                        polygon.add( LatLon( index++, it.latitude, it.longitude ))
                    }

                    val enumerationTeam = DAO.enumerationTeamDAO.createOrUpdateTeam( EnumerationTeam( enumArea.uuid, "E-Team-${enumArea.uuid}", polygon, enumArea.locations ))
                    enumArea.enumerationTeams.add(enumerationTeam!!)

                    for (location in enumArea.locations)
                    {
                        val enumerationItem = EnumerationItem()
                        enumerationItem.subAddress = count.toString()
                        enumerationItem.enumerationDate = Date().time
                        enumerationItem.enumerationEligibleForSampling = true
                        enumerationItem.syncCode = enumerationItem.syncCode + 1
                        enumerationItem.enumerationState = EnumerationState.Enumerated
                        DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
                        location.enumerationItems.add(enumerationItem)
                        activity!!.runOnUiThread {
                            busyIndicatorDialog.updateProgress("${count}/${total}")
                        }
                        count += 1
                    }
                }

                DAO.instance().writableDatabase.setTransactionSuccessful()
                DAO.instance().writableDatabase.endTransaction()

                activity!!.runOnUiThread {
                    busyIndicatorDialog.alertDialog.cancel()
                    updateOverview()
                    enumerationAreasAdapter.updateEnumAreas(config.enumAreas)
                }
            }.start()
        }
    }

    fun surveyEverything()
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->

            var count = 1
            var total = 0
            val busyIndicatorDialog = BusyIndicatorDialog(activity!!, "Enumerating HH's...", this, false)

            for (enumArea in config.enumAreas)
            {
                for (location in enumArea.locations)
                {
                    total += 1
                }
            }

            Thread {
                DAO.instance().writableDatabase.beginTransaction()

                for (enumArea in config.enumAreas)
                {
                    val polygon = ArrayList<LatLon>()

                    var index = 0

                    enumArea.vertices.map {
                        polygon.add( LatLon( index++, it.latitude, it.longitude ))
                    }

                    sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                        val collectionTeam = DAO.collectionTeamDAO.createOrUpdateTeam( CollectionTeam( enumArea.uuid, study.uuid, "S-Team-${enumArea.uuid}", polygon, enumArea.locations ))
                        study.collectionTeams.add(collectionTeam!!)
                    }

                    for (location in enumArea.locations)
                    {
                        for (enumerationItem in location.enumerationItems)
                        {
                            enumerationItem.collectionDate = Date().time
                            enumerationItem.syncCode = enumerationItem.syncCode + 1
                            enumerationItem.collectionState = CollectionState.Complete
                            enumerationItem.samplingState = SamplingState.Sampled
                            DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
                            activity!!.runOnUiThread {
                                busyIndicatorDialog.updateProgress("${count}/${total}")
                            }
                            count += 1
                        }
                    }
                }

                DAO.instance().writableDatabase.setTransactionSuccessful()
                DAO.instance().writableDatabase.endTransaction()

                activity!!.runOnUiThread {
                    busyIndicatorDialog.alertDialog.cancel()
                    updateOverview()
                    enumerationAreasAdapter.updateEnumAreas(config.enumAreas)
                }
            }.start()
        }
    }

    override fun didPressCancelButton()
    {
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}