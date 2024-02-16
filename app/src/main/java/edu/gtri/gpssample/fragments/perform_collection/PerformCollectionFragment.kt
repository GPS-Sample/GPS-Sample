package edu.gtri.gpssample.fragments.perform_collection

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.model.*
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformCollectionBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class PerformCollectionFragment : Fragment(),
    OnCameraChangeListener,
    MapboxManager.MapTileCacheDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate,
    AdditionalInfoDialog.AdditionalInfoDialogDelegate,
    SurveyLaunchNotificationDialog.SurveyLaunchNotificationDialogDelegate
{
    companion object
    {
        const val LaunchSurveyRequest = "LaunchSurveyRequest"
        const val AdditionalInfoRequest = "AdditionalInfoRequest"
    }

    private lateinit var user: User
    private lateinit var enumArea: EnumArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var collectionTeam: CollectionTeam
    private lateinit var defaultColorList : ColorStateList
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var performCollectionAdapter: PerformCollectionAdapter
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager

    private val binding get() = _binding!!
    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: Point? = null
    private var busyIndicatorDialog: BusyIndicatorDialog? = null
    private var _binding: FragmentPerformCollectionBinding? = null
    private val locationHashMap = java.util.HashMap<Long, Location>()
    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()
    private var allPolygonAnnotations = java.util.ArrayList<PolygonAnnotation>()
    private var allPolylineAnnotations = java.util.ArrayList<PolylineAnnotation>()

    private val kExportTag = 2
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setFragmentResultListener( AdditionalInfoRequest ) { key, bundle ->
            AdditionalInfoDialog( activity, "", "", this)
        }

        setFragmentResultListener( LaunchSurveyRequest ) { key, bundle ->
            sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
                if (gpsAccuracyIsGood() && gpsLocationIsGood( location ))
                {
                    SurveyLaunchNotificationDialog( activity!!, this )
                }
//                else
//                {
//                    LaunchSurveyDialog( activity, gpsAccuracyIsGood() && gpsLocationIsGood( location ), this@PerformCollectionFragment)
//                }
            }
        }

        val vm : ConfigurationViewModel by activityViewModels()
        val networkVm : NetworkViewModel by activityViewModels()
        sharedViewModel = vm
        sharedNetworkViewModel = networkVm
        sharedNetworkViewModel.currentFragment = this

        val samplingVm : SamplingViewModel by activityViewModels()
        samplingViewModel = samplingVm
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentPerformCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.teamViewModel.currentCollectionTeam?.value?.let {
            collectionTeam = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        val _user = (activity!!.application as? MainApplication)?.user

        _user?.let { user ->
            this.user = user
        }

        val currentZoomLevel = sharedViewModel.currentZoomLevel?.value
        if (currentZoomLevel == null)
        {
            sharedViewModel.setCurrentZoomLevel( 14.0 )
        }

        val enumerationItems = ArrayList<EnumerationItem>()

        collectionTeam.locations.map { location ->
            for (enumurationItem in location.enumerationItems)
            {
                if (enumurationItem.samplingState == SamplingState.Sampled)
                {
                    // FIX THIS!!!
                    // why is the locationId NOT set?
                    if (enumurationItem.locationId < 0)
                    {
                        location.id?.let {
                            enumurationItem.locationId = it
                        }
                    }
                    enumerationItems.add( enumurationItem )
                }
            }
        }

        performCollectionAdapter = PerformCollectionAdapter( enumerationItems, enumArea.name )
        performCollectionAdapter.didSelectEnumerationItem = this::didSelectEnumerationItem

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performCollectionAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enumArea ->
            binding.titleTextView.text =  "Configuration " + enumArea.name + " (" + collectionTeam.name + " team)"
        }

        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    initLocationComponent()
                    refreshMap()
                }
            }
        )

        binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));

        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager(binding.mapView)
        polygonAnnotationManager = binding.mapView.annotations.createPolygonAnnotationManager()
        polylineAnnotationManager = binding.mapView.annotations.createPolylineAnnotationManager()
        mapboxManager = MapboxManager( activity!!, pointAnnotationManager, polygonAnnotationManager, polylineAnnotationManager )

        binding.mapView.getMapboxMap().addOnCameraChangeListener( this )

        pointAnnotationManager.apply {
            addClickListener(
                OnPointAnnotationClickListener { pointAnnotation ->
                    locationHashMap[pointAnnotation.id]?.let { location ->
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
                                bundle.putBoolean( Keys.kGpsAccuracyIsGood.toString(), gpsAccuracyIsGood())
                                bundle.putBoolean( Keys.kGpsLocationIsGood.toString(), gpsLocationIsGood( location ))

                                findNavController().navigate(R.id.action_navigate_to_PerformMultiCollectionFragment, bundle)
                            }
                            else
                            {
                                sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
                                    (this@PerformCollectionFragment.activity!!.application as? MainApplication)?.currentEnumerationItemUUID = location.enumerationItems[0].uuid
                                    (this@PerformCollectionFragment.activity!!.application as? MainApplication)?.currentEnumerationAreaName = enumArea.name
                                    (this@PerformCollectionFragment.activity!!.application as? MainApplication)?.currentSubAddress = location.enumerationItems[0].subAddress

                                    val bundle = Bundle()
                                    bundle.putBoolean( Keys.kEditMode.toString(), false )
                                    bundle.putBoolean( Keys.kCollectionMode.toString(), true )
                                    findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)

//                                    LaunchSurveyDialog( activity, gpsAccuracyIsGood() && gpsLocationIsGood( location ), this@PerformCollectionFragment)
                                }
                            }
                        }
                    }
                    true
                }
            )
        }

        binding.mapTileCacheButton.setOnClickListener {
            sharedViewModel.currentConfiguration?.value?.let { config ->
                if (config.mapTileRegions.isNotEmpty())
                {
                    busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.downloading_map_tiles), this )
                    MapboxManager.loadStylePack( activity!!, this )
                }
            }
        }

        binding.legendTextView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        val centerOnCurrentLocation = sharedViewModel.centerOnCurrentLocation?.value
        if (centerOnCurrentLocation == null)
        {
            sharedViewModel.setCenterOnCurrentLocation( false )
        }

        binding.mapTileCacheButton.backgroundTintList?.let {
            defaultColorList = it
        }

        sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
            if (centerOnCurrentLocation)
            {
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.helpButton.setOnClickListener {
            PerformCollectionHelpDialog( activity!! )
        }

        binding.centerOnLocationButton.setOnClickListener {
            sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
                if (centerOnCurrentLocation)
                {
                    sharedViewModel.setCenterOnCurrentLocation( false )
                    binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                }
                else
                {
                    sharedViewModel.setCenterOnCurrentLocation( true )
                    binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                }
                refreshMap()
            }
        }

        binding.exportButton.setOnClickListener {
            when(user.role)
            {
                Role.Supervisor.toString(), Role.Admin.toString() ->
                {
                    ConfirmationDialog( activity, resources.getString(R.string.export_configuration) , resources.getString(R.string.select_export_message), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kExportTag, this)
                }
                Role.Enumerator.toString(), Role.DataCollector.toString() ->
                {
                    ConfirmationDialog( activity, resources.getString(R.string.export_collection_data), resources.getString(R.string.select_export_message), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kExportTag, this)
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

        for (location in enumArea.locations)
        {
            for (enumItem in location.enumerationItems)
            {
                if (enumItem.enumerationState == EnumerationState.Enumerated)
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

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun refreshMap()
    {
        for (polygonAnnotation in allPolygonAnnotations)
        {
            polygonAnnotationManager.delete( polygonAnnotation )
        }

        allPolygonAnnotations.clear()

        for (polylineAnnotation in allPolylineAnnotations)
        {
            polylineAnnotationManager.delete( polylineAnnotation )
        }

        allPolylineAnnotations.clear()

        for (pointAnnotation in allPointAnnotations)
        {
            pointAnnotationManager.delete( pointAnnotation )
        }

        allPointAnnotations.clear()
        locationHashMap.clear()

        sharedViewModel.currentConfiguration?.value?.let { config ->
            for (enumArea in config.enumAreas)
            {
                for (location in enumArea.locations)
                {
                    if (location.isLandmark)
                    {
                        val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                        val pointAnnotation = mapboxManager.addMarker( point, R.drawable.location_blue )

                        pointAnnotation?.let {
                            locationHashMap[pointAnnotation.id] = location
                            allPointAnnotations.add( pointAnnotation )
                        }
                    }
                }
            }
        }

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        collectionTeam.polygon.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty() && pointList[0].isNotEmpty())
        {
            val polygonAnnotation = mapboxManager.addPolygon(pointList,"#000000", 0.25)

            polygonAnnotation?.let {
                allPolygonAnnotations.add( it )
            }

            val polylineAnnotation = mapboxManager.addPolyline(pointList[0],"#ff0000")

            polylineAnnotation?.let {
                allPolylineAnnotations.add( it )
            }

            val currentZoomLevel = sharedViewModel.currentZoomLevel?.value
            currentZoomLevel?.let { currentZoomLevel ->
                val latLngBounds = GeoUtils.findGeobounds(collectionTeam.polygon)
                val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
                val cameraPosition = CameraOptions.Builder()
                    .zoom(currentZoomLevel)
                    .center(point)
                    .build()

                binding.mapView.getMapboxMap().setCamera(cameraPosition)
            }

            for (location in collectionTeam.locations)
            {
                if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                {
                    var resourceId = 0
                    var isMultiFamily = false

                    location.isMultiFamily?.let {
                        isMultiFamily = it
                    }

                    if (!isMultiFamily)
                    {
                        val sampledItem = location.enumerationItems[0]

                        if (sampledItem.samplingState == SamplingState.Sampled)
                        {
                            resourceId = if (sampledItem.collectionState == CollectionState.Incomplete) R.drawable.home_orange else R.drawable.home_purple
                        }
                    }
                    else
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

                    if (resourceId > 0)
                    {
                        // one point per location!
                        val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                        val pointAnnotation = mapboxManager.addMarker( point, resourceId )

                        pointAnnotation?.let { pointAnnotation ->
                            allPointAnnotations.add( pointAnnotation )
                            locationHashMap[pointAnnotation.id] = location
                        }
                    }
                }
            }
        }
    }

    private fun didSelectEnumerationItem( enumerationItem: EnumerationItem )
    {
        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
            DAO.locationDAO.getLocation( enumerationItem.locationId )?.let { location ->
                sharedViewModel.locationViewModel.setCurrentLocation(location)
                sharedViewModel.locationViewModel.setCurrentEnumerationItem(enumerationItem)
                (this.activity!!.application as? MainApplication)?.currentEnumerationItemUUID = enumerationItem.uuid
                (this.activity!!.application as? MainApplication)?.currentEnumerationAreaName = enumArea.name
                (this.activity!!.application as? MainApplication)?.currentSubAddress = enumerationItem.subAddress

                val bundle = Bundle()
                bundle.putBoolean( Keys.kEditMode.toString(), false )
                bundle.putBoolean( Keys.kCollectionMode.toString(), true )
                findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)

//                LaunchSurveyDialog( activity, gpsAccuracyIsGood() && gpsLocationIsGood( location ), this)
            }
        }
    }

    override fun didSelectRightButton(tag: Any?)
    {
        var payload: String = ""
        var message: String = ""
        var fileName: String = ""

        val user = (activity!!.application as MainApplication).user

        var userName = user!!.name.replace(" ", "" ).uppercase()

        if (userName.length > 4)
        {
            userName = userName.substring(0,4)
        }

        val role = user.role.toString().substring(0,2).uppercase()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
        val dateTime = LocalDateTime.now().format(formatter)

        when(user.role) {
            Role.Admin.toString(),
            Role.Supervisor.toString() ->
            {
                sharedViewModel.currentConfiguration?.value?.let { config ->

                    fileName = "C-${role}-${userName}-${dateTime!!}.json"
                    payload = config.pack()
                    message = resources.getString(R.string.config_saved_doc)
                }
            }

            Role.Enumerator.toString(),
            Role.DataCollector.toString() ->
            {
                sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enumArea ->
                    val clusterName = enumArea.name.replace(" ", "" ).uppercase()
                    fileName = "D-${role}-${userName}-${clusterName}-${dateTime!!}.json"
                    payload = enumArea.pack()
                    message = resources.getString(R.string.collection_saved_doc)
                }
            }
        }

        val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS + "/GPSSample")
        root.mkdirs()
        val file = File(root, fileName)
        val writer = FileWriter(file)
        writer.append(payload)
        writer.flush()
        writer.close()

        Toast.makeText(activity!!.applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectLeftButton(tag: Any?)
    {
        // Launch connection screen
        view?.let{ view ->
            sharedViewModel.currentConfiguration?.value?.let { config ->
                sharedNetworkViewModel.setCurrentConfig(config)
            }

            when(user.role)
            {
                Role.Supervisor.toString() ->
                {
                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Supervisor)
                    startHotspot(view)
                }

                Role.Admin.toString() ->
                {
                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Admin)
                    startHotspot(view)
                }

                Role.Enumerator.toString(),
                Role.DataCollector.toString() ->
                {
                    sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enumArea ->
                        sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.CollectionTeam)
                        sharedNetworkViewModel.networkClientModel.currentEnumArea = enumArea
                        val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                        getResult.launch(intent)
                    }
                }
                else -> {}
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResultCode.BarcodeScanned.value) {
                val payload = it.data!!.getStringExtra(Keys.kPayload.toString())

                val jsonObject = JSONObject(payload);

                Log.d("xxx", jsonObject.toString(2))

                val ssid = jsonObject.getString(Keys.kSSID.toString())
                val pass = jsonObject.getString(Keys.kPass.toString())
                val serverIp = jsonObject.getString(Keys.kIpAddress.toString())

                Log.d("xxxx", "the ssid, pass, serverIP ${ssid}, ${pass}, ${serverIp}")

                sharedNetworkViewModel.connectHotspot(ssid, pass, serverIp)
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startHotspot(view : View)
    {
        sharedNetworkViewModel.createHotspot(view)
    }

//    override fun launchSurveyButtonPressed()
//    {
//        SurveyLaunchNotificationDialog( activity!!, this )
//    }

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
        val mainApplication = activity!!.application as MainApplication

        mainApplication.currentSubAddress = mainApplication.defaultSubAddress
        mainApplication.currentEnumerationItemUUID = mainApplication.defaultEnumerationItemUUID
        mainApplication.currentEnumerationAreaName = mainApplication.defaultEnumerationAreaName

        AdditionalInfoDialog( activity, "", "", this)
    }

//    override fun markAsIncompleteButtonPressed()
//    {
//        AdditionalInfoDialog( activity, "", "", this)
//    }
//
//    override fun showInfoButtonPressed()
//    {
//        val bundle = Bundle()
//        bundle.putBoolean( Keys.kEditMode.toString(), false )
//        bundle.putBoolean( Keys.kCollectionMode.toString(), true )
//        findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)
//    }

    override fun didSelectCancelButton()
    {
    }

    override fun didSelectSaveButton( incompleteReason: String, notes: String )
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->

            sharedViewModel.locationViewModel.currentEnumerationItem?.value?.let { sampledItem ->

                sampledItem.collectionNotes = notes
                sampledItem.modificationDate = DAO.updateCreationDate( sampledItem.modificationDate )
                sampledItem.collectionDate = sampledItem.modificationDate
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

                sharedViewModel.currentConfiguration?.value?.let { config ->
                    DAO.configDAO.getConfig( config.id!! )?.let {
                        sharedViewModel.setCurrentConfig( it )
                    }
                }

                sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
                    DAO.enumAreaDAO.getEnumArea( enumArea.id!! )?.let {
                        sharedViewModel.enumAreaViewModel.setCurrentEnumArea( it )
                    }
                }

                sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                    DAO.studyDAO.getStudy( study.id!! )?.let {
                        sharedViewModel.createStudyModel.setStudy( it )
                    }
                }

                performCollectionAdapter.notifyDataSetChanged()

                updateSummaryInfo()

                refreshMap()
            }
        }
    }

    override fun onCameraChanged(eventData: CameraChangedEventData)
    {
        sharedViewModel.setCurrentZoomLevel( binding.mapView.getMapboxMap().cameraState.zoom )
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        sharedViewModel.centerOnCurrentLocation?.value?.let {
            if (it)
            {
                binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(point).build())
                binding.mapView.gestures.focalPoint = binding.mapView.getMapboxMap().pixelForCoordinate(point)
            }
        }
    }

    private val locationConsumer = object : LocationConsumer
    {
        override fun onBearingUpdated(vararg bearing: Double, options: (ValueAnimator.() -> Unit)?) {
        }

        override fun onLocationUpdated(vararg location: Point, options: (ValueAnimator.() -> Unit)?) {
            if (location.size > 0)
            {
                val point = location.last()
                binding.locationTextView.text = String.format( "%.7f, %.7f", point.latitude(), point.longitude())
                currentGPSLocation = point
            }
        }

        override fun onPuckBearingAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) {
        }

        override fun onPuckLocationAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) {
        }
    }

    private val onIndicatorAccurracyRadiusChangedListener = OnIndicatorAccuracyRadiusChangedListener {
        val accuracy = it.toInt()
        currentGPSAccuracy = accuracy

        sharedViewModel.currentConfiguration?.value?.let { config ->
            if (accuracy <= config.minGpsPrecision)
            {
                binding.accuracyLabelTextView.text = resources.getString(R.string.good)
                binding.accuracyLabelTextView.setTextColor( Color.parseColor("#0000ff"))
            }
            else
            {
                binding.accuracyLabelTextView.text = resources.getString(R.string.poor)
                binding.accuracyLabelTextView.setTextColor( Color.parseColor("#ff0000") )
            }
        }

        binding.accuracyValueTextView.text = " : ${accuracy.toString()}m"
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

    private fun initLocationComponent()
    {
        val locationComponentPlugin = binding.mapView.location

        locationComponentPlugin.enabled = true
        locationComponentPlugin.getLocationProvider()?.registerLocationConsumer( locationConsumer )
        locationComponentPlugin.addOnIndicatorPositionChangedListener( onIndicatorPositionChangedListener )

        val locationComponentPlugin2 = binding.mapView.location2
        locationComponentPlugin2.enabled = true
        locationComponentPlugin2.addOnIndicatorAccuracyRadiusChangedListener( onIndicatorAccurracyRadiusChangedListener )

        locationComponentPlugin2.updateSettings2 {
            this.showAccuracyRing = true
        }

        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    activity!!,
                    com.mapbox.maps.R.drawable.mapbox_user_puck_icon,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    activity!!,
                    com.mapbox.maps.R.drawable.mapbox_user_icon_shadow,
                ),
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
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
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    MapboxManager.loadTilePacks( activity!!, config.mapTileRegions, this )
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


    override fun onDestroyView()
    {
        super.onDestroyView()

        binding.mapView.location.getLocationProvider()?.unRegisterLocationConsumer( locationConsumer )
        binding.mapView.location.removeOnIndicatorPositionChangedListener( onIndicatorPositionChangedListener )
        _binding = null
    }
}