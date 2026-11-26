/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.create_sample

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.SamplingMethod
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateSampleBinding
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.MapEngine
import edu.gtri.gpssample.constants.ReviewStatus
import edu.gtri.gpssample.fragments.perform_enumeration.LocationService
import edu.gtri.gpssample.fragments.perform_enumeration.PerformEnumerationAdapter
import edu.gtri.gpssample.fragments.perform_enumeration.PerformEnumerationFragment.BreadcrumbRecordingState
import edu.gtri.gpssample.fragments.perform_sample.CreateSampleAdapter
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableInputDialogHost
import edu.gtri.gpssample.ui.compose.ComposableMapLegendDialogHost

class CreateSampleFragment : Fragment()
{
    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var createSampleAdapter: CreateSampleAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var mapboxMapClickListener: OnMapClickListener
    private lateinit var composableInputDialogHost: ComposableInputDialogHost
    private lateinit var composableMapLegendDialogHost: ComposableMapLegendDialogHost
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost
    private var mapView: View? = null
    private var sampledLocations = ArrayList<Location>()
    private var duplicateLocations = ArrayList<Location>()
    private var geofenceViolations = ArrayList<Location>()
    private var isHandlingTapEvent = false

    var _binding: FragmentCreateSampleBinding? = null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        val samplingVm : SamplingViewModel by activityViewModels()

        sharedViewModel = vm

        samplingViewModel = samplingVm
        samplingViewModel.currentConfig = sharedViewModel.currentConfiguration
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy
        samplingViewModel.currentEnumArea = sharedViewModel.enumAreaViewModel.currentEnumArea

        samplingViewModel.setSampleState( SamplingViewModel.SampleState.Idle )
        samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.PreviewPage )

        setHasOptionsMenu(true)
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateSampleBinding.inflate(inflater, container, false)
        binding.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel
            this.sampleViewModel = samplingViewModel

            // Assign the fragment
            createSampleFragment = this@CreateSampleFragment
        }

        composableInputDialogHost = ComposableInputDialogHost()
        composableMapLegendDialogHost = ComposableMapLegendDialogHost()
        composableConfirmationDialogHost = ComposableConfirmationDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableInputDialogHost.Content()
            composableMapLegendDialogHost.Content()
            composableConfirmationDialogHost.Content()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        mapboxMapClickListener = MapManager.instance().createMapboxMapClickListener( binding.mapboxMapView, true )

        sharedViewModel.currentConfiguration?.value?.let { config ->
            this.config = config
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
            this.study = study
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
            this.enumArea = enumArea
        }

        if (config.mapEngineIndex == MapEngine.OpenStreetMap.value)
        {
            binding.osmMapView.visibility = View.VISIBLE
            binding.mapboxMapView.visibility = View.GONE
            MapManager.instance().centerMap(enumArea.vertices, binding.osmMapView )
        }
        else
        {
            binding.osmMapView.visibility = View.GONE
            binding.mapboxMapView.visibility = View.VISIBLE
            MapManager.instance().centerMap(enumArea.vertices, binding.mapboxMapView )
        }

        if (study.samplingMethod == SamplingMethod.Structure)
        {
            if (enumArea.locations.firstOrNull()?.enumerationItems.isNullOrEmpty())
            {
                var i = 0

                for (location in enumArea.locations)
                {
                    i += 1
                    val enumerationItem = EnumerationItem()
                    enumerationItem.subAddress = i.toString()
                    enumerationItem.enumerationState = EnumerationState.Enumerated
                    location.enumerationItems.add( enumerationItem )
                    DAO.locationDAO.createOrUpdateLocation( location, enumArea, location.version )
                }
            }
        }

        binding.progressOverlayView.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (study.samplingMethod == SamplingMethod.SimpleRandom)
                {
                    config.enumAreas = DAO.enumAreaDAO.getEnumAreas( config )
                }

                getNumDuplicates( config.enumAreas )
                getNumGeofenceViolations( config.enumAreas )
            }

            // back on the main thread...
            binding.progressOverlayView.visibility = View.GONE
        }

        binding.legendTextView.setOnClickListener {
            composableMapLegendDialogHost.show()
        }

        binding.legendImageView.setOnClickListener {
            composableMapLegendDialogHost.show()
        }

        when(study.samplingMethod)
        {
            SamplingMethod.SimpleRandom-> binding.titleTextView.text = resources.getString(R.string.simple_random)
            SamplingMethod.Cluster -> binding.titleTextView.text = resources.getString(R.string.cluster_sampling)
            SamplingMethod.SelfWeighted -> binding.titleTextView.text = resources.getString(R.string.weighted_sampling)
            SamplingMethod.Strata -> binding.titleTextView.text = resources.getString(R.string.strata_exclusive)
            SamplingMethod.Structure -> binding.titleTextView.text = resources.getString(R.string.structure)
            SamplingMethod.None -> TODO()
        }

        sampledLocations.clear()

        for (location in enumArea.locations)
        {
            if (!location.isLandmark && location.enumerationItems.isNotEmpty())
            {
                if (location.enumerationItems.size == 1)
                {
                    val enumerationItem = location.enumerationItems.first()
                    if (enumerationItem.enumerationState == EnumerationState.Enumerated && enumerationItem.reviewStatus != ReviewStatus.Exclude)
                    {
                        sampledLocations.add( location )
                    }
                }
                else
                {
                    for (enumerationItem in location.enumerationItems)
                    {
                        if (enumerationItem.enumerationState == EnumerationState.Enumerated && enumerationItem.reviewStatus != ReviewStatus.Exclude)
                        {
                            sampledLocations.add( location )
                            break
                        }
                    }
                }
            }
        }

        createSampleAdapter = CreateSampleAdapter( sampledLocations,enumArea.name )
        createSampleAdapter.didSelectLocation = this::didSelectLocation

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = createSampleAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )
        binding.recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );

        if (ActivityCompat.checkSelfPermission( requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission( requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            if (!LocationService.started)
            {
                LocationService.locationCallback = locationCallback
                val intent = Intent(requireActivity(), LocationService::class.java)
                ContextCompat.startForegroundService(requireActivity(), intent )
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

        if (enumArea.mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( enumArea.mbTilesPath )
        }

        MapManager.instance().selectMap( requireActivity(), config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
            this.mapView = mapView

            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

            MapManager.instance().centerMap( enumArea.vertices, mapView )

            val sampleAlreadyGenerated = determineIfSampleWasGenerated()

            if (sampleAlreadyGenerated)
            {
                binding.generateSampleButton.visibility = View.GONE
                samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.SampleGeneratedPage )
                updatePageForState( samplingViewModel.samplePageState.value )
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    if (!sampleAlreadyGenerated)
                    {
                        launch {
                            samplingViewModel.sampleState.collect { state ->
                                when (state)
                                {
                                    SamplingViewModel.SampleState.Idle -> {}

                                    SamplingViewModel.SampleState.SampleGenerated -> {
                                        if (study.samplingMethod == SamplingMethod.Structure)
                                        {
                                            // create a list of sampledItems

                                            val sampledItems = ArrayList<EnumerationItem>()

                                            for (location in enumArea.locations)
                                            {
                                                val enumerationItem = location.enumerationItems.firstOrNull() ?: continue

                                                if (enumerationItem.samplingState == SamplingState.Sampled)
                                                {
                                                    sampledItems.add( enumerationItem )
                                                }
                                            }

                                            val validSamples = sampledItems.shuffled().take(study.sampleSize )

                                            // mark any leftovers as spare

                                            sampledItems.forEach { sampledItem ->
                                                if (validSamples.none { it.uuid == sampledItem.uuid }) {
                                                    sampledItem.samplingState = SamplingState.SampledSpare
                                                }
                                            }
                                        }

                                        saveSample()

                                        binding.progressOverlayView.visibility = View.GONE
                                        samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.SampleGeneratedPage )
                                        refreshMap()
                                    }

                                    SamplingViewModel.SampleState.NoEligibleSamples -> {
                                        binding.progressOverlayView.visibility = View.GONE
                                        Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.no_eligible_households), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }

                        launch {
                            samplingViewModel.samplePageState.collect { state ->
                                updatePageForState( state )
                            }
                        }
                    }

                    val mapManager = MapManager.instance()

                    launch {
                        mapManager.markerTapped.collect { location ->
                            if (!isHandlingTapEvent)
                            {
                                isHandlingTapEvent = true

                                val bundle = Bundle()

                                if ((samplingViewModel.samplePageState.value == SamplingViewModel.SamplePageState.DuplicatePage)
                                    || (samplingViewModel.samplePageState.value == SamplingViewModel.SamplePageState.GeofenceViolationPage))
                                {
                                    bundle.putBoolean( Keys.kEditMode.value, true )
                                }
                                else
                                {
                                    bundle.putBoolean( Keys.kEditMode.value, false )
                                }

                                sharedViewModel.currentLocationUuid = location.uuid
                                sharedViewModel.currentEnumerationItemUuid = location.enumerationItems.first().uuid

                                if (samplingViewModel.samplePageState.value == SamplingViewModel.SamplePageState.DuplicatePage)
                                {
                                    bundle.putBoolean( Keys.kReviewDuplicate.value, true )
                                }
                                else if (samplingViewModel.samplePageState.value == SamplingViewModel.SamplePageState.GeofenceViolationPage)
                                {
                                    bundle.putBoolean( Keys.kReviewFenceViolation.value, true )
                                }

                                if (location.enumerationItems.size > 1)
                                {
                                    findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment,bundle )
                                }
                                else
                                {
                                    findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle )
                                }
                            }
                        }
                    }
                }
            }
        }

        binding.backButton.setOnClickListener {
            handleBackButtonPress()
        }

        binding.generateSampleButton.setOnClickListener {
            if (study.samplingMethod == SamplingMethod.SelfWeighted)
            {
                composableInputDialogHost.show(
                    title = null,
                    description = "Enter the sample weight", //resources.getString(R.string.enter_sample_weight),
                    text = "",
                    keyboardType = KeyboardType.Number,
                    onResult = { text ->
                        text.toIntOrNull()?.let { sampleSize ->
                            if (sampleSize > 0)
                            {
                                study.sampleSize = sampleSize
                                enumArea.sampleSize = sampleSize
                                binding.progressOverlayView.visibility = View.VISIBLE
                                samplingViewModel.beginSampling()
                            }
                        }
                    }
                )
            }
            else
            {
                binding.progressOverlayView.visibility = View.VISIBLE
                samplingViewModel.beginSampling()
            }
        }

        binding.nextButton.setOnClickListener {
            handleNextButtonPress()
        }

        if (GeoUtils.isSelfIntersectingPolygon3( enumArea.vertices))
        {
            var message = resources.getString( R.string.boundary_is_self_intersecting_1_3 ) + "\n\n"
            message += resources.getString( R.string.boundary_is_self_intersecting_2_3 ) + "\n\n"
            message += resources.getString( R.string.boundary_is_self_intersecting_3_3 )

            composableConfirmationDialogHost.show(
                title = resources.getString(R.string.oops),
                message = message,
                leftButtonText = resources.getString(R.string.no),
                rightButtonText = resources.getString(R.string.yes),
                destructive = true
            ) { selection ->
                if (selection == resources.getString(R.string.no ))
                {
                    binding.nextButton.isEnabled = false
                }
                else
                {
                    redefineEnumerationAreaBoundary()
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        isHandlingTapEvent = false
        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.CreateSampleFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun determineIfSampleWasGenerated() : Boolean
    {
        if (study.samplingMethod == SamplingMethod.SimpleRandom)
        {
            for (enumArea in config.enumAreas)
            {
                for (location in enumArea.locations)
                {
                    for (enumerationItem in location.enumerationItems)
                    {
                        if (enumerationItem.samplingState == SamplingState.Sampled)
                        {
                            return true
                        }
                    }
                }
            }
        }
        else
        {
            for (location in enumArea.locations)
            {
                for (enumerationItem in location.enumerationItems)
                {
                    if (enumerationItem.samplingState == SamplingState.Sampled)
                    {
                        return true
                    }
                }
            }
        }

        return false
    }

    fun getNumDuplicates( enumAreas: ArrayList<EnumArea> ) : Int
    {
        duplicateLocations.clear()

        for (enumArea in enumAreas)
        {
            for (location1 in enumArea.locations)
            {
                if (!location1.isLandmark && location1.enumerationItems.isNotEmpty())
                {
                    for (location2 in enumArea.locations)
                    {
                        if (!location2.isLandmark  && location2.enumerationItems.isNotEmpty())
                        {
                            if (location1.uuid == location2.uuid) continue
                            var distance = GeoUtils.distanceBetween( LatLng( location1.latitude, location1.longitude ), LatLng( location2.latitude, location2.longitude ))
                            if (config.distanceFormat == DistanceFormat.Feet)
                            {
                                distance *= 0.3048
                            }
                            if (distance < config.proximityWarningValue)
                            {
                                duplicateLocations.add( location1 )
                            }
                        }
                    }
                }
            }
        }

        return duplicateLocations.count()
    }

    fun getNumGeofenceViolations( enumAreas: ArrayList<EnumArea> ) : Int
    {
        geofenceViolations.clear()

        for (enumArea in enumAreas)
        {
            for (location in enumArea.locations)
            {
                if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                {
                    val point = Point.fromLngLat( location.longitude, location.latitude, 0.0 )
                    var distance = GeoUtils.distance( point, enumArea )

                    if (config.distanceFormat == DistanceFormat.Feet)
                    {
                        distance *= 0.3048
                    }

                    if (distance > config.geofenceBufferValue)
                    {
                        geofenceViolations.add( location )
                    }
                }
            }
        }

        return geofenceViolations.count()
    }

    fun handleBackButtonPress()
    {
        when (samplingViewModel.samplePageState.value)
        {
            SamplingViewModel.SamplePageState.PreviewPage -> {
                findNavController().popBackStack()
            }
            SamplingViewModel.SamplePageState.DuplicatePage -> {
                samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.PreviewPage )
            }
            SamplingViewModel.SamplePageState.GeofenceViolationPage -> {
                if (duplicateLocations.isNotEmpty())
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.DuplicatePage )
                }
                else
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.PreviewPage )
                }
            }
            SamplingViewModel.SamplePageState.GenerateSamplePage -> {
                if (geofenceViolations.isNotEmpty())
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.GeofenceViolationPage )
                }
                else if (duplicateLocations.isNotEmpty())
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.DuplicatePage )
                }
                else
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.PreviewPage )
                }
            }
            SamplingViewModel.SamplePageState.SampleGeneratedPage -> {
                findNavController().popBackStack()
            }
        }
    }

    fun handleNextButtonPress()
    {
        when (samplingViewModel.samplePageState.value)
        {
            SamplingViewModel.SamplePageState.PreviewPage -> {
                if (duplicateLocations.isNotEmpty())
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.DuplicatePage )
                }
                else if (geofenceViolations.isNotEmpty())
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.GeofenceViolationPage )
                }
                else
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.GenerateSamplePage )
                }
            }
            SamplingViewModel.SamplePageState.DuplicatePage -> {
                if (geofenceViolations.isNotEmpty())
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.GeofenceViolationPage )
                }
                else
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.GenerateSamplePage )
                }
            }
            SamplingViewModel.SamplePageState.GeofenceViolationPage -> {
                samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.GenerateSamplePage )
            }
            SamplingViewModel.SamplePageState.GenerateSamplePage -> {
            }
            SamplingViewModel.SamplePageState.SampleGeneratedPage -> {
                findNavController().navigate(R.id.action_navigate_to_ManageCollectionTeamsFragment)
            }
        }
    }

    fun updatePageForState( state: SamplingViewModel.SamplePageState )
    {
        when (state)
        {
            SamplingViewModel.SamplePageState.PreviewPage -> {
                binding.nextButton.isEnabled = true
                binding.titleTextView.text = resources.getString( R.string.review_sample )
                binding.generateSampleButton.visibility = View.GONE
                binding.backButton.text = resources.getString(R.string.back )
                binding.nextButton.text = resources.getString(R.string.next )
                refreshMap()
                createSampleAdapter.updateLocations( sampledLocations )
            }
            SamplingViewModel.SamplePageState.DuplicatePage -> {
                binding.nextButton.isEnabled = true
                binding.titleTextView.text = resources.getString( R.string.review_possible_duplicates ) + " (${duplicateLocations.count()})"
                binding.generateSampleButton.visibility = View.GONE
                binding.backButton.text = resources.getString(R.string.back )
                binding.nextButton.text = resources.getString(R.string.next )
                showMap( duplicateLocations )
                createSampleAdapter.updateLocations( duplicateLocations )
            }
            SamplingViewModel.SamplePageState.GeofenceViolationPage -> {
                binding.nextButton.isEnabled = true
                binding.titleTextView.text = resources.getString( R.string.review_boundary_violations ) + " (${geofenceViolations.count()})"
                binding.generateSampleButton.visibility = View.GONE
                binding.backButton.text = resources.getString(R.string.back )
                binding.nextButton.text = resources.getString(R.string.next )
                showMap(geofenceViolations )
                createSampleAdapter.updateLocations( geofenceViolations )
            }
            SamplingViewModel.SamplePageState.GenerateSamplePage -> {
                binding.nextButton.isEnabled = false
                binding.titleTextView.text = resources.getString( R.string.generate_sample )
                binding.generateSampleButton.visibility = View.VISIBLE
                binding.backButton.text = resources.getString(R.string.back )
                binding.nextButton.text = resources.getString(R.string.next )
                clearSample()
                refreshMap(false )
                createSampleAdapter.updateLocations( sampledLocations )
            }
            SamplingViewModel.SamplePageState.SampleGeneratedPage -> {
                binding.nextButton.isEnabled = true
                binding.titleTextView.text = resources.getString( R.string.save_sample )
                binding.generateSampleButton.visibility = View.GONE
                binding.backButton.visibility = View.GONE
                binding.nextButton.text = resources.getString(R.string.next )
                refreshMap()
                createSampleAdapter.updateLocations( sampledLocations )
            }
        }
    }

    fun clearSample()
    {
        for (ea in config.enumAreas)
        {
            if (study.samplingMethod == SamplingMethod.SimpleRandom || ea.uuid == enumArea.uuid)
            {
                for (location in ea.locations)
                {
                    for (enumerationItem in location.enumerationItems)
                    {
                        enumerationItem.samplingState = SamplingState.NotSampled
                    }
                }
            }
        }
    }

    fun saveSample()
    {
        binding.progressOverlayView.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                DAO.instance().writableDatabase.beginTransaction()
                try
                {
                    if (study.samplingMethod == SamplingMethod.SimpleRandom)
                    {
                        for (enumArea in config.enumAreas)
                        {
                            DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea,enumArea.version )
                        }
                    }
                    else
                    {
                        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
                            DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea, enumArea.version )
                        }
                    }

                    DAO.instance().writableDatabase.setTransactionSuccessful()
                }
                finally
                {
                    DAO.instance().writableDatabase.endTransaction()
                }
            }

            // back on the main thread...
            binding.progressOverlayView.visibility = View.GONE
        }
    }

    fun refreshMap( showExclusions: Boolean = true )
    {
        MapManager.instance().clearMap( mapView!! )

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty())
        {
            MapManager.instance().createPolygon( mapView!!, pointList, Color.BLACK, 0x40 )
        }

        val markerProperties = ArrayList<MapManager.MarkerProperty>()

        for (enumArea in config.enumAreas)
        {
            if (study.samplingMethod != SamplingMethod.SimpleRandom)
            {
                if (enumArea.uuid != config.selectedEnumAreaUuid)
                {
                    continue
                }
            }

            for (location in enumArea.locations)
            {
                if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                {
                    if (location.enumerationItems.first().reviewStatus == ReviewStatus.Exclude && !showExclusions)
                    {
                        continue
                    }

                    val currentPage = samplingViewModel.samplePageState.value
                    val generateSamplePage = SamplingViewModel.SamplePageState.GenerateSamplePage
                    val sampleGeneratedPage = SamplingViewModel.SamplePageState.SampleGeneratedPage

                    if (location.enumerationItems.first().reviewStatus == ReviewStatus.Exclude && (currentPage == generateSamplePage || currentPage == sampleGeneratedPage))
                    {
                        continue
                    }

                    var resourceId: Int
                    val title = location.enumerationItems[0].subAddress

                    if (location.enumerationItems.size == 1)
                    {
                        val sampledItem = location.enumerationItems[0]

                        if (sampledItem.enumerationState == EnumerationState.Enumerated)
                        {
                            resourceId = R.drawable.home_green

                            if (sampledItem.samplingState == SamplingState.Sampled || sampledItem.subsetSamplingState == SamplingState.Sampled)
                            {
                                resourceId = R.drawable.home_light_blue
                            }

                            markerProperties.add( MapManager.MarkerProperty( location, resourceId, title ))
                        }
                    }
                    else
                    {
                        resourceId = R.drawable.multi_home_green

                        for (sampledItem in location.enumerationItems)
                        {
                            if (sampledItem.samplingState == SamplingState.Sampled || sampledItem.subsetSamplingState == SamplingState.Sampled)
                            {
                                resourceId = R.drawable.multi_home_light_blue
                            }
                        }

                        markerProperties.add( MapManager.MarkerProperty( location, resourceId, title ))
                    }
                }
            }
        }

        if (markerProperties.isNotEmpty())
        {
            binding.mapboxMapView.gestures.removeOnMapClickListener(mapboxMapClickListener )

            MapManager.instance().loadMarkers( requireActivity(), mapView!!, markerProperties, null )
        }
    }

    fun showMap( locations: ArrayList<Location> )
    {
        MapManager.instance().clearMap( mapView!! )

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty())
        {
            MapManager.instance().createPolygon( mapView!!, pointList, Color.BLACK, 0x40 )
        }

        val markerProperties = ArrayList<MapManager.MarkerProperty>()

        for (enumArea in config.enumAreas)
        {
            if (study.samplingMethod != SamplingMethod.SimpleRandom)
            {
                if (enumArea.uuid != config.selectedEnumAreaUuid)
                {
                    continue
                }
            }

            for (location in locations)
            {
                if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                {
                    var resourceId: Int
                    val title = location.enumerationItems[0].subAddress

                    if (location.enumerationItems.size == 1)
                    {
                        val sampledItem = location.enumerationItems[0]

                        if (sampledItem.enumerationState == EnumerationState.Enumerated)
                        {
                            resourceId = R.drawable.home_green

                            if (sampledItem.reviewStatus == ReviewStatus.Exclude)
                            {
                                resourceId = R.drawable.home_red
                            }
                            else if (sampledItem.reviewStatus == ReviewStatus.Keep)
                            {
                                resourceId = R.drawable.home_purple
                            }

                            markerProperties.add( MapManager.MarkerProperty( location, resourceId, title ))
                        }
                    }
                    else
                    {
                        resourceId = R.drawable.multi_home_green

                        for (sampledItem in location.enumerationItems)
                        {
                            if (sampledItem.reviewStatus == ReviewStatus.Exclude)
                            {
                                resourceId = R.drawable.multi_home_red
                                break
                            }
                            else if (sampledItem.reviewStatus == ReviewStatus.Keep)
                            {
                                resourceId = R.drawable.multi_home_purple
                                break
                            }
                        }

                        markerProperties.add( MapManager.MarkerProperty( location, resourceId, title ))
                    }
                }
            }
        }

        if (markerProperties.isNotEmpty())
        {
            MapManager.instance().loadMarkers( requireActivity(), mapView!!, markerProperties, mapboxMapClickListener )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_redefine_ea_boundary, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            R.id.redefine_ea_boundary -> redefineEnumerationAreaBoundary()

            R.id.mapbox_streets ->
            {
                val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                editor.commit()

                MapManager.instance().selectMap( requireActivity(), config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                MapManager.instance().selectMap( requireActivity(), config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
                    refreshMap()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun redefineEnumerationAreaBoundary()
    {
        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
            val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
            var creationDate = Date().time

            val northWest = LatLon( creationDate++, latLngBounds.northeast.latitude, latLngBounds.southwest.longitude )
            val northEast = LatLon( creationDate++, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
            val southEast = LatLon( creationDate++,latLngBounds.southwest.latitude, latLngBounds.northeast.longitude )
            val southWest = LatLon( creationDate++, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )
            val northWest2 = LatLon( creationDate++, latLngBounds.northeast.latitude, latLngBounds.southwest.longitude )

            for (vertice in enumArea.vertices)
            {
                DAO.latLonDAO.delete(vertice)
            }

            enumArea.vertices.clear()

            enumArea.vertices.add( northWest )
            enumArea.vertices.add( northEast )
            enumArea.vertices.add( southEast )
            enumArea.vertices.add( southWest )
            enumArea.vertices.add( northWest2 )

            DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea, UUID.randomUUID().toString())

            refreshMap()
        }
    }

    private var lastLocationUpdateTime: Long = 0

    private val locationCallback = object : LocationCallback()
    {
        override fun onLocationResult(locationResult: LocationResult)
        {
            if (_binding == null)
            {
                return
            }

            val location = locationResult.locations.last()
            val altitude = if (location.altitude.isNaN()) 0.0 else location.altitude
            val point = Point.fromLngLat( location.longitude, location.latitude, altitude )

            sharedViewModel.currentConfiguration?.value?.let { config ->
                if (Date().time - lastLocationUpdateTime > 3000)
                {
                    lastLocationUpdateTime = Date().time

                    for (location in createSampleAdapter.locations)
                    {
                        val currentLatLng = LatLng( point.latitude(), point.longitude())
                        val itemLatLng = LatLng( location.latitude, location.longitude )
                        val distance = GeoUtils.distanceBetween( currentLatLng, itemLatLng )
                        var (distanceValue, distanceUnits) = formatDistance( config, distance )
                        location.distance = distanceValue
                        location.distanceUnits = distanceUnits
                    }

                    createSampleAdapter.updateLocations( createSampleAdapter.locations )
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

    private fun didSelectLocation( location: Location )
    {
        val bundle = Bundle()
        bundle.putBoolean( Keys.kEditMode.value, false)

        if (samplingViewModel.samplePageState.value == SamplingViewModel.SamplePageState.DuplicatePage)
        {
            bundle.putBoolean( Keys.kReviewDuplicate.value, true )
        }
        else if (samplingViewModel.samplePageState.value == SamplingViewModel.SamplePageState.GeofenceViolationPage)
        {
            bundle.putBoolean( Keys.kReviewFenceViolation.value, true )
        }

        sharedViewModel.currentLocationUuid = location.uuid

        if (location.enumerationItems.size == 1)
        {
            sharedViewModel.currentEnumerationItemUuid = location.enumerationItems[0].uuid
            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment, bundle)
        }
        else
        {
            findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment, bundle)
        }
    }

    override fun onDestroyView()
    {
        binding.mapboxMapView.gestures.removeOnMapClickListener(mapboxMapClickListener )

        if (LocationService.started)
        {
            val intent = Intent(requireActivity(), LocationService::class.java)
            requireActivity().stopService( intent )
        }

        mapView = null
        _binding = null

        super.onDestroyView()
    }
}