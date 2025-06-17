/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

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
import com.mapbox.geojson.Point
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.HotspotMode
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentConfigurationBinding
import edu.gtri.gpssample.dialogs.BusyIndicatorDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InfoDialog
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class ConfigurationFragment : Fragment(),
    View.OnTouchListener,
    ConfirmationDialog.ConfirmationDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!

    private lateinit var studiesAdapter: StudiesAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var enumerationAreasAdapter: ConfigurationAdapter

    private val kDeleteTag          = 1
    private val kExportTag          = 2
    private val kImportTag          = 3
    private val kTaskTag            = 4
    private val kFileLocationTag    = 5

    val REQUEST_CODE_PICK_DIR = 1001
    val REQUEST_CODE_PICK_FILE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm: ConfigurationViewModel by activityViewModels()
        val networkVm: NetworkViewModel by activityViewModels()

        sharedViewModel = vm
        sharedNetworkViewModel = networkVm

        sharedNetworkViewModel.currentFragment = this
        sharedViewModel.currentFragment = this

        if (BuildConfig.DEBUG)
        {
            setHasOptionsMenu(true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
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

        binding.mapOverlayView.setOnTouchListener(this)

        sharedViewModel.currentConfiguration?.value?.let { config ->
            MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView ) { mapView ->

                binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

                MapManager.instance().enableLocationUpdates( activity!!, mapView )

                if (config.enumAreas.isNotEmpty())
                {
                    sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                        MapManager.instance().centerMap( config.enumAreas[0], currentZoomLevel, mapView )
                    }
                }
                else
                {
                    sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                        MapManager.instance().centerMap( Point.fromLngLat( MapManager.GEORGIA_TECH.longitude, MapManager.GEORGIA_TECH.latitude), currentZoomLevel, mapView )
                    }
                }
            }

            val items = ArrayList<String>()
            val mapEngines = resources.getTextArray( R.array.map_engines )

            for (mapEngine in mapEngines)
            {
                items.add( mapEngine.toString() )
            }

            binding.mapEngineText.text = items[config.mapEngineIndex]

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

//    fun refreshMap()
//    {
//        sharedViewModel.currentConfiguration?.value?.let {config ->
//
//            val enumVerts = ArrayList<LatLon>()
//
//            for (enumArea in config.enumAreas)
//            {
//                val points = ArrayList<com.mapbox.geojson.Point>()
//                val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()
//
//                enumArea.vertices.map {
//                    enumVerts.add(it)
//                    points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
//                }
//
//                pointList.add( points )
//
//                MapManager.instance().createPolygon( mapView, pointList, "#000000", 0.25 )
//                MapManager.instance().createPolyline( mapView, pointList[0], "#ff0000" )
//            }
//        }
//    }

    private fun didSelectStudy(study: Study)
    {
        sharedViewModel.createStudyModel.setStudy(study)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectFirstButton(tag: Any?)
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

                    view?.let { view ->
                        sharedViewModel.currentConfiguration?.value?.let{ config ->

                            config.selectedEnumAreaUuid = ""

                            for (enumArea in config.enumAreas)
                            {
                                enumArea.selectedEnumerationTeamUuid = ""
                                enumArea.selectedCollectionTeamUuid = ""
                            }

                            sharedNetworkViewModel.setCurrentConfig(config)
                            sharedNetworkViewModel.networkHotspotModel.encryptionPassword = config.encryptionPassword
                            sharedNetworkViewModel.createHotspot(view)
                        }
                    }
                }

                kTaskTag -> {
                    findNavController().navigate( R.id.action_navigate_to_ManageEnumerationTeamsFragment )
                }

                kFileLocationTag -> {
                    exportToDefaultLocation()
                }
                else -> {}
            }
        }
    }

    override fun didSelectSecondButton(tag: Any?)
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            tag?.let { tag ->

                when(tag)
                {
                    kDeleteTag -> {
                        DAO.deleteAll()
                        findNavController().popBackStack()
                    }

                    kImportTag -> {
                        val intent = Intent()
                            .setType("*/*")
                            .setAction(Intent.ACTION_GET_CONTENT)
                        startActivityForResult(Intent.createChooser(intent, "Select an Enumeration"), REQUEST_CODE_PICK_FILE)
                    }

                    kExportTag -> {
                        ConfirmationDialog( activity, resources.getString(R.string.select_file_location), "", resources.getString(R.string.default_location), resources.getString(R.string.let_me_choose), kFileLocationTag, this, true)
                    }

                    kTaskTag -> {
                        findNavController().navigate( R.id.action_navigate_to_CreateSampleFragment )
                    }

                    kFileLocationTag -> {
                        exportToDevice()
                    }
                    else -> {}
                }
            }
        }
    }

    fun exportToDevice( )
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->

            config.selectedEnumAreaUuid = ""

            for (enumArea in config.enumAreas)
            {
                enumArea.selectedEnumerationTeamUuid = ""
                enumArea.selectedCollectionTeamUuid = ""
            }

            val user = (activity!!.application as MainApplication).user

            var userName = user!!.name.replace(" ", "" ).uppercase()

            if (userName.length > 3)
            {
                userName = userName.substring(0,3)
            }

            val role = user.role.toString().substring(0,1).uppercase()

            val formatter = DateTimeFormatter.ofPattern("yyMMdd-HHmm")
            val dateTime = LocalDateTime.now().format(formatter)

            var version = ""
            val versionName = BuildConfig.VERSION_NAME.split( "#" )
            if (versionName.size == 2)
            {
                version = versionName[1]
            }

            val fileName = "${role}-${userName}-${config.name}-${dateTime!!}-${version}.json"

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }

            startActivityForResult( intent, REQUEST_CODE_PICK_DIR )
        }
    }

    fun exportToDefaultLocation()
    {
        try
        {
            sharedViewModel.currentConfiguration?.value?.let { config ->

                config.selectedEnumAreaUuid = ""

                for (enumArea in config.enumAreas)
                {
                    enumArea.selectedEnumerationTeamUuid = ""
                    enumArea.selectedCollectionTeamUuid = ""
                }

                val packedConfig = config.pack()

                val user = (activity!!.application as MainApplication).user

                var userName = user!!.name.replace(" ", "" ).uppercase()

                if (userName.length > 3)
                {
                    userName = userName.substring(0,3)
                }

                val role = user.role.toString().substring(0,1).uppercase()

                val formatter = DateTimeFormatter.ofPattern("yyMMdd-HHmm")
                val dateTime = LocalDateTime.now().format(formatter)

                var version = ""
                val versionName = BuildConfig.VERSION_NAME.split( "#" )
                if (versionName.size == 2)
                {
                    version = versionName[1]
                }

                val fileName = "${role}-${userName}-${config.name}-${dateTime!!}-${version}.json"

                val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS + "/GPSSample/Configurations")
                root.mkdirs()
                val file = File(root, fileName)
                val writer = FileWriter(file)
                writer.append(packedConfig)
                writer.flush()
                writer.close()

                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved), Toast.LENGTH_SHORT).show()
            }
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
            Toast.makeText( activity!!.applicationContext, ex.stackTraceToString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun didSelectEnumArea(enumArea: EnumArea)
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            config.selectedEnumAreaUuid = enumArea.uuid
            sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                config.selectedStudyUuid = study.uuid
                sharedViewModel.enumAreaViewModel.setCurrentEnumArea(enumArea)
                ConfirmationDialog( activity, resources.getString(R.string.select_task), "", resources.getString(R.string.client), resources.getString(R.string.survey), kTaskTag, this, true)
            } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.no_study_ea), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        try
        {
            if (requestCode == REQUEST_CODE_PICK_DIR && resultCode == Activity.RESULT_OK)
            {
                data?.data?.let { uri ->
                    sharedViewModel.currentConfiguration?.value?.let { config ->
                        val packedConfig = config.pack()
                        activity!!.applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                            FileOutputStream(it.fileDescriptor).use {
                                it.write(packedConfig.toByteArray())
                                it.close()
                                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            else if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == Activity.RESULT_OK)
            {
                val uri = data?.data

                uri?.let { uri ->
                    sharedViewModel.currentConfiguration?.value?.let { currentConfig ->
                        try
                        {
                            activity!!.getContentResolver().openInputStream(uri)?.let { inputStream ->

                                binding.overlayView.visibility = View.VISIBLE

                                Thread {
                                    val text = inputStream.bufferedReader().readText()

                                    val config = Config.unpack( text, currentConfig.encryptionPassword )

                                    if (config == null)
                                    {
                                        activity!!.runOnUiThread {
                                            binding.overlayView.visibility = View.GONE
                                            InfoDialog( activity!!, resources.getString(R.string.error), resources.getString(R.string.import_failed), resources.getString(R.string.ok), null, null)
                                        }
                                    }
                                    else
                                    {
                                        DAO.instance().writableDatabase.beginTransaction()

                                        DAO.configDAO.createOrUpdateConfig( config )

                                        DAO.instance().writableDatabase.setTransactionSuccessful()
                                        DAO.instance().writableDatabase.endTransaction()

                                        DAO.configDAO.getConfig( config.uuid )?.let {
                                            activity!!.runOnUiThread {
                                                sharedViewModel.setCurrentConfig(it)
                                                updateOverview()
                                                binding.overlayView.visibility = View.GONE
                                                enumerationAreasAdapter.updateEnumAreas(it.enumAreas)
                                                InfoDialog( activity!!, resources.getString(R.string.success), resources.getString(R.string.import_succeeded), resources.getString(R.string.ok), null, null)
                                            }
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
        catch( ex: java.lang.Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
            Toast.makeText( activity!!.applicationContext, ex.stackTraceToString(), Toast.LENGTH_LONG).show()
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

                    var creationDate = Date().time

                    enumArea.vertices.map {
                        polygon.add( LatLon( creationDate++, it.latitude, it.longitude ))
                    }

                    // TODO! FIX THIS
//                    val enumerationTeam = DAO.enumerationTeamDAO.createOrUpdateEnumerationTeam( EnumerationTeam( enumArea.uuid, "E-Team-${enumArea.uuid}", polygon, enumArea.locations ))
//                    enumArea.enumerationTeams.add(enumerationTeam!!)

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

                    var creationDate = Date().time

                    enumArea.vertices.map {
                        polygon.add( LatLon( creationDate++, it.latitude, it.longitude ))
                    }

//                    val collectionTeam = DAO.collectionTeamDAO.createOrUpdateCollectionTeam( CollectionTeam( enumArea.uuid,"S-Team-${enumArea.uuid}", polygon, enumArea.locations ))
//                    enumArea.collectionTeams.add(collectionTeam!!)

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

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        motionEvent?.let {
            if (it.action == MotionEvent.ACTION_UP) {
                val bundle = Bundle()
                bundle.putBoolean( Keys.kEditMode.value, false )
                findNavController().navigate(R.id.action_navigate_to_CreateEnumerationAreaFragment, bundle)
            }
        }

        view?.performClick()

        return true
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}