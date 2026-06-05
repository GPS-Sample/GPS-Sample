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
import edu.gtri.gpssample.constants.MapEngine

class CreateSampleFragment : Fragment(), MapManager.MapManagerDelegate
{
    private lateinit var study: Study
    private lateinit var mapView: View
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel : ConfigurationViewModel

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
                    for (enumArea in config.enumAreas)
                    {
                        DAO.enumAreaDAO.loadLazyLocations( enumArea )
                    }
                }
                else
                {
                    DAO.enumAreaDAO.loadLazyLocations( enumArea )
                }
            }

            // back on the main thread...
            binding.progressOverlayView.visibility = View.GONE

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

            val zoom = sharedViewModel.currentZoomLevel?.value ?: 0.0

            MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea, zoom,this@CreateSampleFragment ) { mapView ->
                this@CreateSampleFragment.mapView = mapView

                binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

                sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                    MapManager.instance().centerMap( enumArea.vertices, currentZoomLevel, mapView )
                }


                samplingViewModel.refreshMap.observe(viewLifecycleOwner)
                {
                    refreshMap()
                }

                refreshMap()
            }

            binding.infoButton.setOnClickListener{
                findNavController().navigate(R.id.action_navigate_to_SamplingInfoDialogFragment)
            }

            if (study.samplingMethod == SamplingMethod.SimpleRandom)
            {
                for (enumArea in config.enumAreas)
                {
                    if (enumArea.collectionTeams.isNotEmpty())
                    {
                        binding.sampleButton.visibility = View.GONE
                        binding.nextButton.setText( resources.getString( R.string.next ))
                        break
                    }
                }
            }
            else if (study.samplingMethod == SamplingMethod.Cluster || study.samplingMethod == SamplingMethod.Strata)
            {
                if (enumArea.collectionTeams.isNotEmpty())
                {
                    binding.sampleButton.visibility = View.GONE
                    binding.nextButton.setText( resources.getString( R.string.next ))
                }
            }

            if (binding.sampleButton.isVisible)
            {
                // Clear the sample state from the unsaved enumeration items.
                // SampleState may be set to Sampled if you generate
                // a sample, then hit the back button, instead of the next button
                for (ea in config.enumAreas)
                {
                    if (study.samplingMethod == SamplingMethod.SimpleRandom || (study.samplingMethod == SamplingMethod.Cluster && ea.uuid == enumArea.uuid))
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

            binding.nextButton.setOnClickListener {
                if (binding.nextButton.text == resources.getString( R.string.save ))
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
                                        DAO.enumAreaDAO.createOrUpdateEnumArea(enumArea)
                                    }
                                }
                                else
                                {
                                    sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
                                        DAO.enumAreaDAO.createOrUpdateEnumArea(enumArea)
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
                else
                {
                    findNavController().navigate(R.id.action_navigate_to_ManageCollectionTeamsFragment)
                }
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
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateSampleFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun refreshMap()
    {
        MapManager.instance().clearMap( mapView )

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty())
        {
            MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x40 )
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
                    var resourceId: Int
                    var title = ""

                    if (location.enumerationItems.isNotEmpty())
                    {
                        title = location.enumerationItems[0].subAddress
                    }

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
            MapManager.instance().loadMarkers( activity!!, mapView, markerProperties, false )
        }
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

                val zoom = sharedViewModel.currentZoomLevel?.value ?: 0.0

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea, zoom,this ) { mapView ->
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

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea, zoom,this ) { mapView ->
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

            DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea )

            refreshMap()
        }
    }

    override fun onMarkerTapped( location: Location )
    {
    }

    override fun onZoomLevelChanged( zoomLevel: Double )
    {
        sharedViewModel.setCurrentZoomLevel( zoomLevel )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }
}