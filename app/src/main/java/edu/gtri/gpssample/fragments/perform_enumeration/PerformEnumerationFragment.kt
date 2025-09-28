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
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.*
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformEnumerationBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.utils.ZipUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class PerformEnumerationFragment : Fragment(),
    View.OnTouchListener,
    MapManager.MapManagerDelegate,
    InfoDialog.InfoDialogDelegate,
    MapboxManager.MapTileCacheDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private lateinit var user: User
    private lateinit var mapView: View
    private lateinit var enumArea: EnumArea
    private lateinit var enumerationTeam: EnumerationTeam
    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var performEnumerationAdapter: PerformEnumerationAdapter

    private var _binding: FragmentPerformEnumerationBinding? = null
    private val binding get() = _binding!!

    private var lastBreadcrumbGroupId = ""

    private var dropMode = false
    private var isShowingBreadcrumbs = false
    private var isRecordingBreadcrumbs = false
    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: Point? = null
    private val enumerationTeamLocations = ArrayList<Location>()
    private var busyIndicatorDialog: BusyIndicatorDialog? = null

    private var maxSubaddress = 0
    private val REQUEST_CODE_PICK_CONFIG_DIR = 1001

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        val networkVm : NetworkViewModel by activityViewModels()
        sharedViewModel = vm
        sharedNetworkViewModel = networkVm
        sharedNetworkViewModel.currentFragment = this

        sharedViewModel.setCurrentCenterPoint( null )

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

        lateinit var config: Config

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
            sharedNetworkViewModel.networkHotspotModel.encryptionPassword = config.encryptionPassword
            sharedNetworkViewModel.networkClientModel.encryptionPassword = config.encryptionPassword
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        enumArea.enumerationTeams.find { it.uuid == sharedViewModel.currentEnumerationTeamUuid }?.let { enumerationTeam ->
            this.enumerationTeam = enumerationTeam
        }

        if (sharedViewModel.currentCenterPoint?.value == null)
        {
            val latLngBounds = GeoUtils.findGeobounds(enumerationTeam.polygon)
            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
            sharedViewModel.setCurrentCenterPoint( point )
        }

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

        MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView,this ) { mapView ->
            this.mapView = mapView

            MapManager.instance().enableLocationUpdates( activity!!, mapView )

            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

            sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                MapManager.instance().centerMap( enumerationTeam.polygon, currentZoomLevel, mapView )
            }

            sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
                if (centerOnCurrentLocation)
                {
                    MapManager.instance().startCenteringOnLocation( mapView )
                    binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                }
                else
                {
                    binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
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
                Log.d( "xxx", "locationServer.locationCallback NOT null" )
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

        binding.showSpinner.adapter = ArrayAdapter<String>(this.context!!, android.R.layout.simple_spinner_dropdown_item, views )

        binding.showSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
            {
                // Note! OnItemSelected fires automatically when the fragment is created
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

        binding.filterSpinner.adapter = ArrayAdapter<String>(this.context!!, android.R.layout.simple_spinner_dropdown_item, filters )

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
                busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.downloading_map_tiles), this )
                MapboxManager.loadStylePack( activity!!, this )
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
                    MapManager.instance().startCenteringOnLocation( mapView )
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
                sharedViewModel.currentConfiguration?.value?.let { config ->
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
                        val altitude = if (point.altitude().isNaN()) 0.0 else point.altitude()
                        val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
                        val location = Location( timeZone, LocationType.Enumeration, accuracy, point.latitude(), point.longitude(), altitude, true, "", "")

                        DAO.locationDAO.createOrUpdateLocation( location, enumArea )
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
                        sharedViewModel.currentConfiguration?.value?.let { config ->
                            sharedNetworkViewModel.setCurrentConfig(config)

                            when(user.role)
                            {
                                Role.Admin.toString(),
                                Role.Supervisor.toString() ->
                                {
                                    sharedNetworkViewModel.networkHotspotModel.setTitle(resources.getString(R.string.export_configuration))
                                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Export)
                                    sharedNetworkViewModel.networkHotspotModel.encryptionPassword = config.encryptionPassword
                                    startHotspot(view)
                                }

                                Role.Enumerator.toString() ->
                                {
                                    sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.EnumerationTeam)
                                    sharedNetworkViewModel.networkClientModel.currentConfig = config
                                    val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                                    getResult.launch(intent)
                                }
                            }
                        }
                    }
                    ConfirmationDialog.ButtonPress.Right -> {
                        ConfirmationDialog( activity, resources.getString(R.string.select_file_location), "", resources.getString(R.string.default_location), resources.getString(R.string.let_me_choose), null, true) { buttonPressed, tag ->
                            when( buttonPressed )
                            {
                                ConfirmationDialog.ButtonPress.Left -> {
                                    ZipUtils.exportToDefaultLocation( activity!!, config, getPathName(), shouldPackMinimal(), { success ->
                                        if (success)
                                        {
                                            Toast.makeText( activity!!.applicationContext, resources.getString(R.string.export_succeeded), Toast.LENGTH_LONG).show()
                                        }
                                        else
                                        {
                                            NotificationDialog( activity!!, resources.getString(R.string.oops), resources.getString(R.string.export_failed))
                                        }
                                    })
                                }
                                ConfirmationDialog.ButtonPress.Right -> {
                                    exportToDevice()
                                }
                                ConfirmationDialog.ButtonPress.None -> {
                                }
                            }
                        }
                    }
                    ConfirmationDialog.ButtonPress.None -> {
                    }
                }
            }
        }

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
        binding.listItemEnumArea.numberEnumeratedTextView.text = "$enumerationCount"
        binding.listItemEnumArea.numberSampledTextView.text = "$sampledCount"
        binding.listItemEnumArea.numberSurveyedTextView.text = "$surveyedCount"

        trimToolbarToFit( binding.toolbar )

        if (isRecordingBreadcrumbs)
        {
            binding.recordBreadcrumbsButton.setBackgroundResource( R.drawable.pause )
            binding.recordBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
        }

        if (isShowingBreadcrumbs)
        {
            binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
        }

        binding.recordBreadcrumbsButton.setOnClickListener {
            if (!isRecordingBreadcrumbs)
            {
                isShowingBreadcrumbs = true
                isRecordingBreadcrumbs = true
                lastBreadcrumbGroupId = UUID.randomUUID().toString()
                binding.recordBreadcrumbsButton.setBackgroundResource( R.drawable.pause )
                binding.recordBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
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

            if (!isShowingBreadcrumbs)
            {
                isShowingBreadcrumbs = true
                binding.showBreadcrumbsButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                isShowingBreadcrumbs = false
                binding.showBreadcrumbsButton.setBackgroundTintList(defaultColorList);
            }

            refreshMap()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun addHouseholdButtonPress()
    {
        currentGPSLocation?.let { point ->

            sharedViewModel.currentConfiguration?.value?.let { config ->
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

            val altitude = if (point.altitude().isNaN()) 0.0 else point.altitude()
            val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
            val location = Location( timeZone, LocationType.Enumeration, accuracy, point.latitude(), point.longitude(), altitude, false, "", "")

            DAO.locationDAO.createOrUpdateLocation( location, enumArea )
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
                    val altitude = if (pt.altitude().isNaN()) 0.0 else pt.altitude()
                    val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
                    val location = Location( timeZone, LocationType.Enumeration, accuracy, tag.latitude(), tag.longitude(), altitude, false, "", "")

                    DAO.locationDAO.createOrUpdateLocation( location, enumArea )
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

    fun trimToolbarToFit(toolbar: LinearLayout)
    {
        toolbar.post {
            val displayMetrics = toolbar.resources.displayMetrics
            val screenWidthPx = displayMetrics.widthPixels
            val density = displayMetrics.density
            val screenWidthDp = screenWidthPx / density

            val moreButtonWidthDp = 60f // 50dp button + 5dp margin start + 5dp margin end
            val minSpaceWidthDp = 50f

            val allButtons = mutableListOf<View>()

            // Collect only the buttons
            for (i in 0 until toolbar.childCount) {
                val child = toolbar.getChildAt(i)
                if (child is Button) {
                    allButtons.add(child)
                }
            }

            var usedWidthDp = 0f
            val visibleButtons = mutableListOf<View>()
            val overflowButtons = mutableListOf<View>()

            // Measure button width in dp (assuming fixed 50dp width and 5dp margins each side)
            allButtons.forEach { button ->
                val lp = button.layoutParams as? ViewGroup.MarginLayoutParams
                val widthDp = 50f + ((lp?.marginStart ?: 0) + (lp?.marginEnd ?: 0)) / density

                if (usedWidthDp + widthDp + moreButtonWidthDp > screenWidthDp) {
                    overflowButtons.add(button)
                } else {
                    usedWidthDp += widthDp
                    visibleButtons.add(button)
                }
            }

            if (overflowButtons.isNotEmpty())
            {
                allButtons.clear()

                for (i in 0 until toolbar.childCount) {
                    val child = toolbar.getChildAt(i)
                    allButtons.add(child)
                }

                // Remove everything from layout
                allButtons.forEach { toolbar.removeView(it) }

                // Add visible buttons back
                visibleButtons.forEach { toolbar.addView(it) }
                val space = Space(toolbar.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f // stretchable
                    ).apply {
                        val minWidthPx = (minSpaceWidthDp * density).toInt()
                        this.width = minWidthPx
                    }
                }

                toolbar.addView(space)

                val moreButton = Button(toolbar.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(50),
                        dpToPx(50)
                    ).apply {
                        marginStart = dpToPx(5)
                        marginEnd = dpToPx(5)
                    }
                    background = getDrawable(context, R.drawable.more_button)
                    setOnClickListener {
                        showMoreMenu(context, it, overflowButtons)
                    }
                }

                toolbar.addView(moreButton)
            }
        }
    }

    fun dpToPx(dp: Int): Int
    {
        return (dp * resources.displayMetrics.density).toInt()
    }

    fun showMoreMenu(context: Context, anchorView: View, overflowButtons: List<View>)
    {
        val popupMenu = PopupMenu(context, anchorView)

        overflowButtons.forEachIndexed { index, view ->
            when (view.id)
            {
                R.id.help_button -> popupMenu.menu.add(0, view.id, index, "Help")
                R.id.export_button -> popupMenu.menu.add(0, view.id, index, "Export Configuration")
                R.id.delete_breadcrumbs_button -> popupMenu.menu.add(0, view.id, index, "Delete the walked path")
                R.id.show_breadcrumbs_button ->
                {
                    if (isShowingBreadcrumbs)
                    {
                        popupMenu.menu.add(0, view.id, index, "Hide Breadcrumbs")
                    }
                    else
                    {
                        popupMenu.menu.add(0, view.id, index, "Show Breadcrumbs")
                    }
                }
            }
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId)
            {
                R.id.help_button -> binding.helpButton.performClick()
                R.id.export_button -> binding.exportButton.performClick()
                R.id.show_breadcrumbs_button -> binding.showBreadcrumbsButton.performClick()
                R.id.delete_breadcrumbs_button -> binding.deleteBreadcrumbsButton.performClick()
            }
            true
        }

        popupMenu.show()
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
        MapManager.instance().clearMap( mapView )

//        performEnumerationAdapter.updateLocations( enumerationTeamLocations )

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumerationTeam.polygon.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty() && pointList[0].isNotEmpty())
        {
            MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x40 )

            if (isShowingBreadcrumbs && enumArea.breadcrumbs.isNotEmpty())
            {
                for (breadcrumb in enumArea.breadcrumbs)
                {
                    MapManager.instance().createMarker( activity!!, mapView, Point.fromLngLat(breadcrumb.longitude, breadcrumb.latitude), R.drawable.breadcrumb, "")
                }

                val breadcrumbs = ArrayList<Breadcrumb>()
                var groupId: String = ""

                for (breadcrumb in enumArea.breadcrumbs)
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

            for (location in enumArea.locations)
            {
                if (location.isLandmark && location.isVisible)
                {
                    MapManager.instance().createMarker( activity!!, mapView, location, R.drawable.location_blue, "" )
                }
            }

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

                    MapManager.instance().createMarker( activity!!, mapView, location, resourceId, title )
                }
            }
        }
    }

    fun navigateToAddHouseholdFragment()
    {
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
                        enumArea.breadcrumbs.add(Breadcrumb( UUID.randomUUID().toString(), Date().time, enumArea.uuid, location.latitude, location.longitude, enumArea.breadcrumbs.last().groupId))
                    }

                    DAO.enumerationItemDAO.createOrUpdateEnumerationItem( EnumerationItem(), location )?.let { enumerationItem ->
                        location.enumerationItems.add( enumerationItem )

                        sharedViewModel.currentConfiguration?.value?.let { config ->
                            if (config.autoIncrementSubaddress)
                            {
                                enumerationItem.subAddress = "${maxSubaddress + 1}"
                            }
                        }

                        sharedViewModel.currentEnumerationItemUuid = enumerationItem.uuid

                        ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.is_multi_family), resources.getString(R.string.no), resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
                            when( buttonPressed )
                            {
                                ConfirmationDialog.ButtonPress.Left -> {
                                    val bundle = Bundle()
                                    bundle.putBoolean( Keys.kEditMode.value, gpsLocationIsGood( location ))
                                    findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)
                                }
                                ConfirmationDialog.ButtonPress.Right -> {
                                    val bundle = Bundle()
                                    bundle.putBoolean( Keys.kEditMode.value, gpsLocationIsGood( location ))
                                    bundle.putInt( Keys.kStartSubaddress.value, maxSubaddress)
                                    findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment,bundle)
                                }
                                ConfirmationDialog.ButtonPress.None -> {
                                }
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
                sharedViewModel.currentEnumerationItemUuid = location.enumerationItems[0].uuid
                findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment, bundle)
            }
            else
            {
                findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment, bundle)
            }
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

    fun shouldPackMinimal() : Boolean
    {
        (activity!!.application as MainApplication).user?.let { user->
            when(user.role)
            {
                Role.Admin.value,
                Role.Supervisor.value ->
                {
                    return true
                }

                Role.Enumerator.value,
                Role.DataCollector.value ->
                {
                    return false
                }
                Role.Undefined.value -> {}
            }
        }

        return false
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

            var version = ""
            val versionName = BuildConfig.VERSION_NAME.split( "#" )
            if (versionName.size == 2)
            {
                version = versionName[1]
            }

            val clusterName = enumArea.name.replace(" ", "" ).uppercase()

            when(user.role)
            {
                Role.Admin.value,
                Role.Supervisor.value ->
                {
                    return "${role}-${userName}-${clusterName}-EN-${dateTime!!}-${version}"
                }

                Role.Enumerator.value,
                Role.DataCollector.value ->
                {
                    return "${role}-${userName}-${clusterName}-${dateTime!!}-${version}"
                }
                Role.Undefined.value -> {}
            }
        }

        return ""
    }

    fun getPathName() : String
    {
        val path = Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS + "/GPSSample/Enumerated"
        val root = File( path )
        root.mkdirs()
        return path + "/" + getFileName()
    }

    fun exportToDevice()
    {
        val zipFileName = getFileName() + ".zip"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
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
                        ZipUtils.saveToDefaultLocation( activity!!, config, getPathName(), shouldPackMinimal()) { configFile, imageFile ->
                            if (configFile != null)
                            {
                                if (imageFile != null)
                                {
                                    ZipUtils.zipToUri( activity!!, listOf( configFile, imageFile ), uri ) { error ->
                                        if (error.isEmpty())
                                        {
                                            imageFile.delete()
                                            configFile.delete()
                                            Toast.makeText( activity!!.applicationContext, resources.getString(R.string.export_succeeded), Toast.LENGTH_LONG).show()
                                        }
                                        else
                                        {
                                            NotificationDialog( activity!!, resources.getString(R.string.oops), resources.getString(R.string.export_failed))
                                        }
                                    }
                                }
                                else
                                {
                                    ZipUtils.zipToUri( activity!!, listOf( configFile ), uri ) { error ->
                                        if (error.isEmpty())
                                        {
                                            configFile.delete()
                                            Toast.makeText( activity!!.applicationContext, resources.getString(R.string.export_succeeded), Toast.LENGTH_LONG).show()
                                        }
                                        else
                                        {
                                            NotificationDialog( activity!!, resources.getString(R.string.oops), resources.getString(R.string.export_failed))
                                        }
                                    }
                                }
                            }
                            else
                            {
                                NotificationDialog( activity!!, resources.getString(R.string.oops), resources.getString(R.string.export_failed))
                            }
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResultCode.BarcodeScanned.value) {
                val payload = it.data!!.getStringExtra(Keys.kPayload.value)

                val jsonObject = JSONObject(payload);

                Log.d("xxx", jsonObject.toString(2))

                val ssid = jsonObject.getString(Keys.kSSID.value)
                val pass = jsonObject.getString(Keys.kPass.value)
                val serverIp = jsonObject.getString(Keys.kIpAddress.value)

                Log.d("xxxx", "the ssid, pass, serverIP ${ssid}, ${pass}, ${serverIp}")

                sharedNetworkViewModel.connectHotspot(ssid, pass, serverIp)
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startHotspot(view : View)
    {
        sharedNetworkViewModel.createHotspot(view)
    }

    override fun didPressCancelButton()
    {
        MapboxManager.cancelStylePackDownload()
        MapboxManager.cancelTilePackDownload()
    }

    override fun stylePackLoaded( error: String )
    {
        activity!!.runOnUiThread {
            if (error.isNotEmpty())
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                    Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.style_pack_download_failed), Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                enumArea.mapTileRegion?.let {
                    val mapTileRegions = ArrayList<MapTileRegion>()
                    mapTileRegions.add( it )
                    MapboxManager.loadTilePacks( activity!!, mapTileRegions, this )
                }
            }
        }
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
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_set_subaddress, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            when (item.itemId)
            {
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

                    MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView,this ) { mapView ->
                        refreshMap()
                    }
                }

                R.id.satellite_streets ->
                {
                    val editor = activity!!.getSharedPreferences("default", 0).edit()
                    editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                    editor.commit()

                    MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView,this ) { mapView ->
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
            val point = Point.fromLngLat( location.longitude, location.latitude )

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
                binding.locationTextView.text = String.format( "%.7f, %.7f, %.0f", point.latitude(), point.longitude(), location.bearing)

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

                            val breadcrumb = Breadcrumb( enumArea.uuid, point.latitude(), point.longitude(), lastBreadcrumbGroupId )
                            DAO.breadcrumbDAO.createOrUpdateBreadcrumb( breadcrumb )
                            enumArea.breadcrumbs.add( breadcrumb )

                            refreshMap()
                        }
                    }

                    for (loc in enumArea.locations)
                    {
                        val currentLatLng = LatLng( point.latitude(), point.longitude())
                        val itemLatLng = LatLng( loc.latitude, loc.longitude )
                        val distance = GeoUtils.distanceBetween( currentLatLng, itemLatLng )
                        if (distance < 400) // display in meters or feet
                        {
                            if (config.distanceFormat == DistanceFormat.Meters)
                            {
                                loc.distance = distance
                                loc.distanceUnits = resources.getString( R.string.meters )
                            }
                            else
                            {
                                loc.distance = distance * 3.28084
                                loc.distanceUnits = resources.getString( R.string.feet )
                            }
                        }
                        else // display in kilometers or miles
                        {
                            if (config.distanceFormat == DistanceFormat.Meters)
                            {
                                loc.distance = distance / 1000.0
                                loc.distanceUnits = resources.getString( R.string.kilometers )
                            }
                            else
                            {
                                loc.distance = distance / 1609.34
                                loc.distanceUnits = resources.getString( R.string.miles )
                            }
                        }
                    }

                    performEnumerationAdapter.updateLocations( enumerationTeamLocations )
                }
            }
        }
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        view?.performClick()

        motionEvent?.let {
            if (it.action == MotionEvent.ACTION_DOWN) {
                if (dropMode)
                {
                    dropMode = false
                    val point = MapManager.instance().getLocationFromPixelPoint(mapView, motionEvent )
                    binding.addHouseholdButton.setBackgroundTintList(defaultColorList);

                    sharedViewModel.currentConfiguration?.value?.let { config ->
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
                    }

                    var accuracy = -1

                    currentGPSAccuracy?.let {
                        accuracy = it
                    }

                    val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
                    val location = Location( timeZone, LocationType.Enumeration, accuracy, point.latitude(), point.longitude(), 0.0, false, "", "")

                    if (gpsLocationIsGood( location ))
                    {
                        DAO.locationDAO.createOrUpdateLocation( location, enumArea )
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

                    return true
                }
            }
        }

        return false
    }

    override fun onMarkerTapped( location: Location )
    {
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

    override fun onZoomLevelChanged( zoomLevel: Double )
    {
        sharedViewModel.setCurrentZoomLevel( zoomLevel )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        if (LocationService.started)
        {
            val intent = Intent(activity!!, LocationService::class.java)
            activity!!.stopService( intent )
        }

        _binding = null
    }
}