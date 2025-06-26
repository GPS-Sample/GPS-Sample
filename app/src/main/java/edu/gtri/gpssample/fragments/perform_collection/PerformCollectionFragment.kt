/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.perform_collection

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
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
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.*
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformCollectionBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class PerformCollectionFragment : Fragment(),
    MapManager.MapManagerDelegate,
    MapboxManager.MapTileCacheDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate,
    AdditionalInfoDialog.AdditionalInfoDialogDelegate,
    SurveyLaunchNotificationDialog.SurveyLaunchNotificationDialogDelegate
{
    private lateinit var user: User
    private lateinit var mapView: View
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var collectionTeam: CollectionTeam
    private lateinit var defaultColorList: ColorStateList
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel: ConfigurationViewModel
    private lateinit var sharedNetworkViewModel: NetworkViewModel
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var performCollectionAdapter: PerformCollectionAdapter

    private val binding get() = _binding!!
    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: Point? = null
    private var landmarkLocations = ArrayList<Location>()
    private val collectionTeamLocations = ArrayList<Location>()
    private var busyIndicatorDialog: BusyIndicatorDialog? = null
    private var _binding: FragmentPerformCollectionBinding? = null

    private val fragmentResultListener = "PerformCollectionFragment"

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener( fragmentResultListener ) { key, bundle ->
            bundle.getString( Keys.kRequest.value )?.let { request ->
                sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
                    if (gpsAccuracyIsGood() && gpsLocationIsGood(location))
                    {
                        when (request)
                        {
                            Keys.kAdditionalInfoRequest.value -> AdditionalInfoDialog(activity, "", "", this)
                            Keys.kLaunchSurveyRequest.value ->
                            {
                                sharedViewModel.locationViewModel.currentEnumerationItem?.value?.let { enumerationItem ->
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
                                        SurveyLaunchNotificationDialog(activity!!, this)
                                    }
                                }
                            }
                        }
                    }
                    else if (!gpsAccuracyIsGood())
                    {
                        Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.gps_accuracy_error), Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.gps_location_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val vm: ConfigurationViewModel by activityViewModels()
        val networkVm: NetworkViewModel by activityViewModels()
        sharedViewModel = vm
        sharedNetworkViewModel = networkVm
        sharedNetworkViewModel.currentFragment = this

        sharedViewModel.setCurrentCenterPoint( null )

        val samplingVm: SamplingViewModel by activityViewModels()
        samplingViewModel = samplingVm
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

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

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
            sharedNetworkViewModel.networkHotspotModel.encryptionPassword = config.encryptionPassword
            sharedNetworkViewModel.networkClientModel.encryptionPassword = config.encryptionPassword
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        DAO.collectionTeamDAO.getCollectionTeam( enumArea.selectedCollectionTeamUuid )?.let {
            collectionTeam = it
        }

        if (sharedViewModel.currentCenterPoint?.value == null)
        {
            val latLngBounds = GeoUtils.findGeobounds(collectionTeam.polygon)
            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
            sharedViewModel.setCurrentCenterPoint( point )
        }

        val _user = (activity!!.application as? MainApplication)?.user

        _user?.let { user ->
            this.user = user
        }

        collectionTeamLocations.clear()
        val enumerationItems = ArrayList<EnumerationItem>()

        for (teamLocationUuid in collectionTeam.locationUuids)
        {
            enumArea.locations.find { location -> location.uuid == teamLocationUuid  }?.let { location ->
                collectionTeamLocations.add( location )
                for (enumurationItem in location.enumerationItems)
                {
                    if (enumurationItem.samplingState == SamplingState.Sampled)
                    {
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

        val centerOnCurrentLocation = sharedViewModel.centerOnCurrentLocation?.value
        if (centerOnCurrentLocation == null)
        {
            sharedViewModel.setCenterOnCurrentLocation( false )
        }

        if (enumArea.mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( enumArea.mbTilesPath )
        }

        MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, this ) { mapView ->
            this.mapView = mapView

            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

            sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                MapManager.instance().centerMap( collectionTeam.polygon, currentZoomLevel, mapView )
            }

            sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
                if (centerOnCurrentLocation)
                {
                    MapManager.instance().enableLocationUpdates(activity!!, mapView)
                    binding.centerOnLocationButton.setBackgroundTintList( ColorStateList.valueOf( resources.getColor(android.R.color.holo_red_light)));
                }
                else
                {
                    MapManager.instance().disableLocationUpdates(activity!!, mapView)
                    binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
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

        binding.mapTileCacheButton.setOnClickListener {
            enumArea.mapTileRegion?.let {
                busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.downloading_map_tiles), this )
                MapboxManager.loadStylePack( activity!!, this )
            }
        }

        binding.legendTextView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.legendImageView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.helpButton.setOnClickListener {
            PerformCollectionHelpDialog( activity!! )
        }

        binding.centerOnLocationButton.setOnClickListener {
            sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
                if (centerOnCurrentLocation)
                {
                    sharedViewModel.setCenterOnCurrentLocation( false )
                    MapManager.instance().disableLocationUpdates( activity!!, mapView )
                    binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                }
                else
                {
                    sharedViewModel.setCenterOnCurrentLocation( true )
                    MapManager.instance().enableLocationUpdates( activity!!, mapView )
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

                                Role.Enumerator.toString(),
                                Role.DataCollector.toString() ->
                                {
                                    sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enumArea ->
                                        sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.CollectionTeam)
                                        sharedNetworkViewModel.networkClientModel.currentConfig = config
                                        val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                                        getResult.launch(intent)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    ConfirmationDialog.ButtonPress.Right -> {
                        ConfirmationDialog( activity, resources.getString(R.string.select_file_location), "", resources.getString(R.string.default_location), resources.getString(R.string.let_me_choose), null,true ) { buttonPressed, tag ->
                            when( buttonPressed )
                            {
                                ConfirmationDialog.ButtonPress.Left -> {
                                    exportToDefaultLocation()
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

        updateSummaryInfo()
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

        binding.listItemEnumArea.titleLayout.visibility = View.GONE
        binding.listItemEnumArea.numberEnumeratedTextView.text = "$enumerationCount"
        binding.listItemEnumArea.numberSampledTextView.text = "$sampledCount"
        binding.listItemEnumArea.numberSurveyedTextView.text = "$surveyedCount"
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformCollectionFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun refreshMap()
    {
        MapManager.instance().clearMap( mapView )

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        collectionTeam.polygon.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty() && pointList[0].isNotEmpty())
        {
            MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x40 )

            for (location in enumArea.locations)
            {
                if (location.isLandmark)
                {
                    MapManager.instance().createMarker( activity!!, mapView, location, R.drawable.location_blue, "" )
                }
            }

            for (location in collectionTeamLocations)
            {
                if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                {
                    var resourceId = 0

                    if (location.enumerationItems.size == 1)
                    {
                        val sampledItem = location.enumerationItems[0]

                        if (sampledItem.samplingState == SamplingState.Sampled)
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
                            if (sampledItem.samplingState == SamplingState.Sampled && sampledItem.collectionState == CollectionState.Undefined)
                            {
                                resourceId = R.drawable.multi_home_light_blue
                                break
                            }
                        }

                        if (resourceId == 0)
                        {
                            for (sampledItem in location.enumerationItems)
                            {
                                if (sampledItem.samplingState == SamplingState.Sampled)
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

                        MapManager.instance().createMarker( activity!!, mapView, location, resourceId, title )
                    }
                }
            }
        }
    }

    private fun didSelectItem( item: Any )
    {
        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->

            if (item is Location)
            {
                sharedViewModel.locationViewModel.setCurrentLocation(item)
                findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
            }
            else if (item is EnumerationItem)
            {
                DAO.locationDAO.getLocation( item.locationUuid )?.let { location ->
                    sharedViewModel.locationViewModel.setCurrentLocation(location)
                    sharedViewModel.locationViewModel.setCurrentEnumerationItem(item)
                    (this.activity!!.application as? MainApplication)?.currentEnumerationItemUUID = item.uuid
                    (this.activity!!.application as? MainApplication)?.currentEnumerationAreaName = enumArea.name
                    (this.activity!!.application as? MainApplication)?.currentSubAddress = item.subAddress

                    val bundle = Bundle()
                    bundle.putBoolean( Keys.kEditMode.value, false )
                    bundle.putBoolean( Keys.kCollectionMode.value, true )
                    bundle.putString( Keys.kFragmentResultListener.value, fragmentResultListener )
                    findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment, bundle)
                }
            }
        }
    }

    fun exportToDefaultLocation()
    {
        try
        {
            var payload: String = ""
            var fileName: String = ""

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

            val clusterName = enumArea.name.replace(" ", "" ).uppercase()

            when(user.role) {
                Role.Admin.toString(),
                Role.Supervisor.toString() ->
                {
                    sharedViewModel.currentConfiguration?.value?.let { config ->
                        fileName = "${role}-${userName}-${clusterName}-DC-${dateTime!!}-${version}.json"
                        payload = config.packMinimal()
                    }
                }

                Role.Enumerator.toString(),
                Role.DataCollector.toString() ->
                {
                    sharedViewModel.currentConfiguration?.value?.let { config ->
                        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enumArea ->
                            fileName = "D-${userName}-${clusterName}-${dateTime!!}-${version}.json"
                            payload = config.pack()
                        }
                    }
                }
            }

            val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS + "/GPSSample/Surveyed")
            root.mkdirs()
            val file = File(root, fileName)
            val writer = FileWriter(file)
            writer.append(payload)
            writer.flush()
            writer.close()

            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved), Toast.LENGTH_SHORT).show()
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
            Toast.makeText( activity!!.applicationContext, ex.stackTraceToString(), Toast.LENGTH_LONG).show()
        }
    }

    fun exportToDevice()
    {
        var fileName: String = ""

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

        val clusterName = enumArea.name.replace(" ", "" ).uppercase()

        sharedViewModel.currentConfiguration?.value?.let { config ->
            when(user.role)
            {
                Role.Admin.toString(),
                Role.Supervisor.toString() ->
                {
                    fileName = "${role}-${userName}-${clusterName}-DC-${dateTime!!}-${version}.json"
                }

                Role.Enumerator.toString(),
                Role.DataCollector.toString() ->
                {
                    fileName = "D-${userName}-${clusterName}-${dateTime!!}-${version}.json"
                }
            }
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        startActivityForResult( intent, REQUEST_CODE_PICK_DIR )
    }

    val REQUEST_CODE_PICK_DIR = 1001

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        try
        {
            if (requestCode == REQUEST_CODE_PICK_DIR && resultCode == Activity.RESULT_OK)
            {
                data?.data?.let { uri ->
                    sharedViewModel.currentConfiguration?.value?.let { config ->
                        var packedConfig = ""

                        when( user.role )
                        {
                            Role.Supervisor.toString(), Role.Admin.toString() ->
                            {
                                packedConfig = config.packMinimal()
                            }

                            Role.Enumerator.toString(), Role.DataCollector.toString() ->
                            {
                                packedConfig = config.pack()
                            }
                        }

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
        }
        catch (ex: java.lang.Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
            Toast.makeText( activity!!.applicationContext, ex.stackTraceToString(), Toast.LENGTH_LONG).show()
        }
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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun shouldLaunchODK()
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.type = "vnd.android.cursor.dir/vnd.odk.form"
            odk_result.launch(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val odk_result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == Activity.RESULT_OK)
        {
            result.data?.data?.let { uri ->
                sharedViewModel.locationViewModel.currentEnumerationItem?.value?.let { enumerationItem ->
                    if (enumerationItem.odkRecordUri.isEmpty())
                    {
                        enumerationItem.odkRecordUri = uri.toString()
                        didSelectSaveButton( "Other", "User canceled action, ODK record saved.")
                    }
                }
            }

            val mainApplication = activity!!.application as MainApplication

            mainApplication.currentSubAddress = mainApplication.defaultSubAddress
            mainApplication.currentEnumerationItemUUID = mainApplication.defaultEnumerationItemUUID
            mainApplication.currentEnumerationAreaName = mainApplication.defaultEnumerationAreaName

            AdditionalInfoDialog( activity, "", "", this)
        }
    }

    override fun didSelectCancelButton()
    {
    }

    override fun didSelectSaveButton( incompleteReason: String, notes: String )
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->

            sharedViewModel.locationViewModel.currentEnumerationItem?.value?.let { sampledItem ->

                sampledItem.collectionNotes = notes
                sampledItem.collectionDate = Date().time
                sampledItem.syncCode = sampledItem.syncCode + 1
                sampledItem.collectionState = CollectionState.Complete

                (activity!!.application as MainApplication).user?.let { user ->
                    sampledItem.collectorName = user.name
                }

                if (incompleteReason.isNotEmpty())
                {
                    sampledItem.collectionState = CollectionState.Incomplete
                    sampledItem.collectionIncompleteReason = incompleteReason
                }

                DAO.enumerationItemDAO.createOrUpdateEnumerationItem( sampledItem, location )

                enumArea.locations = DAO.locationDAO.getLocations( collectionTeam )

                collectionTeamLocations.clear()
                val enumerationItems = ArrayList<EnumerationItem>()

                for (teamLocationUuid in collectionTeam.locationUuids)
                {
                    enumArea.locations.find { location -> location.uuid == teamLocationUuid  }?.let { location ->
                        collectionTeamLocations.add( location )
                        for (enumurationItem in location.enumerationItems)
                        {
                            if (enumurationItem.samplingState == SamplingState.Sampled)
                            {
                                enumerationItems.add( enumurationItem )
                            }
                        }
                    }
                }

                performCollectionAdapter.updateItems( enumerationItems, landmarkLocations )

                sharedViewModel.currentConfiguration?.value?.let { config ->
                    DAO.configDAO.getConfig( config.uuid )?.let {
                        sharedViewModel.setCurrentConfig( it )
                    }
                }

                sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
                    DAO.enumAreaDAO.getEnumArea( enumArea.uuid )?.let {
                        sharedViewModel.enumAreaViewModel.setCurrentEnumArea( it )
                    }
                }

                sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                    DAO.studyDAO.getStudy( study.uuid )?.let {
                        sharedViewModel.createStudyModel.setStudy( it )
                    }
                }

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
            currentGPSLocation?.let { point ->
                val distance = GeoUtils.distanceBetween( LatLng( location.latitude, location.longitude ), LatLng( point.latitude(), point.longitude()))
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    editMode = distance <= config.minGpsPrecision
                }
            }
        }

        return editMode
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

    override fun didPressCancelButton()
    {
        MapboxManager.cancelStylePackDownload()
        MapboxManager.cancelTilePackDownload()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_map_style_min, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            R.id.mapbox_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                editor.commit()

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, this ) { mapView ->
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, this ) { mapView ->
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

            binding.locationTextView.text = String.format( "%.7f, %.7f", point.latitude(), point.longitude())

            if (Date().time - lastLocationUpdateTime > 3000)
            {
                lastLocationUpdateTime = Date().time

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

                performCollectionAdapter.updateItems( performCollectionAdapter.enumerationItems, performCollectionAdapter.locations )
            }
        }
    }

    override fun onMarkerTapped( location: Location )
    {
        sharedViewModel.locationViewModel.setCurrentLocation(location)

        if (location.isLandmark)
        {
            findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
        }
        else
        {
            sharedViewModel.locationViewModel.setCurrentLocation(location)

            if (location.isLandmark)
            {
                findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
            }
            else
            {
                var count = 0

                for (enumerationItem in location.enumerationItems)
                {
                    if (enumerationItem.samplingState == SamplingState.Sampled)
                    {
                        count += 1

                        // This is really only necessary here for the enumerationItems.size == 1 case
                        // For size > 1, this will get set in the multiCollectionFragment
                        sharedViewModel.locationViewModel.setCurrentEnumerationItem( enumerationItem )
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
                    didSelectItem( location.enumerationItems[0])
                }
            }
        }
    }

    override fun onZoomLevelChanged( zoomLevel: Double )
    {
        sharedViewModel.setCurrentZoomLevel( zoomLevel )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        fusedLocationClient.removeLocationUpdates( locationCallback )

        _binding = null
    }
}