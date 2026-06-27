/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.createsample

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
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
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.MapLegendDialog
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.model.LatLng
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.MapEngine
import edu.gtri.gpssample.dialogs.AdditionalInfoDialog
import edu.gtri.gpssample.dialogs.ExcludeLocationDialog
import org.osmdroid.events.MapListener

class CreateSampleFragment : Fragment()
{
    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel : ConfigurationViewModel
    private var mapView: View? = null
    private var sampleHasDuplicates = false
    private var sampleHasGeofenceViolations = false
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
        sharedViewModel.currentFragment = this

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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.currentConfiguration?.value?.let { config ->
            this.config = config
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
            this.study = study
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
            this.enumArea = enumArea
        }

        sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
            if (config.mapEngineIndex == MapEngine.OpenStreetMap.value)
            {
                binding.osmMapView.visibility = View.VISIBLE
                binding.mapboxMapView.visibility = View.GONE
                MapManager.instance().centerMap(enumArea.vertices, currentZoomLevel, binding.osmMapView )
            }
            else
            {
                binding.osmMapView.visibility = View.GONE
                binding.mapboxMapView.visibility = View.VISIBLE
                MapManager.instance().centerMap(enumArea.vertices, currentZoomLevel, binding.mapboxMapView )
            }
        }

        binding.progressOverlayView.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (study.samplingMethod == SamplingMethod.SimpleRandom)
                {
                    config.enumAreas = DAO.enumAreaDAO.getEnumAreas( config )
                }
            }

            sampleHasDuplicates = doesSampleHaveDuplicates(config.enumAreas )
            sampleHasGeofenceViolations = doesSampleHaveGeofenceViolations(config.enumAreas )

            // back on the main thread...
            binding.progressOverlayView.visibility = View.GONE
        }

        binding.legendTextView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.legendImageView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        when(study.samplingMethod)
        {
            SamplingMethod.SimpleRandom-> binding.titleTextView.text = resources.getString(R.string.simple_random)
            SamplingMethod.Cluster -> binding.titleTextView.text = resources.getString(R.string.cluster_sampling)
            SamplingMethod.Subsets -> binding.titleTextView.text = resources.getString(R.string.subset_overlap)
            SamplingMethod.Strata -> binding.titleTextView.text = resources.getString(R.string.strata_exclusive)
            SamplingMethod.None -> TODO()
        }

        if (enumArea.mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( enumArea.mbTilesPath )
        }

        val sampleAlreadyGenerated = determineIfSampleWasGenerated()

        if (sampleAlreadyGenerated)
        {
            binding.generateSampleButton.visibility = View.GONE
            binding.beginReviewTextView.visibility = View.GONE
            samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.SampleGeneratedPage )
        }

        val zoom = sharedViewModel.currentZoomLevel?.value ?: 0.0

        MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea, zoom ) { mapView ->
            this@CreateSampleFragment.mapView = mapView

            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

            sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                MapManager.instance().centerMap( enumArea.vertices, currentZoomLevel, mapView )
            }


            samplingViewModel.refreshMap.observe(viewLifecycleOwner)
            {
                refreshMap()
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
                                        binding.progressOverlayView.visibility = View.GONE
                                        samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.SaveSamplePage )
                                        refreshMap()
                                    }

                                    SamplingViewModel.SampleState.NoEligibleSamples -> {
                                        binding.progressOverlayView.visibility = View.GONE
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
                        mapManager.zoomLevel.collect { zoomLevel ->
                            sharedViewModel.setCurrentZoomLevel(zoomLevel)
                        }
                    }

                    launch {
                        mapManager.markerTapped.collect { location ->
                            if (!isHandlingTapEvent)
                            {
                                if ((samplingViewModel.samplePageState.value == SamplingViewModel.SamplePageState.DuplicatePage)
                                    || (samplingViewModel.samplePageState.value == SamplingViewModel.SamplePageState.GeofenceViolationPage))
                                {
                                    sharedViewModel.currentLocationUuid = location.uuid
                                    sharedViewModel.currentEnumerationItemUuid = location.enumerationItems.first().uuid

                                    isHandlingTapEvent = true

                                    ExcludeLocationDialog(requireContext(),location.enumerationItems ) { buttonPress ->
                                        isHandlingTapEvent = false

                                        when (buttonPress)
                                        {
                                            ExcludeLocationDialog.ButtonPress.Cancel -> {
                                            }
                                            ExcludeLocationDialog.ButtonPress.Save -> {
                                                for (enumerationItem in location.enumerationItems)
                                                {
                                                    DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem,enumerationItem.version )
                                                }

                                                if (samplingViewModel.samplePageState.value == SamplingViewModel.SamplePageState.DuplicatePage)
                                                {
                                                    showMap( duplicateLocations )
                                                }
                                                else if (samplingViewModel.samplePageState.value == SamplingViewModel.SamplePageState.GeofenceViolationPage)
                                                {
                                                    showMap(geofenceViolations )
                                                }
                                            }
                                            ExcludeLocationDialog.ButtonPress.Info -> {
                                                val bundle = Bundle()
                                                bundle.putBoolean( Keys.kEditMode.value, false )
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
                    }
                }
            }

            refreshMap()
        }

        binding.infoButton.setOnClickListener{
            findNavController().navigate(R.id.action_navigate_to_SamplingInfoDialogFragment)
        }

        binding.backButton.setOnClickListener {
            handleBackButtonPress()
        }

        binding.generateSampleButton.setOnClickListener {
            binding.progressOverlayView.visibility = View.VISIBLE

            samplingViewModel.beginSampling()
        }

        binding.nextButton.setOnClickListener {
            handleNextButtonPress()
        }

        if (GeoUtils.isSelfIntersectingPolygon3( enumArea.vertices))
        {
            ConfirmationDialog( activity, resources.getString(R.string.oops), resources.getString(R.string.boundary_is_self_intersecting), resources.getString(R.string.no), resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
                when( buttonPressed )
                {
                    ConfirmationDialog.ButtonPress.Left -> {
                    }
                    ConfirmationDialog.ButtonPress.Right -> {
                        redefineEnumerationAreaBoundary()
                    }
                    ConfirmationDialog.ButtonPress.None -> {
                    }
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        isHandlingTapEvent = false
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateSampleFragment.value.toString() + ": " + this.javaClass.simpleName
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
        else if (study.samplingMethod == SamplingMethod.Cluster || study.samplingMethod == SamplingMethod.Strata)
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

    fun doesSampleHaveDuplicates( enumAreas: ArrayList<EnumArea> ) : Boolean
    {
        duplicateLocations.clear()

        for (enumArea in enumAreas)
        {
            for (location1 in enumArea.locations)
            {
                for (location2 in enumArea.locations)
                {
                    if (location1 == location2) continue
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

        return duplicateLocations.isNotEmpty()
    }

    fun doesSampleHaveGeofenceViolations( enumAreas: ArrayList<EnumArea> ) : Boolean
    {
        geofenceViolations.clear()

        for (enumArea in enumAreas)
        {
            for (location in enumArea.locations)
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

        return geofenceViolations.isNotEmpty()
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
                if (sampleHasDuplicates)
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.DuplicatePage )
                }
                else
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.PreviewPage )
                }
            }
            SamplingViewModel.SamplePageState.GenerateSamplePage -> {
                if (sampleHasGeofenceViolations)
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.GeofenceViolationPage )
                }
                else if (sampleHasDuplicates)
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.DuplicatePage )
                }
                else
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.PreviewPage )
                }
            }
            SamplingViewModel.SamplePageState.SaveSamplePage -> {
                samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.GenerateSamplePage )
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
                if (sampleHasDuplicates)
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.DuplicatePage )
                }
                else if (sampleHasGeofenceViolations)
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.GeofenceViolationPage )
                }
                else
                {
                    samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.GenerateSamplePage )
                }
            }
            SamplingViewModel.SamplePageState.DuplicatePage -> {
                if (sampleHasGeofenceViolations)
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
                samplingViewModel.setSamplePageState( SamplingViewModel.SamplePageState.SaveSamplePage )
            }
            SamplingViewModel.SamplePageState.SaveSamplePage -> {
                saveSample()
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
                binding.beginReviewTextView.visibility = View.VISIBLE
                binding.backButton.text = resources.getString(R.string.back )
                binding.nextButton.text = resources.getString(R.string.next )
                refreshMap()
            }
            SamplingViewModel.SamplePageState.DuplicatePage -> {
                binding.nextButton.isEnabled = true
                binding.titleTextView.text = resources.getString( R.string.review_possible_duplicates )
                binding.generateSampleButton.visibility = View.GONE
                binding.beginReviewTextView.visibility = View.GONE
                binding.backButton.text = resources.getString(R.string.back )
                binding.nextButton.text = resources.getString(R.string.next )
                showMap( duplicateLocations )
            }
            SamplingViewModel.SamplePageState.GeofenceViolationPage -> {
                binding.nextButton.isEnabled = true
                binding.titleTextView.text = resources.getString( R.string.review_booudary_violations )
                binding.generateSampleButton.visibility = View.GONE
                binding.beginReviewTextView.visibility = View.GONE
                binding.backButton.text = resources.getString(R.string.back )
                binding.nextButton.text = resources.getString(R.string.next )
                showMap(geofenceViolations )
            }
            SamplingViewModel.SamplePageState.GenerateSamplePage -> {
                binding.nextButton.isEnabled = false
                binding.titleTextView.text = resources.getString( R.string.generate_sample )
                binding.generateSampleButton.visibility = View.VISIBLE
                binding.beginReviewTextView.visibility = View.GONE
                binding.backButton.text = resources.getString(R.string.back )
                binding.nextButton.text = resources.getString(R.string.next )
                clearSample()
                refreshMap()
            }
            SamplingViewModel.SamplePageState.SaveSamplePage -> {
                binding.nextButton.isEnabled = true
                binding.titleTextView.text = resources.getString( R.string.save_sample )
                binding.generateSampleButton.visibility = View.GONE
                binding.beginReviewTextView.visibility = View.GONE
                binding.backButton.text = resources.getString(R.string.back )
                binding.nextButton.text = resources.getString(R.string.save )
            }
            SamplingViewModel.SamplePageState.SampleGeneratedPage -> {
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
            findNavController().navigate(R.id.action_navigate_to_ManageCollectionTeamsFragment)
        }
    }

    fun refreshMap()
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
            if (study.samplingMethod == SamplingMethod.Cluster || study.samplingMethod == SamplingMethod.Strata)
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
                    val currentPage = samplingViewModel.samplePageState.value
                    val generateSamplePage = SamplingViewModel.SamplePageState.GenerateSamplePage
                    val saveSamplePage = SamplingViewModel.SamplePageState.SaveSamplePage
                    val sampleGeneratedPage = SamplingViewModel.SamplePageState.SampleGeneratedPage

                    if (location.enumerationItems.first().isExcluded && (currentPage == generateSamplePage || currentPage == saveSamplePage || currentPage == sampleGeneratedPage))
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
            MapManager.instance().loadMarkers( activity!!, mapView!!, markerProperties, false )
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
            if (study.samplingMethod == SamplingMethod.Cluster || study.samplingMethod == SamplingMethod.Strata)
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

                            if (sampledItem.isExcluded)
                            {
                                resourceId = R.drawable.home_red
                            }
                            else if (sampledItem.samplingState == SamplingState.Sampled || sampledItem.subsetSamplingState == SamplingState.Sampled)
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
                            if (sampledItem.isExcluded)
                            {
                                resourceId = R.drawable.multi_home_red
                            }
                            else if (sampledItem.samplingState == SamplingState.Sampled || sampledItem.subsetSamplingState == SamplingState.Sampled)
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
            MapManager.instance().loadMarkers( activity!!, mapView!!, markerProperties, false )
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
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                editor.commit()

                val zoom = sharedViewModel.currentZoomLevel?.value ?: 0.0

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea, zoom ) { mapView ->
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                val zoom = sharedViewModel.currentZoomLevel?.value ?: 0.0

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea, zoom ) { mapView ->
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

    override fun onDestroyView()
    {
        mapView = null
        _binding = null

        super.onDestroyView()
    }
}