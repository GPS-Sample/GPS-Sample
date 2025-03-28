/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.createsample

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
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
import edu.gtri.gpssample.databinding.FragmentHotspotBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.LaunchSurveyDialog
import edu.gtri.gpssample.dialogs.MapLegendDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import java.util.*

class CreateSampleFragment : Fragment(), OnCameraChangeListener, ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager

    private var _binding: FragmentCreateSampleBinding? = null
    private val binding get() = _binding!!
    private var allPolygonAnnotations = ArrayList<PolygonAnnotation>()
    private var allPolylineAnnotations = ArrayList<PolylineAnnotation>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        val samplingVm : SamplingViewModel by activityViewModels()

        sharedViewModel = vm
        sharedViewModel.currentFragment = this

        samplingViewModel = samplingVm
        samplingViewModel.currentFragment = this
        samplingViewModel.currentConfig = sharedViewModel.currentConfiguration
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy
        samplingViewModel.currentEnumArea = sharedViewModel.enumAreaViewModel.currentEnumArea

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

        if (sharedViewModel.currentZoomLevel?.value == null)
        {
            sharedViewModel.setCurrentZoomLevel( 16.0 )
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

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
        var style = Style.MAPBOX_STREETS
        sharedPreferences.getString( Keys.kMapStyle.value, null)?.let {
            style = it
        }

        binding.mapView.getMapboxMap().loadStyleUri(
            style,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style)
                {
                    pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager(binding.mapView)
                    polygonAnnotationManager = binding.mapView.annotations.createPolygonAnnotationManager()
                    polylineAnnotationManager = binding.mapView.annotations.createPolylineAnnotationManager()
                    mapboxManager = MapboxManager.instance( activity!! )

                    refreshMap()

                    samplingViewModel.setSampleAreasForMap( mapboxManager, pointAnnotationManager )
                }
            }
        )

        binding.infoButton.setOnClickListener{
            findNavController().navigate(R.id.action_navigate_to_SamplingInfoDialogFragment)
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
            if (study.samplingMethod == SamplingMethod.SimpleRandom)
            {
                for (enumArea in config.enumAreas)
                {
                    // visibility of the SampleButton is dependent on whether
                    // the sample was saved to the DB

                    val locations = DAO.locationDAO.getLocations( enumArea )

                    for (location in locations)
                    {
                        for (enumerationItem in location.enumerationItems)
                        {
                            if (enumerationItem.samplingState == SamplingState.Sampled)
                            {
                                binding.sampleButton.visibility = View.GONE
                                binding.nextButton.setText( resources.getString( R.string.next ))
                            }
                        }
                    }
                }
            }
            else if (study.samplingMethod == SamplingMethod.Cluster)
            {
                sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
                    for (location in DAO.locationDAO.getLocations( enumArea ))
                    {
                        for (enumerationItem in location.enumerationItems)
                        {
                            if (enumerationItem.samplingState == SamplingState.Sampled)
                            {
                                binding.sampleButton.visibility = View.GONE
                                binding.nextButton.setText( resources.getString( R.string.next ))
                            }
                        }
                    }
                }
            }
        }

        if (binding.sampleButton.visibility == View.VISIBLE) {
            // Clear the sample state from the unsaved enumeration items.
            // SampleState may be set to Sampled if you generate
            // a sample, then hit the back button, instead of the next button
            sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { currentEnumArea ->
                    for (enumArea in config.enumAreas) {
                        if (study.samplingMethod == SamplingMethod.SimpleRandom || (study.samplingMethod == SamplingMethod.Cluster && enumArea.uuid == currentEnumArea.uuid)) {
                            for (location in enumArea.locations) {
                                for (enumerationItem in location.enumerationItems) {
                                    enumerationItem.samplingState = SamplingState.NotSampled
                                }
                            }
                        }
                    }
                }
            }
        }

        binding.nextButton.setOnClickListener {

            if (binding.nextButton.text == resources.getString( R.string.save ))
            {
                binding.overlayView.visibility = View.VISIBLE

                Thread {
                    DAO.instance().writableDatabase.beginTransaction()

                    for (enumArea in config.enumAreas)
                    {
                        if (study.samplingMethod == SamplingMethod.Cluster)
                        {
                            if (enumArea.uuid == config.selectedEnumAreaUuid)
                            {
                                DAO.enumAreaDAO.createOrUpdateEnumArea(enumArea)
                                break
                            }
                        }
                        else
                        {
                            DAO.enumAreaDAO.createOrUpdateEnumArea(enumArea)
                        }
                    }

                    DAO.instance().writableDatabase.setTransactionSuccessful()
                    DAO.instance().writableDatabase.endTransaction()

                    activity!!.runOnUiThread {
                        binding.overlayView.visibility = View.GONE
                        findNavController().navigate(R.id.action_navigate_to_ManageCollectionTeamsFragment)
                    }
                }.start()
            }
            else
            {
                findNavController().navigate(R.id.action_navigate_to_ManageCollectionTeamsFragment)
            }
        }

        if (MapboxManager.isSelfIntersectingPolygon3( enumArea.vertices))
        {
            ConfirmationDialog( activity, resources.getString(R.string.oops),
                resources.getString(R.string.boundary_is_self_intersecting),
                resources.getString(R.string.no), resources.getString(R.string.yes), null, this@CreateSampleFragment)
        }
    }

    override fun didSelectFirstButton(tag: Any?)
    {
    }

    override fun didSelectSecondButton(tag: Any?)
    {
        redefineEnumerationAreaBoundary()
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateSampleFragment.value.toString() + ": " + this.javaClass.simpleName

    }

    fun refreshMap()
    {
        binding.mapView.getMapboxMap().removeOnCameraChangeListener( this )

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

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty())
        {
            mapboxManager.addPolygon( polygonAnnotationManager, pointList,"#000000", 0.25)?.let{
                allPolygonAnnotations.add( it )
            }

            mapboxManager.addPolyline( polylineAnnotationManager, pointList[0], "#ff0000" )?.let {
                allPolylineAnnotations.add( it )
            }

            val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
            mapboxManager.addViewAnnotationToPoint( binding.mapView.viewAnnotationManager, point, enumArea.name, "#80FFFFFF")

            if (enumArea.uuid == config.selectedEnumAreaUuid)
            {
                sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                    val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
                    val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
                    val cameraPosition = CameraOptions.Builder()
                        .zoom(currentZoomLevel)
                        .center(point)
                        .build()

                    binding.mapView.getMapboxMap().setCamera(cameraPosition)
                }
            }
        }

        binding.mapView.getMapboxMap().addOnCameraChangeListener( this )
    }

    override fun onCameraChanged(eventData: CameraChangedEventData)
    {
        sharedViewModel.setCurrentZoomLevel( binding.mapView.getMapboxMap().cameraState.zoom )
    }

    fun sampleGenerated()
    {
        binding.sampleButton.visibility = View.GONE
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

                binding.mapView.getMapboxMap().loadStyleUri(
                    Style.MAPBOX_STREETS,
                    object : Style.OnStyleLoaded {
                        override fun onStyleLoaded(style: Style) {
                            refreshMap()
                        }
                    }
                )
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                binding.mapView.getMapboxMap().loadStyleUri(
                    Style.SATELLITE_STREETS,
                    object : Style.OnStyleLoaded {
                        override fun onStyleLoaded(style: Style) {
                            refreshMap()
                        }
                    }
                )
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

            DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea )

            refreshMap()
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }
}