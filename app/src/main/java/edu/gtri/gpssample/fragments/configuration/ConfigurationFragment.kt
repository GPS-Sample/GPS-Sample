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
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import edu.gtri.gpssample.database.EnumAreaDAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentConfigurationBinding
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.NearbySessionClientManager
import edu.gtri.gpssample.managers.NearbySessionHostManager
import edu.gtri.gpssample.managers.NearbySessionState
import edu.gtri.gpssample.managers.PerformanceManager
import edu.gtri.gpssample.ui.compose.ComposableCheckboxDialogHost
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableNearbySessionStatusDialogHost
import edu.gtri.gpssample.ui.compose.ComposableNotificationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableSelectionDialogHost
import edu.gtri.gpssample.utils.ZipUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.views.MapView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class ConfigurationFragment : Fragment(), View.OnTouchListener
{
    private lateinit var studiesAdapter: StudiesAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var enumerationAreasAdapter: ConfigurationAdapter
    private var nearbySessionHostManager: NearbySessionHostManager? = null
    private var nearbySessionClientManager: NearbySessionClientManager? = null
    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!
    private var includeConfig = false
    private var includeImages = false
    private val REQUEST_CODE_PICK_CONFIG_DIR    = 1001
    private val REQUEST_CONFIGURATION           = 1003
    private lateinit var composableCheckboxDialogHost: ComposableCheckboxDialogHost
    private lateinit var composableSelectionDialogHost: ComposableSelectionDialogHost
    private lateinit var composableNotificationDialogHost: ComposableNotificationDialogHost
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost
    private lateinit var composableNearbySessionStatusDialogHost: ComposableNearbySessionStatusDialogHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm: ConfigurationViewModel by activityViewModels()

        sharedViewModel = vm
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

        composableCheckboxDialogHost = ComposableCheckboxDialogHost()
        composableSelectionDialogHost = ComposableSelectionDialogHost()
        composableNotificationDialogHost = ComposableNotificationDialogHost()
        composableConfirmationDialogHost = ComposableConfirmationDialogHost()
        composableNearbySessionStatusDialogHost = ComposableNearbySessionStatusDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableCheckboxDialogHost.Content()
            composableSelectionDialogHost.Content()
            composableNotificationDialogHost.Content()
            composableConfirmationDialogHost.Content()
            composableNearbySessionStatusDialogHost.Content()
        }

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

//        binding.deleteImageView.setOnClickListener {
//            ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.delete_configuration_message),
//                resources.getString(R.string.no), resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
//                when( buttonPressed )
//                {
//                    ConfirmationDialog.ButtonPress.Left -> {
//                    }
//                    ConfirmationDialog.ButtonPress.Right -> {
//                        sharedViewModel.currentConfiguration?.value?.let { config ->
//                            Thread {
//                                DAO.configDAO.deleteConfig( config )
//                                activity!!.runOnUiThread {
//                                    findNavController().popBackStack()
//                                }
//                            }.start()
//                        }
//                    }
//                    ConfirmationDialog.ButtonPress.None -> {
//                    }
//                }
//            }
//        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.importButton.setOnClickListener {
            composableSelectionDialogHost.show(
                title = resources.getString(R.string.import_field_data),
                message = resources.getString(R.string.select_import_method),
                items = listOf(resources.getString(R.string.qr_code), resources.getString(R.string.file_system)),
            ) { selection ->
                if (selection == resources.getString(R.string.qr_code)) {
                    val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                    getQrCode.launch(intent)
                }
                else if (selection == resources.getString(R.string.file_system)) {
                    val intent = Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT)
                    startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_configuration)), REQUEST_CONFIGURATION)
                }
            }
        }

        binding.exportButton.setOnClickListener {
            composableSelectionDialogHost.show(
                title = resources.getString(R.string.export_configuration),
                message = resources.getString(R.string.select_export_message),
                items = listOf(resources.getString(R.string.qr_code), resources.getString(R.string.file_system)),
            ) { selection ->
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    config.selectedEnumAreaUuid = ""

                    sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                        config.selectedStudyUuid = study.uuid
                    }

                    binding.progressOverlayView.visibility = View.VISIBLE

                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            // this may take a while...
                            for (enumArea in config.enumAreas) {
                                enumArea.selectedEnumerationTeamUuid = ""
                                enumArea.selectedCollectionTeamUuid = ""
                            }
                        }

                        // back on the main thread...

                        binding.progressOverlayView.visibility = View.GONE

                        if (selection == resources.getString(R.string.qr_code)) {
                            composableNearbySessionStatusDialogHost.show(title = resources.getString(R.string.export_configuration))
                            {
                                nearbySessionHostManager?.stopHosting()
                            }

                            config.enumAreas.clear() // make sure we don't send EA's with the config, they'll be transferred separately

                            nearbySessionHostManager = NearbySessionHostManager( requireContext().applicationContext, config )

                            viewLifecycleOwner.lifecycleScope.launch {
                                repeatOnLifecycle(Lifecycle.State.STARTED )
                                {
                                    nearbySessionHostManager?.state?.collect { state ->
                                        composableNearbySessionStatusDialogHost.updateState(state)
                                    }
                                }
                            }

                            nearbySessionHostManager?.startHosting()
                        }
                        else if (selection == resources.getString(R.string.file_system))
                        {
                            val items = ArrayList<String>()
                            items.add( "Configuration Files" )
                            items.add( "Image Files" )

                            composableCheckboxDialogHost.show(
                                title = "Select Export Items",
                                items = items,
                                isChecked = emptyList(),
                                onContinue = { selections ->
                                    includeConfig = false
                                    includeImages = false

                                    for (selection in selections) {
                                        if (selection == items[0]) includeConfig = true
                                        if (selection == items[1]) includeImages = true
                                    }

                                    if (includeConfig || includeImages)
                                    {
                                        composableSelectionDialogHost.show(
                                            title = resources.getString(R.string.select_file_location),
                                            message = "",
                                            items = listOf(resources.getString(R.string.default_location), resources.getString(R.string.let_me_choose)),
                                        ) { selection ->
                                            if (selection == resources.getString(R.string.default_location))
                                            {
                                                config.enumAreas.clear() // make sure we don't send EA's with the config, they'll be transferred separately

                                                val zipUtils = ZipUtils()

                                                composableNearbySessionStatusDialogHost.show(title = resources.getString(R.string.export_configuration))
                                                {
                                                    zipUtils.cancel()
                                                }

                                                viewLifecycleOwner.lifecycleScope.launch {
                                                    zipUtils.state.collect { state ->
                                                        composableNearbySessionStatusDialogHost.updateState(state)
                                                    }
                                                }

                                                PerformanceManager.startTimer()

                                                zipUtils.zipToPublicDocuments( requireActivity(), config, getFileName(), "Configurations", includeConfig, includeImages ) { success ->
                                                    if (success)
                                                    {
                                                        composableNotificationDialogHost.show(title = resources.getString(R.string.success), message = resources.getString(R.string.export_succeeded))
                                                    }
                                                    else
                                                    {
                                                        composableNotificationDialogHost.show(title = resources.getString(R.string.oops), message = resources.getString(R.string.export_failed))
                                                    }

                                                    composableNearbySessionStatusDialogHost.dismiss()

                                                    Log.d( "xxx", "Export time : ${PerformanceManager.elapsedTime()}")
                                                }
                                            }
                                            else if (selection == resources.getString(R.string.let_me_choose))
                                            {
                                                exportToDevice()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        binding.mapOverlayView.setOnTouchListener(this)

        binding.progressOverlayView.isClickable = true
        binding.progressOverlayView.isFocusable = true

        sharedViewModel.currentConfiguration?.value?.let { config ->

            MapManager.instance().selectMap( requireActivity(), config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null ) { mapView ->

                binding.osmLabel.visibility = if (mapView is MapView) View.VISIBLE else View.GONE

                if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null)
                        {
                            val point = Point.fromLngLat( location.longitude, location.latitude )
                            MapManager.instance().centerMap( point, mapView )
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

            if (!config.geofenceIsEnabled)
            {
                binding.geofenceTextView.visibility = View.GONE
            }

            binding.configNameTextView.text = config.name

            studiesAdapter = StudiesAdapter(config.studies)
            studiesAdapter.didSelectStudy = this::didSelectStudy

            binding.locationSwitch.isChecked = config.allowManualLocationEntry
            binding.supervisorEditSwitch.isChecked = config.allowSupervisorEdits

            if(config.studies.count() > 0)
            {
                sharedViewModel.createStudyModel.setCurrentStudy(config.studies[0])
            }

            val enumAreaSummaries = DAO.enumAreaDAO.getEnumAreaSummary(config.uuid )

            enumerationAreasAdapter = ConfigurationAdapter(enumAreaSummaries )
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
        binding.geofenceSwitch.isChecked = config.geofenceIsEnabled
        binding.geofenceTextView.text = sharedViewModel.currentConfigurationGeofenceBufferValue

        if (!config.proximityWarningIsEnabled)
        {
            binding.proximityWarningTextView.visibility = View.GONE
        }

        if (!config.geofenceIsEnabled)
        {
            binding.geofenceTextView.visibility = View.GONE
        }

        updateOverview()
    }

    override fun onResume()
    {
        super.onResume()

        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.ConfigurationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun updateOverview()
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            viewLifecycleOwner.lifecycleScope.launch {
                val summaryInfo = withContext(Dispatchers.IO) {
                    DAO.configDAO.getConfigSummary(config.uuid )
                }

                // back on main thread

                binding.numberOfEnumerationAreasTextView.text = "${summaryInfo.enumAreaCount}"
                binding.numberEnumeratedTextView.text = "${summaryInfo.enumerationCount}"
                binding.numberEligibleTextView.text = "${summaryInfo.eligibleCount}"
                binding.numberSampledTextView.text = "${summaryInfo.sampledCount}"
                binding.numberSurveyedTextView.text = "${summaryInfo.surveyedCount}"
                binding.numberRemainingTextView.text = "${summaryInfo.sampledCount - summaryInfo.surveyedCount}"
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

                composableNearbySessionStatusDialogHost.show(title = getString(R.string.import_configuration))
                {
                    nearbySessionClientManager?.cancel()
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED )
                    {
                        nearbySessionClientManager?.state?.collect { state ->
                            composableNearbySessionStatusDialogHost.updateState(state)
                        }
                    }
                }

                nearbySessionClientManager?.connect( sessionId ) { config ->

                    // make sure that we're updating the current config!!
                    sharedViewModel.currentConfiguration?.value?.let { currentConfig ->
                        if (config.uuid != currentConfig.uuid)
                        {
                            composableNotificationDialogHost.show(title = resources.getString(R.string.oops), message = resources.getString(R.string.import_mismatch))
                            composableNearbySessionStatusDialogHost.dismiss()
                            return@connect
                        }
                    }

                    composableNearbySessionStatusDialogHost.updateState(NearbySessionState.Message(resources.getString(R.string.saving_configuration)))

                    var enumAreaSummaries : List<EnumAreaDAO.EnumAreaSummary>? = null

                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO)
                        {
                            DAO.configDAO.createOrUpdateConfig( config, config.version )

                            enumAreaSummaries = DAO.enumAreaDAO.getEnumAreaSummary(config.uuid )

                            // TODO!!! re-fetch the config from the db
                        }

                        // back on the main thread...

                        enumAreaSummaries?.let {
                            enumerationAreasAdapter.updateEnumAreas(it )
                        }

                        composableNearbySessionStatusDialogHost.dismiss()

                        sharedViewModel.setCurrentConfig( config )

                        refreshView( config )

                        composableNotificationDialogHost.show(title = resources.getString(R.string.success), message = resources.getString(R.string.import_succeeded))
                    }
                }
            }
        }
    }

    private fun didSelectStudy(study: Study)
    {
        sharedViewModel.createStudyModel.setCurrentStudy(study)
    }

    fun getFileName() : String
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            val user = (requireActivity().application as MainApplication).user

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

    fun exportToDevice()
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

    private fun didSelectEnumArea(uuid: String)
    {
        if (sharedViewModel.createStudyModel.currentStudy == null)
        {
            Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.no_study_ea), Toast.LENGTH_SHORT).show()
            return
        }

        sharedViewModel.currentConfiguration?.value?.let { config ->
            sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                viewLifecycleOwner.lifecycleScope.launch {
                    binding.progressOverlayView.visibility = View.VISIBLE

                    val enumArea = withContext(Dispatchers.IO) {
                        DAO.enumAreaDAO.getEnumArea( uuid )
                    }

                    // back on main thread

                    binding.progressOverlayView.visibility = View.GONE

                    enumArea?.let { enumArea ->
                        config.selectedEnumAreaUuid = uuid
                        config.enumAreas.clear()
                        config.enumAreas.add( enumArea )
                        config.selectedStudyUuid = study.uuid
                        sharedViewModel.enumAreaViewModel.setCurrentEnumArea(enumArea)

                        composableSelectionDialogHost.show(
                            title = resources.getString(R.string.select_task),
                            message = "",
                            items = listOf(resources.getString(R.string.client), resources.getString(R.string.survey)),
                        ) { selection ->
                            if (selection == resources.getString(R.string.client))
                            {
                                findNavController().navigate( R.id.action_navigate_to_ManageEnumerationTeamsFragment )
                            }
                            else if (selection == resources.getString(R.string.survey))
                            {
                                findNavController().navigate( R.id.action_navigate_to_CreateSampleFragment )
                            }
                        }
                    }
                }
            }
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
                        config.enumAreas.clear() // make sure we don't send EA's with the config, they'll be transferred separately

                        val zipUtils = ZipUtils()

                        composableNearbySessionStatusDialogHost.show(title = resources.getString(R.string.import_configuration))
                        {
                            zipUtils.cancel()
                        }

                        viewLifecycleOwner.lifecycleScope.launch {
                            zipUtils.state.collect { state ->
                                composableNearbySessionStatusDialogHost.updateState(state)
                            }
                        }

                        PerformanceManager.startTimer()

                        zipUtils.zipToUri( requireActivity(), config, getFileName(), includeConfig, includeImages,uri ) { success ->
                            if (success)
                            {
                                composableNotificationDialogHost.show(title = resources.getString(R.string.success), message = resources.getString(R.string.export_succeeded))
                            }
                            else
                            {
                                composableNotificationDialogHost.show(title = resources.getString(R.string.oops), message = resources.getString(R.string.export_failed))
                            }

                            composableNearbySessionStatusDialogHost.dismiss()

                            Log.d( "xxx", "Export time : ${PerformanceManager.elapsedTime()}")
                        }
                    }
                }
            }
            else if (requestCode == REQUEST_CONFIGURATION && resultCode == Activity.RESULT_OK)
            {
                val uri = data?.data

                uri?.let { uri ->
                    sharedViewModel.currentConfiguration?.value?.let { currentConfig ->
                        try
                        {
                            val zipUtils = ZipUtils()

                            composableNearbySessionStatusDialogHost.show(title = resources.getString(R.string.import_configuration))
                            {
                                zipUtils.cancel()
                            }


                            viewLifecycleOwner.lifecycleScope.launch {
                                zipUtils.state.collect { state ->
                                    composableNearbySessionStatusDialogHost.updateState(state)
                                }
                            }

                            PerformanceManager.startTimer()

                            zipUtils.unzip(requireActivity(), uri, currentConfig.encryptionPassword ) { result ->
                                val config = result.first
                                val errorCode = result.second

                                if (config == null)
                                {
                                    if (errorCode != ErrorCode.None)
                                    {
                                        val message = if (errorCode == ErrorCode.DecryptError) resources.getString(R.string.password_error ) else resources.getString(R.string.import_failed)
                                        composableNotificationDialogHost.show(title = resources.getString(R.string.error), message = message)
                                    }
                                }
                                else
                                {
                                    DAO.configDAO.createOrUpdateConfig( config,config.version )

                                    DAO.configDAO.getConfig( config.uuid )?.let {
                                        sharedViewModel.setCurrentConfig( it )
                                    	refreshView( it )
                                    }

                                    composableNotificationDialogHost.show(title = resources.getString(R.string.success), message = resources.getString(R.string.import_succeeded))
                                }

                                composableNearbySessionStatusDialogHost.dismiss()

                                Log.d( "xxx", "Import time : ${PerformanceManager.elapsedTime()}")
                            }
                        }
                        catch( ex: java.lang.Exception )
                        {
                            binding.progressOverlayView.visibility = View.GONE
                            composableNotificationDialogHost.show(title = resources.getString(R.string.error), message = resources.getString(R.string.import_failed))
                        }
                    }
                }
            }
        }
        catch( ex: java.lang.Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
            Toast.makeText( requireActivity().applicationContext, ex.stackTraceToString(), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_test, menu)
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        motionEvent?.let {
            if (it.action == MotionEvent.ACTION_UP) {
                val bundle = Bundle()
                bundle.putBoolean( Keys.kEditMode.value, false )
                sharedViewModel.currentConfiguration?.value?.let { config ->

                    binding.progressOverlayView.visibility = View.VISIBLE

                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO)
                        {
                            // TODO: Performance Inprovement: Check to see if each EnumArea really needs to be re-loaded
                            config.enumAreas = DAO.enumAreaDAO.getEnumAreas( config )
                        }

                        // back on the main thread...
                        binding.progressOverlayView.visibility = View.GONE

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
        }

        view?.performClick()

        return true
    }

    override fun onDestroyView()
    {
        binding.mapOverlayView.setOnTouchListener(null)

        composableNearbySessionStatusDialogHost.dismiss()

        nearbySessionHostManager?.stopHosting()
        nearbySessionClientManager?.cancel()

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
