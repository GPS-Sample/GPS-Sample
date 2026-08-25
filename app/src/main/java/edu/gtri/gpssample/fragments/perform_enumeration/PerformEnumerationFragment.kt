/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.perform_enumeration

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.*
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformEnumerationBinding
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.NearbySessionHostManager
import edu.gtri.gpssample.managers.PerformanceManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.ui.compose.ComposableBusyIndicatorDialogHost
import edu.gtri.gpssample.ui.compose.ComposableCheckboxDialogHost
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableEnumerationHelpDialogHost
import edu.gtri.gpssample.ui.compose.ComposableInputDialogHost
import edu.gtri.gpssample.ui.compose.ComposableMapLegendDialogHost
import edu.gtri.gpssample.ui.compose.ComposableNearbySessionStatusDialogHost
import edu.gtri.gpssample.ui.compose.ComposableNotificationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableSelectionDialogHost
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.utils.ZipUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class PerformEnumerationFragment : Fragment(),
    View.OnTouchListener,
    MapManager.MapTileCacheDelegate
{
    private lateinit var user: User
    private lateinit var mapView: View
    private lateinit var enumArea: EnumArea
    private lateinit var enumerationTeam: EnumerationTeam
    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var performEnumerationAdapter: PerformEnumerationAdapter
    private lateinit var mapboxMapClickListener: OnMapClickListener
    private var lastCenterPoint: Point? = null
    private var isHandlingTapEvent = false
    private var _binding: FragmentPerformEnumerationBinding? = null
    private val binding get() = _binding!!
    private var lastBreadcrumbGroupId = ""
    private var dropMode = false
    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: Point? = null
    private val enumerationTeamLocations = ArrayList<Location>()
    private var includeConfig = false
    private var includeImages = false
    private var maxSubaddress = 0
    private var nearbySessionHostManager: NearbySessionHostManager? = null
    private val REQUEST_CODE_PICK_CONFIG_DIR = 1001
    private val selectedTeamNames = ArrayList<String>()
    private val selectedBreadcrumbs = ArrayList<Breadcrumb>()
    private var isShowingBreadcrumbs = false
    private lateinit var composableInputDialogHost: ComposableInputDialogHost
    private lateinit var composableCheckboxDialogHost: ComposableCheckboxDialogHost
    private lateinit var composableMapLegendDialogHost: ComposableMapLegendDialogHost
    private lateinit var composableSelectionDialogHost: ComposableSelectionDialogHost
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost
    private lateinit var composableNotificationDialogHost: ComposableNotificationDialogHost
    private lateinit var composableBusyIndicatorDialogHost: ComposableBusyIndicatorDialogHost
    private lateinit var composableEnumerationHelpDialogHost: ComposableEnumerationHelpDialogHost
    private lateinit var composableNearbySessionStatusDialogHost: ComposableNearbySessionStatusDialogHost

    enum class BreadcrumbRecordingState
    {
        OFF,
        RECORDING,
        PAUSE,
    }

    private var breadcrumbRecordingState = BreadcrumbRecordingState.OFF

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentPerformEnumerationBinding.inflate(inflater, container, false)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        composableInputDialogHost = ComposableInputDialogHost()
        composableCheckboxDialogHost = ComposableCheckboxDialogHost()
        composableMapLegendDialogHost = ComposableMapLegendDialogHost()
        composableSelectionDialogHost = ComposableSelectionDialogHost()
        composableConfirmationDialogHost = ComposableConfirmationDialogHost()
        composableNotificationDialogHost = ComposableNotificationDialogHost()
        composableBusyIndicatorDialogHost = ComposableBusyIndicatorDialogHost()
        composableEnumerationHelpDialogHost = ComposableEnumerationHelpDialogHost()
        composableNearbySessionStatusDialogHost = ComposableNearbySessionStatusDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableInputDialogHost.Content()
            composableCheckboxDialogHost.Content()
            composableMapLegendDialogHost.Content()
            composableSelectionDialogHost.Content()
            composableConfirmationDialogHost.Content()
            composableNotificationDialogHost.Content()
            composableBusyIndicatorDialogHost.Content()
            composableEnumerationHelpDialogHost.Content()
            composableNearbySessionStatusDialogHost.Content()
        }

        mapboxMapClickListener = MapManager.instance().createMapboxMapClickListener( binding.mapboxMapView, true )

        if ((requireActivity().application as MainApplication).user == null)
        {
            findNavController().navigate(R.id.action_navigate_to_MainFragment, null,
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.action_navigate_to_MainFragment, false)
                    .build())
            return
        }

        lateinit var config: Config

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        enumArea.enumerationTeams.find { it.uuid == sharedViewModel.currentEnumerationTeamUuid }?.let { enumerationTeam ->
            this.enumerationTeam = enumerationTeam
        }

        if (config.mapEngineIndex == MapEngine.OpenStreetMap.value)
        {
            binding.osmMapView.visibility = View.VISIBLE
            binding.mapboxMapView.visibility = View.GONE
            MapManager.instance().centerMap( enumerationTeam.polygon, binding.osmMapView )
        }
        else
        {
            binding.osmMapView.visibility = View.GONE
            binding.mapboxMapView.visibility = View.VISIBLE
            MapManager.instance().centerMap( enumerationTeam.polygon, binding.mapboxMapView )
        }

        binding.progressOverlayView.visibility = View.VISIBLE

        binding.progressOverlayView.visibility = View.GONE

        enumerationTeamLocations.clear()

        for (teamLocationUuid in enumerationTeam.locationUuids)
        {
            enumArea.locations.find { location -> location.uuid == teamLocationUuid  }?.let { location ->
                enumerationTeamLocations.add( location )
            }
        }

        for (location in enumArea.locations)
        {
            if (location.isLandmark)
            {
                enumerationTeamLocations.add( location )
            }
        }

        for (location in enumerationTeamLocations) {
            location.isVisible = true
        }

        (activity!!.application as? MainApplication)?.user?.let {
            user = it
        }

        performEnumerationAdapter = PerformEnumerationAdapter( enumerationTeamLocations, enumArea.name )
        performEnumerationAdapter.didSelectLocation = this::didSelectLocation

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performEnumerationAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )
        binding.recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );

        binding.titleTextView.text =  enumArea.name + " (" + enumerationTeam.name + " " +  resources.getString(R.string.team) + ")"

        val centerOnCurrentLocation = sharedViewModel.centerOnCurrentLocation?.value

        if (centerOnCurrentLocation == null)
        {
            sharedViewModel.setCenterOnCurrentLocation( false )
        }

        binding.addHouseholdButton.backgroundTintList?.let {
            defaultColorList = it
        }

        if (enumArea.mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( enumArea.mbTilesPath )
        }

        MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
            this.mapView = mapView

            MapManager.instance().enableLocationUpdates( activity!!, mapView )

            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

            lastCenterPoint?.let {
                MapManager.instance().centerMap( it, mapView )
            } ?: run {
                MapManager.instance().centerMap( enumerationTeam.polygon, mapView )
            }

            sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
                if (centerOnCurrentLocation)
                {
                    MapManager.instance().startCenteringOnLocation( activity!!, mapView )
                    binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                }
                else
                {
                    MapManager.instance().stopCenteringOnLocation( mapView )
                    binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    val mapManager = MapManager.instance()

                    launch {
                        mapManager.markerTapped.collect { location ->
                            if (!isHandlingTapEvent)
                            {
                                isHandlingTapEvent = true

                                sharedViewModel.currentLocationUuid = location.uuid

                                if (location.isLandmark)
                                {
                                    findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
                                }
                                else
                                {
                                    navigateToAddHouseholdFragment()
                                }
                            }
                        }
                    }
                }
            }

            refreshMap()
        }

        binding.mapOverlayView.setOnTouchListener(this)

        if (ActivityCompat.checkSelfPermission( activity!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission( activity!!, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            if (!LocationService.started)
            {
                LocationService.locationCallback = locationCallback
                val intent = Intent(activity!!, LocationService::class.java)
                ContextCompat.startForegroundService(activity!!, intent )
            }
        }

        val views = ArrayList<String>()
        val showViews = resources.getTextArray( R.array.show_views )

        for (showView in showViews)
        {
            views.add( showView.toString())
        }

        binding.showSpinner.adapter = ArrayAdapter<String>(this.requireContext(), android.R.layout.simple_spinner_dropdown_item, views )

        val sharedPreferences: SharedPreferences = MainApplication.getContext().getSharedPreferences("default", 0)
        val position = sharedPreferences.getInt( Keys.kViewPreference.value, 0 )
        binding.showSpinner.setSelection( position )

        binding.showSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
            {
                // Note! OnItemSelected fires automatically when the fragment is created

                val sharedPreferences: SharedPreferences = MainApplication.getContext().getSharedPreferences("default", 0)
                sharedPreferences.edit(commit = true) {
                    putInt(Keys.kViewPreference.value, position )
                }

                when( position )
                {
                    0-> { // nothing
                        binding.mapLayout.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                    1-> { // Map Only
                        binding.mapLayout.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    }
                    2-> { // List Only
                        binding.mapLayout.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val filters = ArrayList<String>()
        val sortFilters = resources.getTextArray( R.array.sort_filters )

        for (sortFilter in sortFilters)
        {
            filters.add( sortFilter.toString())
        }

        binding.filterSpinner.adapter = ArrayAdapter<String>(this.requireContext(), android.R.layout.simple_spinner_dropdown_item, filters )

        // Note! OnItemSelected fires automatically when the fragment is created
        // using post will ensure that this will not happen
        binding.filterSpinner.post {
            binding.filterSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long)
                {
                    when( position )
                    {
                        0-> { // nothing
                            for (location in enumerationTeamLocations) {
                                location.isVisible = true
                            }
                        }
                        1-> { // undefined
                            for (location in enumerationTeamLocations)
                            {
                                location.isVisible = false
                                if (!location.isLandmark)
                                {
                                    if (location.enumerationItems.isEmpty())
                                    {
                                        location.isVisible = true
                                    }
                                    else
                                    {
                                        for (enumerationItem in location.enumerationItems)
                                        {
                                            if (enumerationItem.enumerationState == EnumerationState.Undefined)
                                            {
                                                location.isVisible = true
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2-> { // incomplete
                            for (location in enumerationTeamLocations)
                            {
                                location.isVisible = false
                                if (!location.isLandmark)
                                {
                                    for (enumerationItem in location.enumerationItems)
                                    {
                                        if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                                        {
                                            location.isVisible = true
                                            break
                                        }
                                    }
                                }
                            }
                        }
                        3-> { // complete
                            for (location in enumerationTeamLocations)
                            {
                                location.isVisible = false
                                if (!location.isLandmark)
                                {
                                    for (enumerationItem in location.enumerationItems)
                                    {
                                        if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                                        {
                                            location.isVisible = true
                                            break
                                        }
                                    }
                                }
                            }
                        }
                        4-> { // points of interest
                            for (location in enumerationTeamLocations)
                            {
                                location.isVisible = if (location.isLandmark) true else false
                            }
                        }
                    }

                    performEnumerationAdapter.updateLocations( enumerationTeamLocations )
                    refreshMap()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            })
        }

        binding.legendTextView.setOnClickListener {
            composableMapLegendDialogHost.show()
        }

        binding.legendImageView.setOnClickListener {
            composableMapLegendDialogHost.show()
        }

        binding.helpButton.setOnClickListener {
            composableEnumerationHelpDialogHost.show()
        }

        binding.deleteBreadcrumbsButton.setOnClickListener {
            if (enumArea.breadcrumbs.isNotEmpty())
            {
                var breadcrumb = enumArea.breadcrumbs.last()

                if (selectedBreadcrumbs.isNotEmpty())
                {
                    breadcrumb = selectedBreadcrumbs.last()
                }

                selectedBreadcrumbs.remove( breadcrumb )
                enumArea.breadcrumbs.remove( breadcrumb )
                DAO.breadcrumbDAO.delete( breadcrumb )

                refreshMap()
            }
        }

        binding.mapTileCacheButton.setOnClickListener {
            enumArea.mapTileRegion?.let {
                val mapTileRegions = ArrayList<MapTileRegion>()
                mapTileRegions.add( it )
                composableBusyIndicatorDialogHost.show(title = resources.getString(R.string.downloading_map_tiles), message = null) {
                    composableBusyIndicatorDialogHost.cancel()
                    MapManager.instance().cancelTilePackDownload()
                }
                MapManager.instance().cacheMapTiles(activity!!, mapView, mapTileRegions, this )
            }
        }

        binding.centerOnLocationButton.setOnClickListener {
            sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
                if (centerOnCurrentLocation)
                {
                    MapManager.instance().stopCenteringOnLocation( mapView )
                    sharedViewModel.setCenterOnCurrentLocation( false )
                    binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                }
                else
                {
                    MapManager.instance().startCenteringOnLocation( activity!!, mapView )
                    sharedViewModel.setCenterOnCurrentLocation( true )
                    binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                }
            }
        }

        binding.addHouseholdButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }

            if (gpsAccuracyIsGood())
            {
                if (config.allowManualLocationEntry)
                {
                    composableSelectionDialogHost.show(
                        title = resources.getString(R.string.select_location),
                        message = null,
                        items = listOf(resources.getString(R.string.current_location),resources.getString(R.string.new_location)),
                    ) { selection ->
                        if (selection == resources.getString(R.string.current_location)) {
                            addHouseholdButtonPress()
                        }
                        else if (selection == resources.getString(R.string.new_location)) {
                            dropMode = true
                            binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                        }
                    }
                }
                else
                {
                    addHouseholdButtonPress()
                }
            }
            else
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.gps_accuracy_error), Toast.LENGTH_LONG).show()
            }
        }

        binding.addLandmarkButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }

            if (gpsAccuracyIsGood())
            {
                currentGPSAccuracy?.let { accuracy ->
                    currentGPSLocation?.let { point ->
                        val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
                        val location = Location( timeZone, accuracy, point.latitude(), point.longitude(), point.altitude(), true, "", "")
                        DAO.locationDAO.createOrUpdateLocation( location, enumArea, location.version )
                        enumArea.locations.add(location)
                        sharedViewModel.currentLocationUuid = location.uuid
                        findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
                    } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.current_location_not_set), Toast.LENGTH_LONG).show()
                }
            }
            else
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.gps_accuracy_error), Toast.LENGTH_LONG).show()
            }
        }

        binding.exportButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }

            val title = if (user.role == Role.Enumerator.value) resources.getString(R.string.export_enum_data) else resources.getString(R.string.export_configuration)

            composableSelectionDialogHost.show(
                title = title,
                message = resources.getString(R.string.select_export_message),
                items = listOf(resources.getString(R.string.qr_code),resources.getString(R.string.file_system)),
            ) { selection ->
                if (selection == resources.getString(R.string.qr_code)) {
                    composableNearbySessionStatusDialogHost.show(title = resources.getString(R.string.export_configuration))
                    {
                        nearbySessionHostManager?.stopHosting()
                    }

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
                else if (selection == resources.getString(R.string.file_system)) {
                    val items = ArrayList<String>()
                    items.add( "Configuration Files" )
                    items.add( "Image Files" )

                    composableCheckboxDialogHost.show(
                        title = resources.getString(R.string.select_the_file_types_to_export),
                        items = items,
                        isChecked = emptyList()
                    ) { selections ->
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
                                message = null,
                                items = listOf(resources.getString(R.string.default_location),resources.getString(R.string.let_me_choose)),
                            ) { selection ->
                                if (selection == resources.getString(R.string.default_location)) {
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

                                    zipUtils.zipToPublicDocuments( requireActivity(), config, getFileName(), "Enumerated", includeConfig, includeImages ) { success ->
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
                                else if (selection == resources.getString(R.string.let_me_choose)) {
                                    exportToDevice()
                                }
                            }
                        }
                    }
                }
            }
        }

        for (location in enumArea.locations)
        {
            for (enumItem in location.enumerationItems)
            {
                if (enumItem.enumerationState == EnumerationState.Enumerated || enumItem.enumerationState == EnumerationState.Incomplete)
                {
                    enumItem.subAddress.toIntOrNull()?.let {
                        if (it > maxSubaddress)
                        {
                            maxSubaddress = it
                        }
                    }
                }
            }
        }

        val enumerationCount = updateSummaryInfo()

        if (enumerationCount == 0)
        {
            composableInputDialogHost.show(
                title = null,
                description = resources.getString(R.string.subaddress_start),
                text = "1",
                inputTypeNumber = true,
                cancelable = true,
                onResult = { text ->
                    if (text.isNotEmpty())
                    {
                        text.toIntOrNull()?.let {
                            maxSubaddress = it - 1
                        }
                    }
                }
            )
        }

        binding.listItemEnumArea.titleLayout.visibility = View.GONE

        // show the current breadcrumb state

        when (breadcrumbRecordingState)
        {
            BreadcrumbRecordingState.OFF -> {
                binding.recordBreadcrumbsButton.setBackgroundResource(R.drawable.record)
                binding.recordBreadcrumbsButton.setBackgroundTintList(defaultColorList);
            }
            BreadcrumbRecordingState.RECORDING -> {
                binding.recordBreadcrumbsButton.setBackgroundResource(R.drawable.record)
                binding.recordBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            BreadcrumbRecordingState.PAUSE -> {
                binding.recordBreadcrumbsButton.setBackgroundResource(R.drawable.pause)
                binding.recordBreadcrumbsButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.recordBreadcrumbsButton.setOnClickListener {
            if (breadcrumbRecordingState == BreadcrumbRecordingState.OFF)
            {
                breadcrumbRecordingState = BreadcrumbRecordingState.RECORDING
                isShowingBreadcrumbs = true

                lastBreadcrumbGroupId = UUID.randomUUID().toString()
                binding.recordBreadcrumbsButton.setBackgroundResource( R.drawable.record )
                binding.recordBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                binding.showBreadcrumbsButton.setBackgroundResource( R.drawable.navigate2 )
                binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)))
            }
            else if (breadcrumbRecordingState == BreadcrumbRecordingState.RECORDING)
            {
                breadcrumbRecordingState = BreadcrumbRecordingState.PAUSE

                binding.recordBreadcrumbsButton.setBackgroundResource( R.drawable.pause )
                binding.recordBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else if (breadcrumbRecordingState == BreadcrumbRecordingState.PAUSE)
            {
                breadcrumbRecordingState = BreadcrumbRecordingState.OFF
                isShowingBreadcrumbs = false

                binding.recordBreadcrumbsButton.setBackgroundResource( R.drawable.record )
                binding.recordBreadcrumbsButton.setBackgroundTintList(defaultColorList);
                binding.showBreadcrumbsButton.setBackgroundResource( R.drawable.navigate2 )
                binding.showBreadcrumbsButton.setBackgroundTintList(defaultColorList);
            }

            refreshMap()
        }

        if (isShowingBreadcrumbs)
        {
            binding.showBreadcrumbsButton.setBackgroundResource( R.drawable.navigate2 )
            binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)))
        }
        else
        {
            binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
            binding.showBreadcrumbsButton.setBackgroundTintList(defaultColorList);
        }

        binding.showBreadcrumbsButton.setOnClickListener {

            if (isShowingBreadcrumbs)
            {
                isShowingBreadcrumbs = false
                binding.showBreadcrumbsButton.setBackgroundResource( R.drawable.navigate2 )
                binding.showBreadcrumbsButton.setBackgroundTintList(defaultColorList)
                refreshMap()
            }
            else
            {
                if (enumArea.enumerationTeams.size == 1)
                {
                    isShowingBreadcrumbs = true
                    selectedTeamNames.clear()
                    selectedBreadcrumbs.clear()
                    selectedTeamNames.add( enumerationTeam.name )
                    selectedBreadcrumbs.addAll( enumArea.breadcrumbs)
                    binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
                    binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)))
                    refreshMap()
                }
                else
                {
                    val choices = ArrayList<String>()
                    val isChecked = ArrayList<Boolean>()

                    for (team in enumArea.enumerationTeams)
                    {
                        choices.add( team.name )
                        isChecked.add( if (team.name == enumerationTeam.name) true else false )
                    }

                    composableCheckboxDialogHost.show(
                        title = resources.getString(R.string.select_enumeration_teams),
                        items = choices,
                        isChecked = emptyList()
                    ) { selections ->
                        selectedTeamNames.clear()
                        selectedBreadcrumbs.clear()

                        for (selectedTeamName in selections)
                        {
                            for (team in enumArea.enumerationTeams)
                            {
                                if (team.name == selectedTeamName)
                                {
                                    selectedTeamNames.add( team.name )

                                    for (breadcrumb in enumArea.breadcrumbs)
                                    {
                                        if (breadcrumb.enumTeamName == team.name)
                                        {
                                            selectedBreadcrumbs.add( breadcrumb )
                                        }
                                    }
                                }
                            }
                        }

                        isShowingBreadcrumbs = true
                        binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
                        binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)))
                        refreshMap()
                    }
                }
            }
        }
    }

    fun updateSummaryInfo() : Int
    {
        var sampledCount = 0
        var surveyedCount = 0
        var enumerationCount = 0

        for (location in enumerationTeamLocations)
        {
            for (enumItem in location.enumerationItems)
            {
                if (enumItem.enumerationState == EnumerationState.Enumerated || enumItem.enumerationState == EnumerationState.Incomplete)
                {
                    enumerationCount += 1
                }
                if (enumItem.samplingState == SamplingState.Sampled || enumItem.subsetSamplingState == SamplingState.Sampled)
                {
                    sampledCount += 1
                }
                if (enumItem.collectionState == CollectionState.Complete)
                {
                    surveyedCount += 1
                }
            }
        }

        binding.listItemEnumArea.numberEnumeratedTextView.text = "$enumerationCount"
        binding.listItemEnumArea.numberSampledTextView.text = "$sampledCount"
        binding.listItemEnumArea.numberSurveyedTextView.text = "$surveyedCount"

        return enumerationCount
    }

    override fun onResume()
    {
        super.onResume()

        isHandlingTapEvent = false

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun addHouseholdButtonPress()
    {
        currentGPSLocation?.let { point ->
            sharedViewModel.currentConfiguration?.value?.let { config ->
                if (geofenceCheckFailed( config, point )) { return }

                if (config.proximityWarningIsEnabled)
                {
                    enumArea.locations.map{
                        if (!it.isLandmark)
                        {
                            val haversineCheck = GeoUtils.isCloseTo( LatLng( it.latitude, it.longitude), LatLng(point.latitude(),point.longitude()), config.proximityWarningValue)
                            if (haversineCheck.withinBounds)
                            {
                                val distance = String.format( "%.1f", haversineCheck.distance)
                                val message = "${resources.getString(R.string.duplicate_warning)} (${distance}m)"
                                pointIsTooClose( distance, message, point )
                                return
                            }
                        }
                    }
                }
            }

            var accuracy = -1

            currentGPSAccuracy?.let {
                accuracy = it
            }

            val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
            val location = Location( timeZone, accuracy, point.latitude(), point.longitude(), point.altitude(), false, "", "")

            DAO.locationDAO.createOrUpdateLocation( location, enumArea, location.version )
            enumArea.locations.add(location)

            sharedViewModel.currentLocationUuid = location.uuid

            enumerationTeamLocations.add(location)
            enumerationTeam.locationUuids.add(location.uuid)
            DAO.enumerationTeamDAO.updateConnectorTable( enumerationTeam )
            navigateToAddHouseholdFragment()
        } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.current_location_not_set), Toast.LENGTH_LONG).show()
    }

    private fun pointIsTooClose( distance: String, message: String, point: Point )
    {
        composableConfirmationDialogHost.show(
            title = resources.getString(R.string.warning),
            message = message,
            leftButtonText = resources.getString(R.string.no),
            rightButtonText = resources.getString(R.string.yes),
        ) { selection ->
            if (selection == resources.getString(R.string.yes)) {
                var accuracy = -1

                currentGPSAccuracy?.let {
                    accuracy = it
                }

                val pt = tag as Point
                val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
                val location = Location( timeZone, accuracy, pt.latitude(), pt.longitude(), pt.altitude(), false, "", "")

                DAO.locationDAO.createOrUpdateLocation( location, enumArea, location.version )
                enumArea.locations.add(location)

                sharedViewModel.currentLocationUuid = location.uuid

                enumerationTeamLocations.add(location)
                enumerationTeam.locationUuids.add(location.uuid)
                DAO.enumerationTeamDAO.updateConnectorTable( enumerationTeam )
                navigateToAddHouseholdFragment()
            }
        }
    }

    private fun gpsAccuracyIsGood(): Boolean
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            currentGPSAccuracy?.let {
                return (it <= config.minGpsPrecision)
            }
        }

        return false
    }

    private fun gpsLocationIsGood( location: Location ) : Boolean
    {
        var editMode = false

        if (gpsAccuracyIsGood())
        {
            sharedViewModel.currentConfiguration?.value?.let { config ->
                currentGPSLocation?.let { point ->
                    val distance = GeoUtils.distanceBetween( LatLng( location.latitude, location.longitude ), LatLng( point.latitude(), point.longitude()))
                    editMode = distance <= config.minGpsPrecision
                }
            }
        }

        return editMode
    }

    private fun refreshMap()
    {
        if (!this::mapView.isInitialized)
        {
            return
        }

        MapManager.instance().clearMap( mapView )

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumerationTeam.polygon.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty() && pointList[0].isNotEmpty())
        {
            MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x20, Color.RED, enumArea.name )

            if (breadcrumbRecordingState == BreadcrumbRecordingState.RECORDING)
            {
                selectedTeamNames.clear()
                selectedBreadcrumbs.addAll( enumArea.breadcrumbs )
                selectedTeamNames.add( enumerationTeam.name )
            }

            if (isShowingBreadcrumbs && selectedBreadcrumbs.isNotEmpty())
            {
                MapManager.instance().loadBreadcrumbs(requireContext(), mapView, selectedBreadcrumbs, selectedTeamNames )

                var groupId = ""

                val breadcrumbs = ArrayList<Breadcrumb>()

                for (teamName in selectedTeamNames)
                {
                    for (breadcrumb in selectedBreadcrumbs)
                    {
                        if (breadcrumb.enumTeamName == teamName)
                        {
                            if (breadcrumbs.isEmpty())
                            {
                                groupId = breadcrumb.groupId
                            }

                            if (breadcrumb.groupId == groupId)
                            {
                                breadcrumbs.add( breadcrumb )
                            }
                            else
                            {
                                MapManager.instance().createPolyline( mapView, breadcrumbs )
                                groupId = breadcrumb.groupId
                                breadcrumbs.clear()
                                breadcrumbs.add( breadcrumb )
                            }
                        }
                    }

                    if (breadcrumbs.isNotEmpty())
                    {
                        MapManager.instance().createPolyline( mapView, breadcrumbs )
                    }
                }
            }

            for (location in enumArea.locations)
            {
                if (location.isLandmark && location.isVisible)
                {
                    MapManager.instance().createMarker( activity!!, mapView, location, R.drawable.location_blue, "" )
                }
            }

            val markerProperties = ArrayList<MapManager.MarkerProperty>()

            for (location in enumerationTeamLocations)
            {
                if (!location.isLandmark && location.isVisible)
                {
                    var resourceId = if (location.enumerationItems.size > 1) R.drawable.multi_home_black else R.drawable.home_black

                    var numComplete = 0

                    for (item in location.enumerationItems)
                    {
                        val enumerationItem = item as EnumerationItem?
                        if(enumerationItem != null)
                        {
                            if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                            {
                                resourceId = if (location.enumerationItems.size > 1) R.drawable.multi_home_red else R.drawable.home_red
                                break
                            }
                            else if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                            {
                                numComplete++
                            }
                        }
                    }

                    if (numComplete > 0 && numComplete == location.enumerationItems.size)
                    {
                        resourceId = if (location.enumerationItems.size > 1) R.drawable.multi_home_green else R.drawable.home_green
                    }

                    var title = ""
                    if (location.enumerationItems.isNotEmpty())
                    {
                        title = location.enumerationItems[0].subAddress
                    }

                    markerProperties.add( MapManager.MarkerProperty( location, resourceId, title ))
                }
            }

            if (markerProperties.isNotEmpty())
            {
                MapManager.instance().loadMarkers( activity!!, mapView, markerProperties, mapboxMapClickListener )
            }
        }
    }

    fun navigateToAddHouseholdFragment()
    {
        var didNavigate = false

        enumArea.locations.find { it.uuid == sharedViewModel.currentLocationUuid }?.let { location: Location ->
            val bundle = Bundle()
            bundle.putBoolean( Keys.kEditMode.value, gpsLocationIsGood( location ))
            bundle.putInt( Keys.kStartSubaddress.value, maxSubaddress)

            if (location.enumerationItems.isEmpty())
            {
                if (gpsLocationIsGood( location ))
                {
                    if (breadcrumbRecordingState == BreadcrumbRecordingState.RECORDING && enumArea.breadcrumbs.isNotEmpty())
                    {
                        enumArea.breadcrumbs.add(Breadcrumb(enumArea.uuid, enumerationTeam.name, location.latitude, location.longitude, enumArea.breadcrumbs.last().groupId))
                    }

                    composableConfirmationDialogHost.show(
                        title = resources.getString(R.string.please_confirm),
                        message = resources.getString(R.string.is_multi_family),
                        leftButtonText = resources.getString(R.string.no),
                        rightButtonText = resources.getString(R.string.yes),
                    ) { selection ->
                        val enumerationItem = EnumerationItem()

                        enumerationItem.locationUuid = location.uuid

                        DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem,enumerationItem.version )
                        location.enumerationItems.add(enumerationItem)
                        sharedViewModel.currentEnumerationItemUuid = enumerationItem.uuid

                        sharedViewModel.currentConfiguration?.value?.let { config ->
                            if (config.autoIncrementSubaddress) {
                                enumerationItem.subAddress = "${maxSubaddress + 1}"
                            }
                        }

                        if (selection == resources.getString(R.string.no))
                        {
                            didNavigate = true
                            val bundle = Bundle()
                            bundle.putBoolean( Keys.kEditMode.value, gpsLocationIsGood( location ))
                            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)
                        }
                        else if (selection == resources.getString(R.string.yes))
                        {
                            didNavigate = true
                            val bundle = Bundle()
                            bundle.putBoolean( Keys.kEditMode.value, gpsLocationIsGood( location ))
                            bundle.putInt( Keys.kStartSubaddress.value, maxSubaddress)
                            findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment,bundle)
                        }
                    }
                }
                else
                {
                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.gps_location_error), Toast.LENGTH_LONG).show()
                }
            }
            else if (location.enumerationItems.size == 1)
            {
                didNavigate = true
                sharedViewModel.currentEnumerationItemUuid = location.enumerationItems[0].uuid
                findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment, bundle)
            }
            else
            {
                didNavigate = true
                findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment, bundle)
            }
        }

        if (!didNavigate)
        {
            isHandlingTapEvent = false
        }
    }

    private fun didSelectLocation( location: Location )
    {
        if (dropMode)
        {
            dropMode = false
            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
        }

        sharedViewModel.currentLocationUuid = location.uuid

        if (location.isLandmark)
        {
            findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
        }
        else
        {
            navigateToAddHouseholdFragment()
        }
    }

    fun getFileName() : String
    {
        (activity!!.application as MainApplication).user?.let { user ->
            var userName = user.name.replace(" ", "" ).uppercase()

            if (userName.length > 3)
            {
                userName = userName.substring(0,3)
            }

            val role = user.role.toString().substring(0,1).uppercase()

            val formatter = DateTimeFormatter.ofPattern("yyMMdd-HHmm")
            val dateTime = LocalDateTime.now().format(formatter)

            val versionName = BuildConfig.VERSION_NAME
            var clusterName = enumArea.name.replace(" ", "" ).uppercase()
            clusterName = clusterName.replace(Regex("-\\[.*?]"), "")

            when(user.role)
            {
                Role.Admin.value,
                Role.Supervisor.value ->
                {
                    return "${role}-${userName}-${clusterName}-EN-${dateTime!!}-${versionName}"
                }

                Role.Enumerator.value,
                Role.DataCollector.value ->
                {
                    return "${role}-${userName}-${clusterName}-${dateTime!!}-${versionName}"
                }
                Role.Undefined.value -> {}
            }
        }

        return ""
    }

    fun exportToDevice()
    {
        val zipFileName = getFileName() + ".zip"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, zipFileName)
        }

        startActivityForResult( intent, REQUEST_CODE_PICK_CONFIG_DIR )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        try
        {
            sharedViewModel.currentConfiguration?.value?.let { config ->
                if (requestCode == REQUEST_CODE_PICK_CONFIG_DIR && resultCode == Activity.RESULT_OK)
                {
                    data?.data?.let { uri ->
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
        }
        catch (ex: java.lang.Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
            composableNotificationDialogHost.show(title = resources.getString(R.string.oops), message = resources.getString(R.string.export_failed))
        }
    }

    override fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    {
        activity!!.runOnUiThread {
            composableBusyIndicatorDialogHost.updateMessage("${numLoaded}/${numNeeded}")
        }
    }

    override fun tilePacksLoaded( error: String )
    {
        activity!!.runOnUiThread {
            composableBusyIndicatorDialogHost.cancel()
            if (error.isNotEmpty())
            {
                Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.tile_pack_download_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    var subAddress = 0

    fun autoGenerateLocations()
    {
        composableInputDialogHost.show(
            title = null,
            description = "Enter the number of HH's to create",
            text = "",
            inputTypeNumber = true,
            onResult = { text ->
                if (text.isNotEmpty())
                {
                    text.toIntOrNull()?.let { numLocations ->

                        isAcceptingLocationUpdates = false
                        composableBusyIndicatorDialogHost.show(title = "Generating Locations...", message = null)

                        viewLifecycleOwner.lifecycleScope.launch {
                            withContext(Dispatchers.IO)
                            {
                                DAO.instance().writableDatabase.beginTransaction()

                                val randomLocationGenerator = GeoUtils.RandomLocationGenerator2(enumerationTeam.polygon, 10.0)
                                randomLocationGenerator.generate(numLocations ) { point, count ->
                                    requireActivity().runOnUiThread {
                                        composableBusyIndicatorDialogHost.updateMessage("Generated Location ${count}/${numLocations}")

                                    }
                                    val location = Location(point.latitude(), point.longitude(), 0.0)
                                    DAO.locationDAO.createOrUpdateLocation(location, enumArea, location.version)
                                    enumArea.locations.add(location)
                                    sharedViewModel.currentLocationUuid = location.uuid
                                    enumerationTeamLocations.add(location)
                                    enumerationTeam.locationUuids.add(location.uuid)
                                    DAO.enumerationTeamDAO.updateConnectorTable(enumerationTeam)
                                }

                                DAO.instance().writableDatabase.setTransactionSuccessful()
                                DAO.instance().writableDatabase.endTransaction()
                            }

                            // back on the main thread...

                            composableBusyIndicatorDialogHost.cancel()

                            refreshMap()
                        }
                    }
                }
            }
        )
    }

    fun autoEnumerateLocations()
    {
        val config = sharedViewModel.currentConfiguration!!.value!!

        DAO.instance().writableDatabase.beginTransaction()

        for (location in enumerationTeamLocations)
        {
            if (!location.isLandmark && location.enumerationItems.isEmpty())
            {
                subAddress+= 1

                val enumerationItem = EnumerationItem()

                enumerationItem.uuid = UUID.randomUUID().toString()
                enumerationItem.version = UUID.randomUUID().toString()
                enumerationItem.enumerationIncompleteReason = ""
                enumerationItem.enumerationState = EnumerationState.Enumerated
                enumerationItem.enumerationNotes = ""
                enumerationItem.enumerationDate = Date().time
                enumerationItem.subAddress = subAddress.toString()
                enumerationItem.locationUuid = location.uuid

                var creationDate = Date().time

                for (field in config.studies[0].fields)
                {
                    val fieldData = FieldData(creationDate++, field.uuid, enumerationItem.uuid )

                    if (field.type == FieldType.Note)
                    {
                        fieldData.textValue = "Some Note"
                    }

                    if (field.type == FieldType.Text)
                    {
                        fieldData.textValue = "Some Text"
                    }

                    if (field.type == FieldType.Number)
                    {
                        fieldData.numberValue= 999.0
                    }

                    if (field.type == FieldType.Date)
                    {
                        fieldData.dateValue = Date().time
                    }

                    if (field.type == FieldType.Checkbox)
                    {
                        fieldData.fieldDataOptions.add( FieldDataOption( "CB 1", true ))
                        fieldData.fieldDataOptions.add( FieldDataOption( "CB 2", false ))
                        fieldData.fieldDataOptions.add( FieldDataOption( "CB 3", true ))
                    }

                    if (field.type == FieldType.Dropdown)
                    {
                        fieldData.dropdownIndex = 1
                        fieldData.fieldDataOptions.add( FieldDataOption( "DD 1", false ))
                        fieldData.fieldDataOptions.add( FieldDataOption( "DD 2", false ))
                        fieldData.fieldDataOptions.add( FieldDataOption( "DD 3", false ))
                    }

                    enumerationItem.fieldDataList.add( fieldData )
                }

                location.enumerationItems.add( enumerationItem )
                DAO.locationDAO.createOrUpdateLocation( location, enumArea, location.version )
            }
        }

        DAO.instance().writableDatabase.setTransactionSuccessful()
        DAO.instance().writableDatabase.endTransaction()
    }

    fun autoGenerateImages()
    {
        DAO.instance().writableDatabase.beginTransaction()
        ImageDAO.instance().writableDatabase.beginTransaction()

        for (location in enumerationTeamLocations)
        {
            if (!location.isLandmark && location.imageUuid.isEmpty())
            {
                subAddress += 1
                val testImage = TestImage.imageData.replace("\n", "" )
                val image = Image(location.uuid,testImage )
                ImageDAO.instance().createImage( image )?.let { image ->
                    location.imageUuid = image.uuid
                    DAO.locationDAO.createOrUpdateLocation( location, enumArea, location.version )
                }
            }
        }

        DAO.instance().writableDatabase.setTransactionSuccessful()
        ImageDAO.instance().writableDatabase.setTransactionSuccessful()

        DAO.instance().writableDatabase.endTransaction()
        ImageDAO.instance().writableDatabase.endTransaction()
    }

    var lastLocation : Location? = null

    fun autoGenerateBreadcrumbs()
    {
        var creationDate = Date().time

        DAO.instance().writableDatabase.beginTransaction()

        var count = 0

        for (location in enumerationTeamLocations)
        {
            if (lastLocation == null)
            {
                lastLocation = location
            }
            else
            {
                val breadcrumbs = generateRandomBreadcrumbs(lastLocation!!, location )

                count += breadcrumbs.size

                for (breadcrumb in breadcrumbs)
                {
                    breadcrumb.creationDate = creationDate++
                    DAO.breadcrumbDAO.createOrUpdateBreadcrumb( breadcrumb, breadcrumb.version )
                }

                enumArea.breadcrumbs.addAll( breadcrumbs )
                lastLocation = location

                if (enumArea.breadcrumbs.count() > 25000)
                {
                    break
                }
            }
        }

        Log.d( "xxx", "Generated ${count} breadcrumbs" )


        DAO.instance().writableDatabase.setTransactionSuccessful()
        DAO.instance().writableDatabase.endTransaction()
    }

    fun generateRandomBreadcrumbs( location1: Location, location2: Location ): ArrayList<Breadcrumb>
    {
        val spacingMeters: Double = 100.0
        val breadcrumbs = ArrayList<Breadcrumb>()

        val start = GeoPoint(location1.latitude, location1.longitude )
        val end = GeoPoint(location2.latitude, location2.longitude )
        val distance = start.distanceToAsDouble(end )

        if (distance <= spacingMeters)
        {
            val breadcrumb1 = Breadcrumb( enumArea.uuid, enumerationTeam.name, start.latitude, start.longitude, "0" )
            val breadcrumb2 = Breadcrumb( enumArea.uuid, enumerationTeam.name, end.latitude, end.longitude, "0" )
            breadcrumbs.add( breadcrumb1 )
            breadcrumbs.add( breadcrumb2 )
            return breadcrumbs
        }

        val bearing = start.bearingTo(end)
        val points = mutableListOf<GeoPoint>()

        var traveled = 0.0

        while (traveled < distance)
        {
            points.add(start.destinationPoint(traveled, bearing))
            traveled += spacingMeters
        }

        points.add(end)

        for (point in points)
        {
            val breadcrumb = Breadcrumb( enumArea.uuid, enumerationTeam.name, point.latitude, point.longitude, "0" )
            breadcrumbs.add( breadcrumb )
        }

        return breadcrumbs
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_set_subaddress, menu)

        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("default", Context.MODE_PRIVATE)

        if (sharedPreferences.getBoolean(Keys.kDeveloperMode.value, false ))
        {
            menu.findItem(R.id.action_auto_generate_locations).isVisible = true
            menu.findItem(R.id.action_auto_enumerate).isVisible = true
            menu.findItem(R.id.action_add_images).isVisible = true
            menu.findItem(R.id.action_add_breadcrumbs).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            when (item.itemId)
            {
                R.id.action_auto_generate_locations ->
                {
                    autoGenerateLocations()
                }

                R.id.action_auto_enumerate ->
                {
                    composableConfirmationDialogHost.show(
                        title = resources.getString(R.string.please_confirm),
                        message = "Auto enumerate these locations?",
                        leftButtonText = resources.getString(R.string.no),
                        rightButtonText = resources.getString(R.string.yes),
                    ) { selection ->
                        if (selection == resources.getString(R.string.yes))
                        {
                            binding.progressOverlayView.visibility = View.VISIBLE

                            viewLifecycleOwner.lifecycleScope.launch {
                                withContext(Dispatchers.IO )
                                {
                                    autoEnumerateLocations()
                                }

                                // back on the main thread...

                                binding.progressOverlayView.visibility = View.GONE

                                refreshMap()
                                updateSummaryInfo()
                                performEnumerationAdapter.updateLocations( performEnumerationAdapter.locations )
                                Toast.makeText(activity!!.applicationContext,  "Auto Enumeration Complete.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                R.id.action_add_images ->
                {
                    composableConfirmationDialogHost.show(
                        title = resources.getString(R.string.please_confirm),
                        message = "Auto enumerate Images?",
                        leftButtonText = resources.getString(R.string.no),
                        rightButtonText = resources.getString(R.string.yes),
                    ) { selection ->
                        if (selection == resources.getString(R.string.yes))
                        {
                            binding.progressOverlayView.visibility = View.VISIBLE

                            viewLifecycleOwner.lifecycleScope.launch {
                                withContext(Dispatchers.IO )
                                {
                                    autoGenerateImages()
                                }

                                // back on the main thread...

                                binding.progressOverlayView.visibility = View.GONE

                                refreshMap()
                                Toast.makeText(activity!!.applicationContext,  "Auto Image Generation Complete.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                R.id.action_add_breadcrumbs ->
                {
                    composableConfirmationDialogHost.show(
                        title = resources.getString(R.string.please_confirm),
                        message = "Auto enumerate Breadcrumbs?",
                        leftButtonText = resources.getString(R.string.no),
                        rightButtonText = resources.getString(R.string.yes),
                    ) { selection ->
                        if (selection == resources.getString(R.string.yes))
                        {
                            binding.progressOverlayView.visibility = View.VISIBLE

                            viewLifecycleOwner.lifecycleScope.launch {
                                withContext(Dispatchers.IO )
                                {
                                    autoGenerateBreadcrumbs()
                                }

                                // back on the main thread...

                                binding.progressOverlayView.visibility = View.GONE

                                refreshMap()
                                Toast.makeText(activity!!.applicationContext,  "Auto Breadcrumb Generation Complete.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                R.id.set_subaddress ->
                {
                    composableInputDialogHost.show(
                        title = null,
                        description = resources.getString(R.string.subaddress_start),
                        text = "",
                        inputTypeNumber = true,
                        onResult = { text ->
                            text.toIntOrNull()?.let {
                                maxSubaddress = it - 1
                            }
                        }
                    )
                }

                R.id.mapbox_streets ->
                {
                    val editor = activity!!.getSharedPreferences("default", 0).edit()
                    editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                    editor.commit()

                    MapManager.instance().clearMap( mapView )

                    MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
                        refreshMap()
                    }
                }

                R.id.satellite_streets ->
                {
                    val editor = activity!!.getSharedPreferences("default", 0).edit()
                    editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                    editor.commit()

                    MapManager.instance().clearMap( mapView )

                    MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
                        refreshMap()
                    }
                }

                else -> {}
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private var lastLocationUpdateTime: Long = 0
    private var MIN_BREADCRUMB_METERS: Double = 10.0
    private var isAcceptingLocationUpdates = true

    private val locationCallback = object : LocationCallback()
    {
        override fun onLocationResult(locationResult: LocationResult)
        {
            if (!isAcceptingLocationUpdates)
            {
                return
            }

            val location = locationResult.locations.last()
            val accuracy = location.accuracy.toInt() // in meters
            val altitude = if (location.altitude.isNaN()) 0.0 else location.altitude
            val point = Point.fromLngLat( location.longitude, location.latitude, altitude )

            currentGPSLocation = point
            currentGPSAccuracy = accuracy

            if (_binding == null)
            {
                return
            }

            sharedViewModel.currentConfiguration?.value?.let { config ->
                if (accuracy <= config.minGpsPrecision)
                {
                    binding.accuracyLabelTextView.text = " " + resources.getString(R.string.good)
                    binding.accuracyLabelTextView.setTextColor( Color.parseColor("#0000ff"))
                }
                else
                {
                    binding.accuracyLabelTextView.text = " " + resources.getString(R.string.poor)
                    binding.accuracyLabelTextView.setTextColor( Color.parseColor("#ff0000") )
                }

                binding.accuracyValueTextView.text = " : ${accuracy.toString()}m"
                binding.locationTextView.text = String.format( "%.7f, %.7f, %.0f", point.latitude(), point.longitude(), location.altitude)

                if (Date().time - lastLocationUpdateTime > 3000)
                {
                    lastLocationUpdateTime = Date().time

                    if (breadcrumbRecordingState == BreadcrumbRecordingState.RECORDING)
                    {
                        var distance = 10.1;
                        val currentLatLng = LatLng( point.latitude(), point.longitude())

                        if (enumArea.breadcrumbs.isNotEmpty())
                        {
                            val lastBreadcrumb = enumArea.breadcrumbs.last()
                            val lastLatLng = LatLng( lastBreadcrumb.latitude, lastBreadcrumb.longitude )
                            distance = GeoUtils.distanceBetween( currentLatLng, lastLatLng )
                        }

                        Log.d( "xxx", "distance = ${distance}")

                        for (breadcrumb in enumArea.breadcrumbs)
                        {
                            DAO.breadcrumbDAO.delete( breadcrumb )
                        }

                        if (distance > MIN_BREADCRUMB_METERS)
                        {
                            MapManager.instance().createMarker( activity!!, mapView, point, R.drawable.breadcrumb, "")

                            val breadcrumb = Breadcrumb( enumArea.uuid, enumerationTeam.name, point.latitude(), point.longitude(), lastBreadcrumbGroupId )
                            DAO.breadcrumbDAO.createOrUpdateBreadcrumb( breadcrumb, breadcrumb.version )
                            enumArea.breadcrumbs.add( breadcrumb )

                            Log.d( "xxx", "Created breadcrumb" )
                            refreshMap()
                        }
                    }

                    for (loc in enumArea.locations)
                    {
                        val currentLatLng = LatLng( point.latitude(), point.longitude())
                        val itemLatLng = LatLng( loc.latitude, loc.longitude )
                        val distance = GeoUtils.distanceBetween( currentLatLng, itemLatLng )
                        var (distanceValue, distanceUnits) = formatDistance( config, distance )
                        loc.distance = distanceValue
                        loc.distanceUnits = distanceUnits
                    }

                    performEnumerationAdapter.updateLocations( enumerationTeamLocations )
                }
            }
        }
    }

    fun formatDistance( config: Config, distance: Double ) : Pair<Double,String>
    {
        var distanceUnits: String = ""
        var distanceValue: Double = 0.0

        if (config.distanceFormat == DistanceFormat.Meters)
        {
            if (distance < 500) // 1/2 kilometer
            {
                distanceValue = distance
                distanceUnits = resources.getString(R.string.meters)
            }
            else
            {
                distanceValue = distance / 1000.0
                distanceUnits = resources.getString( R.string.kilometers )
            }
        }
        else if (config.distanceFormat == DistanceFormat.Feet)
        {
            if (distance < 2640) // 1/2 mile
            {
                distanceValue = distance * 3.28084
                distanceUnits = resources.getString( R.string.feet )
            }
            else
            {
                distanceValue = distance / 1609.34
                distanceUnits = resources.getString( R.string.miles )
            }
        }

        return Pair( distanceValue, distanceUnits )
    }

    fun geofenceCheckFailed( config: Config, point: Point ) : Boolean
    {
        if (config.geofenceIsEnabled)
        {
            var distance = GeoUtils.distance( point, enumArea )

            if (config.distanceFormat == DistanceFormat.Feet)
            {
                distance *= 0.3048
            }

            if (distance > config.geofenceBufferValue)
            {
                Toast.makeText( requireContext(), resources.getString(R.string.geofence_error), Toast.LENGTH_LONG).show()
                return true
            }
        }

        return false
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean
    {
        motionEvent?.let {
            if (it.action == MotionEvent.ACTION_DOWN)
            {
                if (dropMode)
                {
                    view?.performClick()

                    dropMode = false

                    var point = MapManager.instance().getLocationFromPixelPoint(mapView, motionEvent )

                    binding.addHouseholdButton.setBackgroundTintList(defaultColorList);

                    currentGPSLocation?.let { current ->
                        point = Point.fromLngLat(point.longitude(), point.latitude(),current.altitude())
                    }

                    sharedViewModel.currentConfiguration?.value?.let { config ->
                        if (geofenceCheckFailed( config, point )) { return true }

                        if (config.proximityWarningIsEnabled)
                        {
                            enumArea.locations.map{
                                if (!it.isLandmark)
                                {
                                    val haversineCheck = GeoUtils.isCloseTo( LatLng( it.latitude, it.longitude), LatLng(point.latitude(), point.longitude()), config.proximityWarningValue )
                                    if (haversineCheck.withinBounds)
                                    {
                                        val distance = String.format( "%.1f", haversineCheck.distance)
                                        val message = "${resources.getString(R.string.duplicate_warning)} (${distance}m)"
                                        pointIsTooClose( distance, message, point )
                                        return true
                                    }
                                }
                            }
                        }

                        var accuracy = -1

                        currentGPSAccuracy?.let {
                            accuracy = it
                        }

                        val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
                        val location = Location( timeZone, accuracy, point.latitude(), point.longitude(),point.altitude(), false, "", "")

                        if (gpsLocationIsGood( location ))
                        {
                            DAO.locationDAO.createOrUpdateLocation( location, enumArea, location.version )
                            enumArea.locations.add(location)

                            sharedViewModel.currentLocationUuid = location.uuid

                            enumerationTeamLocations.add(location)
                            enumerationTeam.locationUuids.add(location.uuid)
                            DAO.enumerationTeamDAO.updateConnectorTable( enumerationTeam )
                            navigateToAddHouseholdFragment()
                        }
                        else
                        {
                            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.gps_location_error), Toast.LENGTH_LONG).show()
                        }
                    }

                    return true
                }
            }
        }

        return false
    }

    override fun onDestroyView()
    {
        if (this::mapView.isInitialized)
        {
            lastCenterPoint = MapManager.instance().getCenter( mapView )
        }

        binding.mapboxMapView.gestures.removeOnMapClickListener(mapboxMapClickListener )
        binding.recyclerView.adapter = null

        composableNearbySessionStatusDialogHost.dismiss()

        nearbySessionHostManager?.stopHosting()

        if (LocationService.started)
        {
            val intent = Intent(activity!!, LocationService::class.java)
            activity!!.stopService( intent )
        }

        _binding = null

        super.onDestroyView()
    }

    override fun onDestroy()
    {
        nearbySessionHostManager?.stopHosting()

        super.onDestroy()
    }
}
