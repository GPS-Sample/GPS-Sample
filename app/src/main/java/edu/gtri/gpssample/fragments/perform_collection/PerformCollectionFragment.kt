/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.perform_collection

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.*
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformCollectionBinding
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.NearbySessionHostManager
import edu.gtri.gpssample.managers.PerformanceManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.ui.compose.ComposableAdditionalInfoDialogHost
import edu.gtri.gpssample.ui.compose.ComposableBusyIndicatorDialogHost
import edu.gtri.gpssample.ui.compose.ComposableCheckboxDialogHost
import edu.gtri.gpssample.ui.compose.ComposableCollectionHelpDialogHost
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableInputDialogHost
import edu.gtri.gpssample.ui.compose.ComposableMapLegendDialogHost
import edu.gtri.gpssample.ui.compose.ComposableNearbySessionStatusDialogHost
import edu.gtri.gpssample.ui.compose.ComposableNotificationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableSelectionDialogHost
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.utils.ZipUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

class PerformCollectionFragment : Fragment(),
    MapManager.MapTileCacheDelegate
{
    private lateinit var user: User
    private lateinit var mapView: View
    private lateinit var enumArea: EnumArea
    private lateinit var collectionTeam: CollectionTeam
    private lateinit var defaultColorList: ColorStateList
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel: ConfigurationViewModel
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var performCollectionAdapter: PerformCollectionAdapter
    private lateinit var mapboxMapClickListener: OnMapClickListener
    private var lastCenterPoint: Point? = null
    private var isHandlingTapEvent = false
    private val binding get() = _binding!!
    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: Point? = null
    private var landmarkLocations = ArrayList<Location>()
    private val collectionTeamLocations = ArrayList<Location>()
    private var _binding: FragmentPerformCollectionBinding? = null
    private val fragmentResultListener = "PerformCollectionFragment"
    private val enumerationItems = ArrayList<EnumerationItem>()
    private var includeConfig = false
    private var includeImages = false
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
    private lateinit var composableCollectionHelpDialogHost: ComposableCollectionHelpDialogHost
    private lateinit var composableAdditionalInfoDialogHost: ComposableAdditionalInfoDialogHost
    private lateinit var composableNearbySessionStatusDialogHost: ComposableNearbySessionStatusDialogHost

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm: ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm

        val samplingVm: SamplingViewModel by activityViewModels()
        samplingViewModel = samplingVm
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        setFragmentResultListener( fragmentResultListener ) { key, bundle ->
            bundle.getString( Keys.kRequest.value )?.let { request ->
                enumArea.locations.find { it.uuid == sharedViewModel.currentLocationUuid }?.let { location: Location ->
                    location.enumerationItems.find { it.uuid == sharedViewModel.currentEnumerationItemUuid }?.let { enumerationItem ->
                        if (gpsAccuracyIsGood() && gpsLocationIsGood(location))
                        {
                            when (request)
                            {
                                Keys.kAdditionalInfoRequest.value ->
                                {
                                    composableAdditionalInfoDialogHost.show(
                                        complete = true,
                                        incompleteReason = "",
                                        notes = ""
                                    ) { complete, incompleteReason, notes ->
                                        didSelectSaveButton( incompleteReason, notes )
                                    }
                                }

                                Keys.kLaunchSurveyRequest.value ->
                                {
                                    if (enumerationItem.odkRecordUri.isNotEmpty())
                                    {
                                        val uri = Uri.parse( enumerationItem.odkRecordUri )
                                        val intent = Intent(Intent.ACTION_EDIT)
                                        intent.setData(uri)
                                        odk_result.launch(intent)
                                    }
                                    else
                                    {
                                        // This will create a new ODK instance record
                                        composableBusyIndicatorDialogHost.show(title = resources.getString(R.string.launching_odk_collect_application), message = null)

                                        viewLifecycleOwner.lifecycleScope.launch {
                                            delay(2000.milliseconds)

                                            composableBusyIndicatorDialogHost.cancel()

                                            val intent = Intent(Intent.ACTION_VIEW)
                                            intent.type = "vnd.android.cursor.dir/vnd.odk.form"
                                            odk_result.launch(intent)
                                        }
                                    }
                                }
                            }
                        }
                        else if (!gpsAccuracyIsGood())
                        {
                            Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.gps_accuracy_error), Toast.LENGTH_LONG).show()
                        }
                        else
                        {
                            Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.gps_location_error), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentPerformCollectionBinding.inflate(inflater, container, false)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        composableNearbySessionStatusDialogHost = ComposableNearbySessionStatusDialogHost()

        composableInputDialogHost = ComposableInputDialogHost()
        composableCheckboxDialogHost = ComposableCheckboxDialogHost()
        composableMapLegendDialogHost = ComposableMapLegendDialogHost()
        composableSelectionDialogHost = ComposableSelectionDialogHost()
        composableConfirmationDialogHost = ComposableConfirmationDialogHost()
        composableNotificationDialogHost = ComposableNotificationDialogHost()
        composableBusyIndicatorDialogHost = ComposableBusyIndicatorDialogHost()
        composableCollectionHelpDialogHost = ComposableCollectionHelpDialogHost()
        composableAdditionalInfoDialogHost = ComposableAdditionalInfoDialogHost()
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
            composableCollectionHelpDialogHost.Content()
            composableAdditionalInfoDialogHost.Content()
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

        enumArea.collectionTeams.find { it.uuid == sharedViewModel.currentCollectionTeamUuid }?.let { collectionTeam ->
            this.collectionTeam = collectionTeam
        }

        if (config.mapEngineIndex == MapEngine.OpenStreetMap.value)
        {
            binding.osmMapView.visibility = View.VISIBLE
            binding.mapboxMapView.visibility = View.GONE
            MapManager.instance().centerMap( collectionTeam.polygon, binding.osmMapView )
        }
        else
        {
            binding.osmMapView.visibility = View.GONE
            binding.mapboxMapView.visibility = View.VISIBLE
            MapManager.instance().centerMap( collectionTeam.polygon, binding.mapboxMapView )
        }

        binding.progressOverlayView.visibility = View.GONE

        val _user = (activity!!.application as? MainApplication)?.user

        _user?.let { user ->
            this@PerformCollectionFragment.user = user
        }

        enumerationItems.clear()
        collectionTeamLocations.clear()

        for (teamLocationUuid in collectionTeam.locationUuids)
        {
            enumArea.locations.find { location -> location.uuid == teamLocationUuid  }?.let { location ->
                var locationAdded = false
                for (enumurationItem in location.enumerationItems)
                {
                    if (enumurationItem.samplingState == SamplingState.Sampled || enumurationItem.subsetSamplingState == SamplingState.Sampled)
                    {
                        if (!locationAdded)
                        {
                            locationAdded = true
                            collectionTeamLocations.add( location )
                        }
                        enumerationItems.add( enumurationItem )
                    }
                }
            }
        }

        landmarkLocations.clear()

        for (location in enumArea.locations)
        {
            if (location.isLandmark)
            {
                landmarkLocations.add( location )
            }
        }

        performCollectionAdapter = PerformCollectionAdapter( ArrayList<EnumerationItem>(), ArrayList<Location>(), enumArea.name )
        performCollectionAdapter.updateItems( enumerationItems, landmarkLocations )

        performCollectionAdapter.didSelectItem = this::didSelectItem

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performCollectionAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )
        binding.recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enumArea ->
            binding.titleTextView.text =  enumArea.name + " (" + collectionTeam.name + " " + resources.getString(R.string.team) + ")"
        }

        binding.mapTileCacheButton.backgroundTintList?.let {
            defaultColorList = it
        }

        if (isShowingBreadcrumbs)
        {
            binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
            binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
        }
        else
        {
            binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
            binding.showBreadcrumbsButton.setBackgroundTintList(defaultColorList);
        }


        val centerOnCurrentLocation = sharedViewModel.centerOnCurrentLocation?.value
        if (centerOnCurrentLocation == null)
        {
            sharedViewModel.setCenterOnCurrentLocation( false )
        }

        if (enumArea.mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( enumArea.mbTilesPath )
        }

        MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
            this.mapView = mapView

            MapManager.instance().enableLocationUpdates(activity!!, mapView)

            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

            lastCenterPoint?.let {
                MapManager.instance().centerMap(it, mapView )
            } ?: run {
                MapManager.instance().centerMap(collectionTeam.polygon, mapView )
            }

            sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
                if (centerOnCurrentLocation)
                {
                    MapManager.instance().startCenteringOnLocation( activity!!, mapView )
                    binding.centerOnLocationButton.setBackgroundTintList( ColorStateList.valueOf( resources.getColor(android.R.color.holo_red_light)));
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
                                    sharedViewModel.currentLocationUuid = location.uuid

                                    if (location.enumerationItems.size > 1)
                                    {
                                        var count = 0
                                        var index = 0

                                        location.enumerationItems.forEachIndexed { i, enumerationItem ->
                                            if (enumerationItem.samplingState == SamplingState.Sampled || enumerationItem.subsetSamplingState == SamplingState.Sampled)
                                            {
                                                count += 1
                                                index = i
                                            }
                                        }

                                        if (count > 1)
                                        {
                                            val bundle = Bundle()
                                            bundle.putBoolean( Keys.kGpsAccuracyIsGood.value, gpsAccuracyIsGood())
                                            bundle.putBoolean( Keys.kGpsLocationIsGood.value, gpsLocationIsGood( location ))

                                            findNavController().navigate(R.id.action_navigate_to_PerformMultiCollectionFragment, bundle)
                                        }
                                        else
                                        {
                                            didSelectItem( location.enumerationItems[index])
                                        }
                                    }
                                    else
                                    {
                                        didSelectItem( location.enumerationItems[0])
                                    }
                                }
                            }
                        }
                    }
                }
            }

            refreshMap()
        }

        if (ActivityCompat.checkSelfPermission( activity!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission( activity!!, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            val locationRequest = LocationRequest.create().apply {
                interval = 5000
                fastestInterval = 2000
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
            fusedLocationClient.requestLocationUpdates( locationRequest, locationCallback, Looper.getMainLooper())
        }

        val views = ArrayList<String>()
        val showViews = resources.getTextArray( R.array.show_views )

        for (showView in showViews)
        {
            views.add( showView.toString())
        }

        binding.showSpinner.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, views )

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

        val items = ArrayList<String>()
        val sortFilters = resources.getTextArray( R.array.sort_filters )

        for (sortFilter in sortFilters)
        {
            items.add( sortFilter.toString())
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
            if (study.subsetRules.isNotEmpty() && study.subsetFilters.isNotEmpty())
            {
                items.add( resources.getString( R.string.primary_sample ))
                items.add( resources.getString( R.string.subset_sample ))
            }
        }

        binding.filterSpinner.adapter = ArrayAdapter<String>(this@PerformCollectionFragment.requireContext(), android.R.layout.simple_spinner_dropdown_item, items )

        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
            {
                // Note! OnItemSelected fires automatically when the fragment is created
                when( position )
                {
                    0-> { // nothing
                        for (enumerationItem in enumerationItems)
                        {
                            enumerationItem.isVisible = true
                        }
                        for (location in collectionTeamLocations) // includes landmarks!
                        {
                            location.isVisible = true
                        }
                    }
                    1-> { // undefined
                        for (location in collectionTeamLocations)
                        {
                            location.isVisible = false
                        }

                        for (enumerationItem in enumerationItems)
                        {
                            enumerationItem.isVisible = false

                            if (enumerationItem.collectionState == CollectionState.Undefined)
                            {
                                enumerationItem.isVisible = true
                                for (location in collectionTeamLocations)
                                {
                                    for (enumItem in location.enumerationItems)
                                    {
                                        if (enumItem.uuid == enumerationItem.uuid)
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
                        for (location in collectionTeamLocations)
                        {
                            location.isVisible = false
                        }

                        for (enumerationItem in enumerationItems)
                        {
                            enumerationItem.isVisible = false

                            if (enumerationItem.collectionState == CollectionState.Incomplete)
                            {
                                enumerationItem.isVisible = true
                                for (location in collectionTeamLocations)
                                {
                                    for (enumItem in location.enumerationItems)
                                    {
                                        if (enumItem.uuid == enumerationItem.uuid)
                                        {
                                            location.isVisible = true
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3-> { // complete
                        for (location in collectionTeamLocations)
                        {
                            location.isVisible = false
                        }

                        for (enumerationItem in enumerationItems)
                        {
                            enumerationItem.isVisible = false

                            if (enumerationItem.collectionState == CollectionState.Complete)
                            {
                                enumerationItem.isVisible = true
                                for (location in collectionTeamLocations)
                                {
                                    for (enumItem in location.enumerationItems)
                                    {
                                        if (enumItem.uuid == enumerationItem.uuid)
                                        {
                                            location.isVisible = true
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4-> { // points of interest
                        for (location in collectionTeamLocations)
                        {
                            location.isVisible = if (location.isLandmark) true else false
                        }
                        for (enumerationItem in enumerationItems)
                        {
                            enumerationItem.isVisible = false
                        }
                    }
                    5 -> { // Primary Sample
                        for (location in collectionTeamLocations)
                        {
                            location.isVisible = false
                        }

                        for (enumerationItem in enumerationItems)
                        {
                            enumerationItem.isVisible = false

                            if (enumerationItem.samplingState == SamplingState.Sampled)
                            {
                                enumerationItem.isVisible = true
                                for (location in collectionTeamLocations)
                                {
                                    for (enumItem in location.enumerationItems)
                                    {
                                        if (enumItem.uuid == enumerationItem.uuid)
                                        {
                                            location.isVisible = true
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                    6 -> { // Subset Sample
                        for (location in collectionTeamLocations)
                        {
                            location.isVisible = false
                        }

                        for (enumerationItem in enumerationItems)
                        {
                            enumerationItem.isVisible = false

                            if (enumerationItem.subsetSamplingState == SamplingState.Sampled)
                            {
                                enumerationItem.isVisible = true
                                for (location in collectionTeamLocations)
                                {
                                    for (enumItem in location.enumerationItems)
                                    {
                                        if (enumItem.uuid == enumerationItem.uuid)
                                        {
                                            location.isVisible = true
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                performCollectionAdapter.updateItems( enumerationItems, landmarkLocations )
                refreshMap()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.mapTileCacheButton.setOnClickListener {
            enumArea.mapTileRegion?.let {
                val mapTileRegions = ArrayList<MapTileRegion>()
                mapTileRegions.add( it )
                composableBusyIndicatorDialogHost.show(title = resources.getString(R.string.downloading_map_tiles), message = null) {
                    composableBusyIndicatorDialogHost.cancel()
                    MapManager.instance().cancelTilePackDownload()
                }

                MapManager.instance().cacheMapTiles(activity!!, mapView, mapTileRegions, this@PerformCollectionFragment )
            }
        }

        binding.legendTextView.setOnClickListener {
            composableMapLegendDialogHost.show()
        }

        binding.legendImageView.setOnClickListener {
            composableMapLegendDialogHost.show()
        }

        binding.helpButton.setOnClickListener {
            composableCollectionHelpDialogHost.show()
        }

        binding.centerOnLocationButton.setOnClickListener {
            sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
                if (centerOnCurrentLocation)
                {
                    sharedViewModel.setCenterOnCurrentLocation( false )
                    MapManager.instance().stopCenteringOnLocation( mapView )
                    binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                }
                else
                {
                    sharedViewModel.setCenterOnCurrentLocation( true )
                    MapManager.instance().startCenteringOnLocation( activity!!, mapView )
                    binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                }
                refreshMap()
            }
        }

        binding.exportButton.setOnClickListener {
            var title = ""

            if (user.role == Role.Admin.value || user.role == Role.Supervisor.value)
            {
                title = resources.getString(R.string.export_configuration)
            }
            else
            {
                title = resources.getString(R.string.export_collection_data)
            }

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

                                    zipUtils.zipToPublicDocuments( requireActivity(), config, getFileName(), "Surveyed", includeConfig, includeImages,) { success ->
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

        binding.showBreadcrumbsButton.setOnClickListener {
            if (isShowingBreadcrumbs)
            {
                isShowingBreadcrumbs = false
                binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
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
                    selectedTeamNames.add( enumArea.enumerationTeams.first().name )
                    selectedBreadcrumbs.addAll( enumArea.breadcrumbs)
                    binding.showBreadcrumbsButton.setBackgroundResource(R.drawable.navigate2)
                    binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                    refreshMap()
                }
                else
                {
                    val choices = ArrayList<String>()
                    val isChecked = ArrayList<Boolean>()

                    for (team in enumArea.enumerationTeams)
                    {
                        choices.add( team.name )
                        isChecked.add( if (team.name == collectionTeam.name) true else false )
                    }

                    composableCheckboxDialogHost.show(
                        title = resources.getString(R.string.select_enumeration_teams),
                        items = choices,
                        isChecked = emptyList()
                    ) { selections ->
                        if (selections.isNotEmpty())
                        {
                            selectedTeamNames.clear()
                            selectedBreadcrumbs.clear()

                            for (selection in selections)
                            {
                                for (team in enumArea.enumerationTeams)
                                {
                                    if (team.name == selection)
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
                            binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                            refreshMap()
                        }
                    }
                }
            }
        }

        updateSummaryInfo()
    }

    fun autoSurveyLocations()
    {
        DAO.instance().writableDatabase.beginTransaction()

        for (location in collectionTeamLocations)
        {
            if (!location.isLandmark && location.enumerationItems.isNotEmpty())
            {
                if (location.enumerationItems.size == 1)
                {
                    val sampledItem = location.enumerationItems[0]
                    if (sampledItem.samplingState == SamplingState.Sampled)
                    {
                        sampledItem.collectionState = CollectionState.Complete
                        DAO.enumerationItemDAO.createOrUpdateEnumerationItem(sampledItem,UUID.randomUUID().toString())
                    }
                }
                else
                {
                    for (sampledItem in location.enumerationItems)
                    {
                        if (sampledItem.samplingState == SamplingState.Sampled)
                        {
                            sampledItem.collectionDate = Date().time
                            sampledItem.collectionState = CollectionState.Complete

                            (activity!!.application as MainApplication).user?.let { user ->
                                sampledItem.collectorName = user.name
                            }

                            DAO.enumerationItemDAO.createOrUpdateEnumerationItem( sampledItem,UUID.randomUUID().toString() )
                        }
                    }
                }
            }
        }

        DAO.instance().writableDatabase.setTransactionSuccessful()
        DAO.instance().writableDatabase.endTransaction()
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
                    return "${role}-${userName}-${clusterName}-DC-${dateTime!!}-${versionName}"
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

    fun updateSummaryInfo()
    {
        var sampledCount = 0
        var enumerationCount = 0
        var surveyedCount = 0

        for (location in collectionTeamLocations)
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

        binding.listItemEnumArea.titleLayout.visibility = View.GONE
        binding.listItemEnumArea.numberEnumeratedTextView.text = "$enumerationCount"
        binding.listItemEnumArea.numberSampledTextView.text = "$sampledCount"
        binding.listItemEnumArea.numberSurveyedTextView.text = "$surveyedCount"
    }

    override fun onResume()
    {
        super.onResume()
        isHandlingTapEvent = false
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformCollectionFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun refreshMap()
    {
        if (!this::mapView.isInitialized)
        {
            return
        }

        MapManager.instance().clearMap( mapView )

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        collectionTeam.polygon.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty() && pointList[0].isNotEmpty())
        {
            MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x20, Color.RED, enumArea.name )

            if (isShowingBreadcrumbs && selectedBreadcrumbs.isNotEmpty())
            {
                MapManager.instance().loadBreadcrumbs(requireContext(), mapView, selectedBreadcrumbs, selectedTeamNames )

                var groupId = ""
                val breadcrumbs = ArrayList<Breadcrumb>()

                for (breadcrumb in selectedBreadcrumbs)
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

                if (breadcrumbs.isNotEmpty())
                {
                    MapManager.instance().createPolyline( mapView, breadcrumbs )
                }
            }

            for (location in landmarkLocations)
            {
                if (location.isVisible)
                {
                    MapManager.instance().createMarker( activity!!, mapView, location, R.drawable.location_blue, "" )
                }
            }

            val markerProperties = ArrayList<MapManager.MarkerProperty>()

            for (location in collectionTeamLocations)
            {
                if (!location.isLandmark && location.isVisible && location.enumerationItems.isNotEmpty())
                {
                    var resourceId = 0

                    if (location.enumerationItems.size == 1)
                    {
                        val sampledItem = location.enumerationItems[0]

                        if (sampledItem.samplingState == SamplingState.Sampled || sampledItem.subsetSamplingState == SamplingState.Sampled)
                        {
                            when( sampledItem.collectionState )
                            {
                                CollectionState.Undefined -> resourceId = R.drawable.home_light_blue
                                CollectionState.Incomplete -> resourceId = R.drawable.home_orange
                                CollectionState.Complete -> resourceId = R.drawable.home_purple
                            }
                        }
                    }
                    else
                    {
                        for (sampledItem in location.enumerationItems)
                        {
                            if ((sampledItem.samplingState == SamplingState.Sampled && sampledItem.collectionState == CollectionState.Undefined)
                            ||  (sampledItem.subsetSamplingState == SamplingState.Sampled && sampledItem.collectionState == CollectionState.Undefined))
                            {
                                resourceId = R.drawable.multi_home_light_blue
                                break
                            }
                        }

                        if (resourceId == 0)
                        {
                            for (sampledItem in location.enumerationItems)
                            {
                                if (sampledItem.samplingState == SamplingState.Sampled || sampledItem.subsetSamplingState == SamplingState.Sampled)
                                {
                                    if (sampledItem.collectionState == CollectionState.Incomplete)
                                    {
                                        resourceId = R.drawable.multi_home_orange
                                        break
                                    }
                                    else if (sampledItem.collectionState == CollectionState.Complete)
                                    {
                                        resourceId = R.drawable.multi_home_purple
                                    }
                                }
                            }
                        }
                    }

                    if (resourceId > 0)
                    {
                        var title = ""
                        if (location.enumerationItems.isNotEmpty())
                        {
                            title = location.enumerationItems[0].subAddress
                        }

                        markerProperties.add( MapManager.MarkerProperty( location, resourceId, title ))
                    }
                }
            }

            if (markerProperties.isNotEmpty())
            {
                MapManager.instance().loadMarkers( activity!!, mapView, markerProperties, mapboxMapClickListener )
            }
        }
    }

    private fun didSelectItem( item: Any )
    {
        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->

            if (item is Location)
            {
                sharedViewModel.currentLocationUuid = item.uuid
                findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
            }
            else if (item is EnumerationItem)
            {
                enumArea.locations.find { it.uuid == item.locationUuid }?.let { location: Location ->
                    sharedViewModel.currentLocationUuid = item.locationUuid
                    sharedViewModel.currentEnumerationItemUuid = item.uuid

                    (this.activity!!.application as? MainApplication)?.currentEnumerationItemUUID = item.uuid
                    (this.activity!!.application as? MainApplication)?.currentEnumerationAreaName = enumArea.name
                    (this.activity!!.application as? MainApplication)?.currentSubAddress = item.subAddress

                    val bundle = Bundle()
                    bundle.putBoolean( Keys.kEditMode.value, false )
                    bundle.putBoolean( Keys.kCollectionMode.value, true )
                    bundle.putString( Keys.kFragmentResultListener.value, fragmentResultListener )

                    findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment, bundle)
                } ?: run {isHandlingTapEvent = false}
            }
            else
            {
                isHandlingTapEvent = false
            }
        }
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private val odk_result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == Activity.RESULT_OK)
        {
            result.data?.data?.let { uri ->
                enumArea.locations.find { it.uuid == sharedViewModel.currentLocationUuid }?.let { location: Location ->
                    location.enumerationItems.find { it.uuid == sharedViewModel.currentEnumerationItemUuid }?.let { enumerationItem ->
                        if (enumerationItem.odkRecordUri.isEmpty())
                        {
                            enumerationItem.odkRecordUri = uri.toString()
                            didSelectSaveButton( "Other", "User canceled action, ODK record saved.")
                        }
                    }
                }
            }

            val mainApplication = activity!!.application as MainApplication

            mainApplication.currentSubAddress = mainApplication.defaultSubAddress
            mainApplication.currentEnumerationItemUUID = mainApplication.defaultEnumerationItemUUID
            mainApplication.currentEnumerationAreaName = mainApplication.defaultEnumerationAreaName

            composableAdditionalInfoDialogHost.show(
                complete = true,
                incompleteReason = "",
                notes = ""
            ) { complete, incompleteReason, notes ->
                didSelectSaveButton( incompleteReason, notes )
            }
        }
    }

    fun didSelectSaveButton( incompleteReason: String?, notes: String )
    {
        enumArea.locations.find { it.uuid == sharedViewModel.currentLocationUuid }?.let { location: Location ->
            location.enumerationItems.find { it.uuid == sharedViewModel.currentEnumerationItemUuid }?.let { enumerationItem ->
                enumerationItem.collectionNotes = notes
                enumerationItem.collectionDate = Date().time
                enumerationItem.version = UUID.randomUUID().toString()
                enumerationItem.collectionState = CollectionState.Complete

                (activity!!.application as MainApplication).user?.let { user ->
                    enumerationItem.collectorName = user.name
                }

                if (!incompleteReason.isNullOrEmpty())
                {
                    enumerationItem.collectionState = CollectionState.Incomplete
                    enumerationItem.collectionIncompleteReason = incompleteReason
                }

                DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem,UUID.randomUUID().toString() )

                collectionTeamLocations.clear()
                val enumerationItems = ArrayList<EnumerationItem>()

                for (teamLocationUuid in collectionTeam.locationUuids)
                {
                    enumArea.locations.find { location -> location.uuid == teamLocationUuid  }?.let { location ->
                        collectionTeamLocations.add( location )
                        for (enumurationItem in location.enumerationItems)
                        {
                            if (enumurationItem.samplingState == SamplingState.Sampled || enumerationItem.subsetSamplingState == SamplingState.Sampled)
                            {
                                enumerationItems.add( enumurationItem )
                            }
                        }
                    }
                }

                performCollectionAdapter.updateItems( enumerationItems, landmarkLocations )

                performCollectionAdapter.notifyDataSetChanged()

                updateSummaryInfo()

                refreshMap()
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_map_style_min, menu)

        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("default", Context.MODE_PRIVATE)

        if (sharedPreferences.getBoolean( Keys.kDeveloperMode.value, false ))
        {
            menu.findItem(R.id.action_auto_survey).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        lateinit var config: Config

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        when (item.itemId)
        {
            R.id.action_auto_survey ->
            {
                composableConfirmationDialogHost.show(
                    title = resources.getString(R.string.please_confirm),
                    message = "Auto survey these locations?",
                    leftButtonText = resources.getString(R.string.no),
                    rightButtonText = resources.getString(R.string.yes),
                ) { selection ->
                    if (selection == resources.getString(R.string.yes)) {
                        binding.progressOverlayView.visibility = View.VISIBLE

                        viewLifecycleOwner.lifecycleScope.launch {
                            withContext(Dispatchers.IO )
                            {
                                autoSurveyLocations()
                            }

                            // back on the main thread...

                            binding.progressOverlayView.visibility = View.GONE

                            refreshMap()
                            updateSummaryInfo()
                            performCollectionAdapter.updateItems( performCollectionAdapter.enumerationItems, performCollectionAdapter.locations )

                            Toast.makeText(activity!!.applicationContext,  "Auto Survey Complete.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            R.id.mapbox_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                editor.commit()

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
                    refreshMap()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private var lastLocationUpdateTime: Long = 0

    private val locationCallback = object : LocationCallback()
    {
        override fun onLocationResult(locationResult: LocationResult)
        {
            val location = locationResult.locations.last()
            val accuracy = location.accuracy.toInt() // in meters
            val point = Point.fromLngLat( location.longitude, location.latitude )

            currentGPSLocation = point
            currentGPSAccuracy = accuracy

            if (_binding == null)
            {
                return
            }

            lateinit var config: Config

            sharedViewModel.currentConfiguration?.value?.let {
                config = it
            }

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
            binding.locationTextView.text = String.format( "%.7f, %.7f, %.0f", point.latitude(), point.longitude(), location.bearing)

            if (Date().time - lastLocationUpdateTime > 3000)
            {
                lastLocationUpdateTime = Date().time

                for (enumerationItem in performCollectionAdapter.enumerationItems)
                {
                    enumArea.locations.find { it.uuid == enumerationItem.locationUuid }?.let { location: Location ->
                        val currentLatLng = LatLng( point.latitude(), point.longitude())
                        val itemLatLng = LatLng( location.latitude, location.longitude )
                        val distance = GeoUtils.distanceBetween( currentLatLng, itemLatLng )
                        var (distanceValue, distanceUnits) = formatDistance( config, distance )
                        enumerationItem.distance = distanceValue
                        enumerationItem.distanceUnits = distanceUnits
                    }
                }

                for (location in performCollectionAdapter.locations)
                {
                    if (location.isLandmark)
                    {
                        val currentLatLng = LatLng( point.latitude(), point.longitude())
                        val itemLatLng = LatLng( location.latitude, location.longitude )
                        val distance = GeoUtils.distanceBetween( currentLatLng, itemLatLng )
                        var (distanceValue, distanceUnits) = formatDistance( config, distance )
                        location.distance = distanceValue
                        location.distanceUnits = distanceUnits
                    }
                }

                performCollectionAdapter.updateItems( performCollectionAdapter.enumerationItems, performCollectionAdapter.locations )
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

        if (this::fusedLocationClient.isInitialized)
        {
            fusedLocationClient.removeLocationUpdates( locationCallback )
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