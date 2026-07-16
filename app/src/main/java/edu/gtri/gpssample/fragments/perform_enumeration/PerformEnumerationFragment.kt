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
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.fragments.createstudy.DeleteMode
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.NearbySessionHostManager
import edu.gtri.gpssample.managers.PerformanceManager
import edu.gtri.gpssample.managers.TileServer
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
    InfoDialog.InfoDialogDelegate,
    MapManager.MapTileCacheDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate
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
    private var isRecordingBreadcrumbs = false
    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: Point? = null
    private val enumerationTeamLocations = ArrayList<Location>()
    private var busyIndicatorDialog: BusyIndicatorDialog? = null
    private var includeConfig = false
    private var includeImages = false
    private var maxSubaddress = 0
    private var nearbySessionHostManager: NearbySessionHostManager? = null
    private var nearbySessionStatusDialog: NearbySessionStatusDialog? = null
    private val REQUEST_CODE_PICK_CONFIG_DIR = 1001

    enum class BreadcrumbState(val format : String) {
        Gone("Gone"),
        Crumbs("Crumbs"),
        Trails("Trails"),
    }

    private var breadcrumbState = BreadcrumbState.Gone

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
                ContextCompat.startForegroundService(activity!!, intent)
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
            MapLegendDialog( activity!! )
        }

        binding.legendImageView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.helpButton.setOnClickListener {
            PerformEnumerationHelpDialog( activity!! )
        }

        binding.deleteBreadcrumbsButton.setOnClickListener {
            if (enumArea.breadcrumbs.isNotEmpty())
            {
                val breadcrumb = enumArea.breadcrumbs.last()
                DAO.breadcrumbDAO.delete( breadcrumb )
                enumArea.breadcrumbs.remove( breadcrumb )
                refreshMap()
            }
        }

        binding.mapTileCacheButton.setOnClickListener {
            enumArea.mapTileRegion?.let {
                val mapTileRegions = ArrayList<MapTileRegion>()
                mapTileRegions.add( it )
                busyIndicatorDialog = BusyIndicatorDialog(activity!!, resources.getString(R.string.downloading_map_tiles), this )
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
                    ConfirmationDialog( activity, resources.getString(R.string.select_location), "", resources.getString(R.string.current_location), resources.getString(R.string.new_location), null, true ) { buttonPressed, tag ->
                        when( buttonPressed )
                        {
                            ConfirmationDialog.ButtonPress.Left -> {
                                addHouseholdButtonPress()
                            }
                            ConfirmationDialog.ButtonPress.Right -> {
                                dropMode = true
                                binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                            }
                            ConfirmationDialog.ButtonPress.None -> {
                            }
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

            ConfirmationDialog( activity, title, resources.getString(R.string.select_export_message), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), null, false ) { buttonPressed, tag ->
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
                                            val zipUtils = ZipUtils()

                                            nearbySessionStatusDialog = NearbySessionStatusDialog(requireContext(), resources.getString( R.string.import_configuration )) {
                                                zipUtils.cancel()
                                            }

                                            viewLifecycleOwner.lifecycleScope.launch {
                                                zipUtils.state.collect { state ->
                                                    nearbySessionStatusDialog?.updateState(state)
                                                }
                                            }

                                            PerformanceManager.startTimer()

                                            zipUtils.zipToPublicDocuments( requireActivity(), config, getFileName(), "Enumerated", includeConfig, includeImages ) { success ->
                                                if (success)
                                                {
                                                    NotificationDialog( activity!!, resources.getString(R.string.success), resources.getString(R.string.export_succeeded))
                                                }
                                                else
                                                {
                                                    NotificationDialog( activity!!, resources.getString(R.string.oops), resources.getString(R.string.export_failed))
                                                }

                                                nearbySessionStatusDialog?.dismiss()
                                                nearbySessionStatusDialog = null

                                                Log.d( "xxx", "Export time : ${PerformanceManager.elapsedTime()}")
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
            InputDialog( activity!!, false, resources.getString(R.string.subaddress_start), "1", resources.getString(R.string.cancel), resources.getString(R.string.save), null, false, true, true )  { action, text, tag ->
                when (action) {
                    InputDialog.Action.DidCancel -> {}
                    InputDialog.Action.DidEnterText -> {
                        text.toIntOrNull()?.let {
                            maxSubaddress = it - 1
                        }
                    }
                    InputDialog.Action.DidPressQRButton -> {}
                }
            }
        }

        binding.listItemEnumArea.titleLayout.visibility = View.GONE

        if (isRecordingBreadcrumbs)
        {
            binding.recordBreadcrumbsButton.setBackgroundResource( R.drawable.pause )
            binding.recordBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
        }

        when (breadcrumbState)
        {
            BreadcrumbState.Gone -> {
                binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
                binding.showBreadcrumbsButton.setBackgroundTintList(defaultColorList);
            }
            BreadcrumbState.Crumbs -> {
                binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
                binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            BreadcrumbState.Trails -> {
                binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate3)
                binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
        }

        binding.recordBreadcrumbsButton.setOnClickListener {
            if (!isRecordingBreadcrumbs)
            {
                breadcrumbState = BreadcrumbState.Crumbs
                isRecordingBreadcrumbs = true
                lastBreadcrumbGroupId = UUID.randomUUID().toString()
                binding.recordBreadcrumbsButton.setBackgroundResource( R.drawable.pause )
                binding.recordBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
                binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                isRecordingBreadcrumbs = false
                binding.recordBreadcrumbsButton.setBackgroundResource( R.drawable.record )
                binding.recordBreadcrumbsButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.showBreadcrumbsButton.setOnClickListener {
            if (isRecordingBreadcrumbs)
            {
                return@setOnClickListener
            }

            if (breadcrumbState == BreadcrumbState.Gone)
            {
                breadcrumbState = BreadcrumbState.Crumbs
                binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
                binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else if (breadcrumbState == BreadcrumbState.Crumbs)
            {
                breadcrumbState = BreadcrumbState.Trails
                binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate3)
                binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                breadcrumbState = BreadcrumbState.Gone
                binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
                binding.showBreadcrumbsButton.setBackgroundTintList(defaultColorList)
            }

            refreshMap()
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

    var subAddress = 0
    var lastLocation : Location? = null

    fun autoEnumerateLocations()
    {
        var creationDate = Date().time
        val config = sharedViewModel.currentConfiguration!!.value!!

        DAO.instance().writableDatabase.beginTransaction()

        for (location in enumerationTeamLocations)
        {
            if (lastLocation == null)
            {
                lastLocation = location
            }
            else
            {
                val breadcrumbs = generateBreadcrumbs(lastLocation!!, location )

                for (breadcrumb in breadcrumbs)
                {
                    breadcrumb.creationDate = creationDate++
                    DAO.breadcrumbDAO.createOrUpdateBreadcrumb( breadcrumb, breadcrumb.version )
                }

                enumArea.breadcrumbs.addAll( breadcrumbs )
                lastLocation = location
            }

            if (!location.isLandmark && location.enumerationItems.isEmpty())
            {
                subAddress+= 1

                ImageDAO.instance().createImage( edu.gtri.gpssample.database.models.Image( location.uuid, TestImage.imageData.replace( "\n", "" )))?.let { image ->
                    location.imageUuid = image.uuid
                }

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

                DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, enumerationItem.version )

                location.enumerationItems.add( enumerationItem )
            }
        }

        DAO.instance().writableDatabase.setTransactionSuccessful()
        DAO.instance().writableDatabase.endTransaction()
    }

    fun generateBreadcrumbs( location1: Location, location2: Location ): ArrayList<Breadcrumb>
    {
        val spacingMeters: Double = 10.0
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
        ConfirmationDialog( activity, resources.getString(R.string.warning), message, resources.getString(R.string.no), resources.getString(R.string.yes), point, false ) { buttonPressed, tag ->
            when( buttonPressed )
            {
                ConfirmationDialog.ButtonPress.Left -> {
                }
                ConfirmationDialog.ButtonPress.Right -> {
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
                ConfirmationDialog.ButtonPress.None -> {
                }
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

            if (breadcrumbState == BreadcrumbState.Trails && enumArea.breadcrumbs.isNotEmpty())
            {
                var groupId = ""

                val breadcrumbs = ArrayList<Breadcrumb>()

                for (breadcrumb in enumArea.breadcrumbs)
                {
                    if (breadcrumb.enumTeamName == enumerationTeam.name)
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

            if (breadcrumbState != BreadcrumbState.Gone && enumArea.breadcrumbs.isNotEmpty()) {
                MapManager.instance().loadBreadcrumbs(requireContext(), mapView, enumArea.breadcrumbs, enumerationTeam.name)
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
                    if (isRecordingBreadcrumbs && enumArea.breadcrumbs.isNotEmpty())
                    {
                        enumArea.breadcrumbs.add(Breadcrumb(enumArea.uuid, enumerationTeam.name, location.latitude, location.longitude, enumArea.breadcrumbs.last().groupId))
                    }

                    ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.is_multi_family), resources.getString(R.string.no), resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
                        when( buttonPressed )
                        {
                            ConfirmationDialog.ButtonPress.Left,
                            ConfirmationDialog.ButtonPress.Right -> {
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

                                if (buttonPressed == ConfirmationDialog.ButtonPress.Left)
                                {
                                    didNavigate = true
                                    val bundle = Bundle()
                                    bundle.putBoolean( Keys.kEditMode.value, gpsLocationIsGood( location ))
                                    findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)
                                }
                                else
                                {
                                    didNavigate = true
                                    val bundle = Bundle()
                                    bundle.putBoolean( Keys.kEditMode.value, gpsLocationIsGood( location ))
                                    bundle.putInt( Keys.kStartSubaddress.value, maxSubaddress)
                                    findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment,bundle)
                                }
                            }
                            ConfirmationDialog.ButtonPress.None -> {
                            }
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

                        nearbySessionStatusDialog = NearbySessionStatusDialog(requireContext(), resources.getString( R.string.export_configuration )) {
                            zipUtils.cancel()
                        }

                        viewLifecycleOwner.lifecycleScope.launch {
                            zipUtils.state.collect { state ->
                                nearbySessionStatusDialog?.updateState(state)
                            }
                        }

                        PerformanceManager.startTimer()

                        zipUtils.zipToUri( requireActivity(), config, getFileName(), includeConfig, includeImages,uri ) { success ->
                            if (success)
                            {
                                NotificationDialog( activity!!, resources.getString(R.string.success), resources.getString(R.string.export_succeeded))
                            }
                            else
                            {
                                NotificationDialog( activity!!, resources.getString(R.string.oops), resources.getString(R.string.export_failed))
                            }

                            nearbySessionStatusDialog?.dismiss()
                            nearbySessionStatusDialog = null

                            Log.d( "xxx", "Export time : ${PerformanceManager.elapsedTime()}")
                        }
                    }
                }
            }
        }
        catch (ex: java.lang.Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
            NotificationDialog( activity!!, resources.getString(R.string.oops), resources.getString(R.string.export_failed))
        }
    }

    override fun didSelectOkButton(tag: Any?)
    {
        dropMode = true
        binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
    }

    override fun didPressCancelButton()
    {
        MapManager.instance().cancelTilePackDownload()
    }

    override fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    {
        busyIndicatorDialog?.let {
            activity!!.runOnUiThread {
                it.updateProgress(resources.getString(R.string.downloading_map_tiles) + " ${numLoaded}/${numNeeded}")
            }
        }
    }

    override fun tilePacksLoaded( error: String )
    {
        activity!!.runOnUiThread {
            if (error.isNotEmpty())
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                    Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.tile_pack_download_failed), Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                busyIndicatorDialog?.alertDialog?.cancel()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_set_subaddress, menu)

        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("default", Context.MODE_PRIVATE)

        if (sharedPreferences.getBoolean( Keys.kDeveloperMode.value, false ))
        {
            menu.findItem(R.id.action_auto_enumerate).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            when (item.itemId)
            {
                R.id.action_auto_enumerate ->
                {
                    ConfirmationDialog(activity, resources.getString(R.string.please_confirm), "Auto enumerate these locations?", resources.getString(R.string.no), resources.getString(R.string.yes), DeleteMode.deleteStudyTag.value, false) { buttonPressed, tag ->
                        when (buttonPressed) {
                            ConfirmationDialog.ButtonPress.None -> {}
                            ConfirmationDialog.ButtonPress.Left -> {}
                            ConfirmationDialog.ButtonPress.Right -> {
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
                }

                R.id.set_subaddress ->
                {
                    InputDialog( activity!!, false, resources.getString(R.string.subaddress_start), "", resources.getString(R.string.cancel), resources.getString(R.string.save), null, false, true )  { action, text, tag ->
                        when (action) {
                            InputDialog.Action.DidCancel -> {}
                            InputDialog.Action.DidEnterText -> {
                                text.toIntOrNull()?.let {
                                    maxSubaddress = it - 1
                                }
                            }
                            InputDialog.Action.DidPressQRButton -> {}
                        }
                    }
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

    private val locationCallback = object : LocationCallback()
    {
        override fun onLocationResult(locationResult: LocationResult)
        {
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

                    if (isRecordingBreadcrumbs)
                    {
                        var distance = 10.1;
                        val currentLatLng = LatLng( point.latitude(), point.longitude())

                        if (!enumArea.breadcrumbs.isEmpty())
                        {
                            val lastBreadcrumb = enumArea.breadcrumbs.last()
                            val lastLatLng = LatLng( lastBreadcrumb.latitude, lastBreadcrumb.longitude )
                            distance = GeoUtils.distanceBetween( currentLatLng, lastLatLng )
                        }

                        if (distance > MIN_BREADCRUMB_METERS)
                        {
                            MapManager.instance().createMarker( activity!!, mapView, point, R.drawable.breadcrumb, "")

                            val breadcrumb = Breadcrumb( enumArea.uuid, enumerationTeam.name, point.latitude(), point.longitude(), lastBreadcrumbGroupId )
                            DAO.breadcrumbDAO.createOrUpdateBreadcrumb( breadcrumb, breadcrumb.version )
                            enumArea.breadcrumbs.add( breadcrumb )

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
        lastCenterPoint = MapManager.instance().getCenter( mapView )
        binding.mapboxMapView.gestures.removeOnMapClickListener(mapboxMapClickListener )
        binding.recyclerView.adapter = null

        nearbySessionStatusDialog?.dismiss()
        nearbySessionStatusDialog = null

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
