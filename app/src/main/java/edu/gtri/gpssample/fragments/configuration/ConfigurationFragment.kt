/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.configuration

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Point
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.MapEngine
import edu.gtri.gpssample.constants.ResultCode
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentConfigurationBinding
import edu.gtri.gpssample.dialogs.BusyIndicatorDialog
import edu.gtri.gpssample.dialogs.CheckboxDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InfoDialog
import edu.gtri.gpssample.dialogs.NotificationDialog
import edu.gtri.gpssample.dialogs.NearbySessionStatusDialog
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.NearbySessionClientManager
import edu.gtri.gpssample.managers.NearbySessionHostManager
import edu.gtri.gpssample.utils.ZipUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.*
import org.osmdroid.views.MapView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class ConfigurationFragment : Fragment(), View.OnTouchListener, BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private lateinit var studiesAdapter: StudiesAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var enumerationAreasAdapter: ConfigurationAdapter

    private var nearbySessionHostManager: NearbySessionHostManager? = null
    private var nearbySessionStatusDialog: NearbySessionStatusDialog? = null
    private var nearbySessionClientManager: NearbySessionClientManager? = null
    private var osmMapListener: MapListener? = null
    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!
    private var includeConfig = false
    private var includeImages = false
    private val REQUEST_CODE_PICK_CONFIG_DIR    = 1001
    private val REQUEST_CONFIGURATION           = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm: ConfigurationViewModel by activityViewModels()

        sharedViewModel = vm

        sharedViewModel.currentFragment = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        if (sharedViewModel.currentConfiguration == null || (requireActivity().application as MainApplication).user == null)
        {
            findNavController().navigate(R.id.action_navigate_to_MainFragment, null,
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.action_navigate_to_MainFragment, false)
                    .build())
            return
        }

        osmMapListener = MapManager.instance().createOsmMapListener( binding.osmMapView, binding.northUpImageView )

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
                resources.getString(R.string.no), resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
                when( buttonPressed )
                {
                    ConfirmationDialog.ButtonPress.Left -> {
                    }
                    ConfirmationDialog.ButtonPress.Right -> {
                        sharedViewModel.currentConfiguration?.value?.let { config ->
                            val busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.delete_config), this, false )
                            Thread {
                                DAO.configDAO.deleteConfig( config )
                                activity!!.runOnUiThread {
                                    busyIndicatorDialog.alertDialog.cancel()
                                    findNavController().popBackStack()
                                }
                            }.start()
                        }
                    }
                    ConfirmationDialog.ButtonPress.None -> {
                    }
                }
            }
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.importButton.setOnClickListener {
            ConfirmationDialog( activity, resources.getString(R.string.import_field_data), resources.getString(R.string.select_import_method_message),
                resources.getString(R.string.qr_code), resources.getString(R.string.file_system), null, false ) { buttonPressed, tag ->
                when( buttonPressed )
                {
                    ConfirmationDialog.ButtonPress.Left -> {
                        val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                        getQrCode.launch(intent)
                    }
                    ConfirmationDialog.ButtonPress.Right -> {
                        Toast.makeText(activity!!.applicationContext, resources.getString(R.string.select_configuration_file), Toast.LENGTH_LONG).show()
                        val intent = Intent()
                            .setType("*/*")
                            .setAction(Intent.ACTION_GET_CONTENT)
                        startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_configuration)), REQUEST_CONFIGURATION)
                    }
                    ConfirmationDialog.ButtonPress.None -> {
                    }
                }
            }
        }

        binding.exportButton.setOnClickListener {
            ConfirmationDialog( activity, resources.getString(R.string.export_configuration), resources.getString(R.string.select_export_message), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), null, false ) { buttonPressed, tag ->
                sharedViewModel.currentConfiguration?.value?.let{ config ->
                    config.selectedEnumAreaUuid = ""

                    sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                        config.selectedStudyUuid = study.uuid
                    }

                    binding.overlayView.visibility = View.VISIBLE

                    viewLifecycleOwner.lifecycleScope.launch {
                        Log.d("MEM", "minimal config = ${usedMB()} MB")

                        withContext(Dispatchers.IO) {
                            // this may take a while...
                            for (enumArea in config.enumAreas)
                            {
                                enumArea.selectedEnumerationTeamUuid = ""
                                enumArea.selectedCollectionTeamUuid = ""
//                                DAO.enumAreaDAO.loadLazyLocations( enumArea ) // if needed
                            }
                        }

                        Log.d("MEM", "full config = ${usedMB()} MB")

                        // back on the main thread...

                        binding.overlayView.visibility = View.GONE

                        when( buttonPressed )
                        {
                            ConfirmationDialog.ButtonPress.Left -> {
                                nearbySessionStatusDialog = NearbySessionStatusDialog(requireContext(), resources.getString( R.string.export_configuration )) {
                                    nearbySessionHostManager?.stopHosting()
                                    nearbySessionStatusDialog = null
                                }

                                nearbySessionHostManager = NearbySessionHostManager( requireContext().applicationContext, config )

                                viewLifecycleOwner.lifecycleScope.launch {
                                    repeatOnLifecycle(Lifecycle.State.STARTED )
                                    {
                                        nearbySessionHostManager?.state?.collect { state ->
                                            nearbySessionStatusDialog?.updateState(state)
                                        }
                                    }
                                }

                                nearbySessionHostManager?.startHosting()
                            }

                            ConfirmationDialog.ButtonPress.Right -> {
                                val items = ArrayList<String>()
                                items.add( "Configuration Files" )
                                items.add( "Image Files" )
                                CheckboxDialog( activity!!, "Select the file types to export", items ) { selections ->
                                    includeConfig = false
                                    includeImages = false

                                    for (selection in selections) {
                                        if (selection == items[0]) includeConfig = true
                                        if (selection == items[1]) includeImages = true
                                    }

                                    if (includeConfig || includeImages)
                                    {
                                        ConfirmationDialog( activity, resources.getString(R.string.select_file_location), "", resources.getString(R.string.default_location), resources.getString(R.string.let_me_choose), null, true) { buttonPressed, tag ->
                                            when( buttonPressed )
                                            {
                                                ConfirmationDialog.ButtonPress.Left -> {
                                                    ZipUtils.zipToPublicDocuments( requireActivity(), config, getFileName(), "Configurations", includeConfig, includeImages, false ) { success ->
                                                        if (success)
                                                        {
                                                            Toast.makeText( activity!!.applicationContext, resources.getString(R.string.export_succeeded), Toast.LENGTH_LONG).show()
                                                        }
                                                        else
                                                        {
                                                            NotificationDialog( activity!!, resources.getString(R.string.oops), resources.getString(R.string.export_failed))
                                                        }
                                                    }
                                                }
                                                ConfirmationDialog.ButtonPress.Right -> {
                                                    exportToDevice()
                                                }
                                                ConfirmationDialog.ButtonPress.None -> {
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            ConfirmationDialog.ButtonPress.None -> {
                            }
                        }
                    }
                }
            }
        }

        binding.mapOverlayView.setOnTouchListener(this)

        binding.overlayView.isClickable = true
        binding.overlayView.isFocusable = true

        sharedViewModel.currentConfiguration?.value?.let { config ->

            val zoom = sharedViewModel.currentZoomLevel?.value ?: 0.0

            MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null, zoom ) { mapView ->

                binding.osmLabel.visibility = if (mapView is MapView) View.VISIBLE else View.GONE

                if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null)
                        {
                            val point = Point.fromLngLat( location.longitude, location.latitude )
                            sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                                MapManager.instance().centerMap( point, currentZoomLevel, mapView )
                            }
                        }
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
                sharedViewModel.createStudyModel.setCurrentStudy(config.studies[0])
            }

            val enumAreaSummary = DAO.configDAO.getEnumAreaSummary(config.uuid )

            enumerationAreasAdapter = ConfigurationAdapter( config.enumAreas, enumAreaSummary )
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

    fun refreshView( config: Config )
    {
        studiesAdapter.updateStudies( config.studies )
        enumerationAreasAdapter.updateEnumAreas( config.enumAreas )

        binding.configNameTextView.text = config.name

        val items = ArrayList<String>()
        val mapEngines = resources.getTextArray( R.array.map_engines )

        for (mapEngine in mapEngines)
        {
            items.add( mapEngine.toString() )
        }

        binding.mapEngineText.text = items[config.mapEngineIndex]

        binding.configPreferredUnitsText.text = sharedViewModel.currentConfigurationDistanceFormat
        binding.configDateFormatText.text = sharedViewModel.currentConfigurationDateFormat
        binding.configTimeFormatText.text = sharedViewModel.currentConfigurationTimeFormat
        binding.minGpsPrecisionEditText.text = sharedViewModel.currentConfigurationMinimumGpsPrecision
        binding.encryptionPasswordTextView.text = config.encryptionPassword
        binding.locationSwitch.isChecked = config.allowManualLocationEntry
        binding.subaddressRequiredSwitch.isChecked = config.subaddressIsrequired
        binding.autoIncrementSubaddressSwitch.isChecked = config.autoIncrementSubaddress
        binding.proximityWarningSwitch.isChecked = config.proximityWarningIsEnabled
        binding.proximityWarningTextView.text = sharedViewModel.currentConfigurationProximityWarning

        if (!config.proximityWarningIsEnabled)
        {
            binding.proximityWarningTextView.visibility = View.GONE
        }

        updateOverview()
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ConfigurationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun updateOverview()
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            viewLifecycleOwner.lifecycleScope.launch {
                val summaryInfo = withContext(Dispatchers.IO) {
                    DAO.configDAO.getConfigSummary(config.uuid )
                }

                // back on main thread
                val numRemaining = summaryInfo.sampledCount - summaryInfo.surveyedCount

                binding.numberOfEnumerationAreasTextView.text = "${config.enumAreas.size}"
                binding.numberEnumeratedTextView.text = "${summaryInfo.enumerationCount}"
                binding.numberEligibleTextView.text = "${summaryInfo.eligibleCount}"
                binding.numberSampledTextView.text = "${summaryInfo.sampledCount}"
                binding.numberSurveyedTextView.text = "${summaryInfo.surveyedCount}"
                binding.numberRemainingTextView.text = "${numRemaining}"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val getQrCode = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {
        if (it.resultCode == ResultCode.BarcodeScanned.value)
        {
            // payload contains the sessionId
            it.data!!.getStringExtra(Keys.kPayload.value )?.let { sessionId ->
                nearbySessionClientManager = NearbySessionClientManager( requireContext().applicationContext )

                nearbySessionStatusDialog = NearbySessionStatusDialog(requireContext(),getString(R.string.import_configuration))
                {
                    nearbySessionClientManager?.cancel()
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED )
                    {
                        nearbySessionClientManager?.state?.collect { state ->
                            nearbySessionStatusDialog?.updateState(state)
                        }
                    }
                }

                Log.d("xxx", "before import = ${usedMB()} MB")

                nearbySessionClientManager?.connect( sessionId ) { config, enumerationItems ->
                    nearbySessionStatusDialog?.setStatus( "Saving Configuration..." )

                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO)
                        {
                            DAO.configDAO.createOrUpdateConfig( config,config.version )
                            DAO.enumerationItemDAO.createOrUpdateEnumerationItems( enumerationItems )

                            // TODO!!! re-fetch the config from the db
                        }

                        // back on the main thread...

                        System.gc()
                        Log.d("xxx", "after import = ${usedMB()} MB")

                        nearbySessionStatusDialog?.dismiss()

                        sharedViewModel.setCurrentConfig( config )

                        refreshView( config )
                    }
                }
            }
        }
    }

    fun usedMB(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    }

    private fun didSelectStudy(study: Study)
    {
        sharedViewModel.createStudyModel.setCurrentStudy(study)
    }

    fun getFileName() : String
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            val user = (activity!!.application as MainApplication).user

            var userName = user!!.name.replace(" ", "" ).uppercase()

            if (userName.length > 3)
            {
                userName = userName.substring(0,3)
            }

            val role = user.role.toString().substring(0,1).uppercase()

            val formatter = DateTimeFormatter.ofPattern("yyMMdd-HHmm")
            val dateTime = LocalDateTime.now().format(formatter)

            val versionName = BuildConfig.VERSION_NAME

            return "${role}-${userName}-${config.name}-${dateTime!!}-${versionName}"
        }

        return ""
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

            val zipFileName = getFileName() + ".zip"

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(Intent.EXTRA_TITLE, zipFileName)
            }

            startActivityForResult( intent, REQUEST_CODE_PICK_CONFIG_DIR )
        }
    }

    private fun didSelectEnumArea(enumArea: EnumArea)
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            config.selectedEnumAreaUuid = enumArea.uuid
            sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                config.selectedStudyUuid = study.uuid
                sharedViewModel.enumAreaViewModel.setCurrentEnumArea(enumArea)
                ConfirmationDialog( activity, resources.getString(R.string.select_task), "", resources.getString(R.string.client), resources.getString(R.string.survey), null, true ) { buttonPressed, tag ->
                    when( buttonPressed )
                    {
                        ConfirmationDialog.ButtonPress.Left -> {
                            findNavController().navigate( R.id.action_navigate_to_ManageEnumerationTeamsFragment )
                        }
                        ConfirmationDialog.ButtonPress.Right -> {
                            findNavController().navigate( R.id.action_navigate_to_CreateSampleFragment )
                        }
                        ConfirmationDialog.ButtonPress.None -> {
                        }
                    }
                }

            } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.no_study_ea), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        try
        {
            if (requestCode == REQUEST_CODE_PICK_CONFIG_DIR && resultCode == Activity.RESULT_OK)
            {
                data?.data?.let { uri ->
                    sharedViewModel.currentConfiguration?.value?.let { config ->
                        ZipUtils.zipToUri( requireActivity(), config, getFileName(), includeConfig, includeImages, false, uri ) { error ->
                            if (error.isEmpty())
                            {
                                Toast.makeText( activity!!.applicationContext, resources.getString(R.string.export_succeeded), Toast.LENGTH_LONG).show()
                            }
                            else
                            {
                                NotificationDialog( activity!!, resources.getString(R.string.oops), resources.getString(R.string.export_failed))
                            }
                        }
                    }
                }
            }
            else if (requestCode == REQUEST_CONFIGURATION && resultCode == Activity.RESULT_OK)
            {
                val uri = data?.data

                uri?.let { uri ->
                    sharedViewModel.currentConfiguration?.value?.let { currentConfig ->
                        binding.overlayView.visibility = View.VISIBLE

                        try
                        {
                            ZipUtils.unzip( activity!!, uri, currentConfig.encryptionPassword ) { result ->
                                val config = result.first
                                val errorCode = result.second

                                if (config == null)
                                {
                                    binding.overlayView.visibility = View.GONE
                                    val message = if (errorCode == Config.ErrorCode.PasswordError) resources.getString(R.string.password_error ) else resources.getString(R.string.import_failed)
                                    InfoDialog( activity!!, resources.getString(R.string.error), message, resources.getString(R.string.ok), null, null)
                                }
                                else
                                {
                                    DAO.configDAO.createOrUpdateConfig( config,config.version )

                                    sharedViewModel.setCurrentConfig( config )

                                    refreshView( config )

                                    binding.overlayView.visibility = View.GONE
                                    InfoDialog( activity!!, resources.getString(R.string.success), resources.getString(R.string.import_succeeded), resources.getString(R.string.ok), null, null)
                                }
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

    override fun didPressCancelButton()
    {
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        motionEvent?.let {
            if (it.action == MotionEvent.ACTION_UP) {
                val bundle = Bundle()
                bundle.putBoolean( Keys.kEditMode.value, false )
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    if (config.mapEngineIndex == MapEngine.OpenStreetMap.value)
                    {
                        findNavController().navigate(R.id.action_navigate_to_CreateOsmEnumerationAreaFragment, bundle)
                    }
                    else if (config.mapEngineIndex == MapEngine.MapBox.value)
                    {
                        findNavController().navigate(R.id.action_navigate_to_CreateEnumerationAreaFragment, bundle)
                    }
                }
            }
        }

        view?.performClick()

        return true
    }

    override fun onDestroyView()
    {
        binding.mapOverlayView.setOnTouchListener(null)

        osmMapListener?.let {
            MapManager.instance().onFragmentDestroyed( binding.osmMapView, it )
            osmMapListener = null
        }

        nearbySessionStatusDialog?.dismiss()
        nearbySessionStatusDialog = null

        nearbySessionHostManager?.stopHosting()
        nearbySessionClientManager?.cancel()

        sharedViewModel.currentFragment = null
        binding.enumAreasRecycler.adapter = null
        binding.studiesRecycler.adapter = null

        _binding = null

        super.onDestroyView()
    }

    override fun onDestroy()
    {
        nearbySessionHostManager?.stopHosting()
        nearbySessionClientManager?.shutdown()

        super.onDestroy()
    }
}