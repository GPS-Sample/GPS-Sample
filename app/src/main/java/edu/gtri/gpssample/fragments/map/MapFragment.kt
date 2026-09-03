/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.map

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.models.LatLon
import edu.gtri.gpssample.database.models.MapTileRegion
import edu.gtri.gpssample.databinding.FragmentMapBinding
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.ui.compose.ComposableBusyIndicatorDialogHost
import edu.gtri.gpssample.ui.compose.ComposableInputDialogHost
import edu.gtri.gpssample.ui.compose.ComposableMapHelpDialogHost
import edu.gtri.gpssample.ui.compose.ComposableSelectionDialogHost
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.ArrayList
import java.util.Date

class MapFragment : Fragment(),
    View.OnTouchListener,
    MapManager.MapTileCacheDelegate
{
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var centerOnLocation = true
    private var defineMapRegion = false
    private var mapTileRegion: MapTileRegion? = null
    private lateinit var mapView: View
    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel: ConfigurationViewModel
    private lateinit var composableInputDialogHost: ComposableInputDialogHost
    private lateinit var composableMapHelpDialogHost: ComposableMapHelpDialogHost
    private lateinit var composableSelectionDialogHost: ComposableSelectionDialogHost
    private lateinit var composableBusyIndicatorDialogHost: ComposableBusyIndicatorDialogHost

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentMapBinding.inflate( inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner
        }

        composableInputDialogHost = ComposableInputDialogHost()
        composableMapHelpDialogHost = ComposableMapHelpDialogHost()
        composableSelectionDialogHost = ComposableSelectionDialogHost()
        composableBusyIndicatorDialogHost = ComposableBusyIndicatorDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableInputDialogHost.Content()
            composableMapHelpDialogHost.Content()
            composableSelectionDialogHost.Content()
            composableBusyIndicatorDialogHost.Content()
        }

        binding.osmLabel.visibility = View.GONE
        binding.osmMapView.visibility = View.GONE
        binding.northUpImageView.visibility = View.GONE
        binding.mapboxMapView.visibility = View.VISIBLE

        MapManager.instance().selectMapboxMap( requireActivity(), binding.mapboxMapView, null ) { mapView ->
            this.mapView = mapView

            if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null)
                    {
                        val point = com.mapbox.geojson.Point.fromLngLat( location.longitude, location.latitude )
                        MapManager.instance().centerMap( point, mapView )
                        MapManager.instance().enableLocationUpdates( requireActivity(), mapView )
                        MapManager.instance().startCenteringOnLocation( requireActivity(), mapView )
                        binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                    }
                }
            }
        }

        binding.mapOverlayView.setOnTouchListener(this)

        binding.defineMapTileRegionButton.backgroundTintList?.let {
            defaultColorList = it
        }

        binding.mapOverlayView.setOnTouchListener(this)

        binding.defineMapTileRegionButton.setOnClickListener {
            if (defineMapRegion)
            {
                defineMapRegion = false
                binding.mapOverlayView.visibility = View.GONE
                binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);
            }
            else
            {
                defineMapRegion = true
                binding.mapOverlayView.visibility = View.VISIBLE
                binding.defineMapTileRegionButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                Toast.makeText(requireActivity().applicationContext,  resources.getString(R.string.define_center), Toast.LENGTH_SHORT).show()
            }
        }

        binding.cacheMapTilesButton.setOnClickListener {
            mapTileRegion?.let {
                defineMapRegion = false
                binding.mapOverlayView.visibility = View.GONE
                binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);

                composableBusyIndicatorDialogHost.show(title = resources.getString(R.string.downloading_map_tiles), message = null) {
                    composableBusyIndicatorDialogHost.cancel()
                    MapManager.instance().cancelTilePackDownload()
                }

                val mapTileRegions = ArrayList<MapTileRegion>()
                mapTileRegions.add(it)
                MapManager.instance().cacheMapTiles(requireActivity(), mapView, mapTileRegions, this )
            }
        }

        binding.helpButton.setOnClickListener {
            composableMapHelpDialogHost.show()
        }

        binding.clearMapButton.setOnClickListener {
            defineMapRegion = false
            binding.mapOverlayView.visibility = View.GONE
            binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);
            MapManager.instance().clearMap( mapView )
        }

        binding.centerOnLocationButton.setOnClickListener {
            defineMapRegion = false
            binding.mapOverlayView.visibility = View.GONE
            binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);

            centerOnLocation = !centerOnLocation

            if (centerOnLocation)
            {
                MapManager.instance().startCenteringOnLocation( requireActivity(), mapView )
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                MapManager.instance().stopCenteringOnLocation( mapView )
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
            }
        }

        selectMapEngine()
    }

    override fun onResume()
    {
        super.onResume()

        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.MapFragment.value.toString() + ": " + this.javaClass.simpleName
    }
    
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean
    {
        if (defineMapRegion)
        {
            p1?.let { p1 ->
                if (p1.action == MotionEvent.ACTION_DOWN)
                {
                    val point = MapManager.instance().getLocationFromPixelPoint(mapView, p1 )
                    MapManager.instance().createMarker( requireActivity(), mapView, Point.fromLngLat(point.longitude(), point.latitude()), R.drawable.breadcrumb, "X")

                    defineMapRegion = false
                    binding.mapOverlayView.visibility = View.GONE
                    binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);

                    composableInputDialogHost.show(
                        title = null,
                        description = resources.getString(R.string.map_tile_boundary),
                        text = "",
                        keyboardType = KeyboardType.Decimal,
                        cancelable = true,
                        onResult = { text ->
                            text.toDoubleOrNull()?.let {
                                defineMapRegion = false

                                text.toDoubleOrNull()?.let {
                                    val radius = it * 1000
                                    val r_earth = 6378000.0

                                    var latitude  = point.latitude()  + (radius / r_earth) * (180.0 / Math.PI)
                                    var longitude = point.longitude() + (radius / r_earth) * (180.0 / Math.PI) / Math.cos(latitude * Math.PI/180.0)
                                    val northEast = LatLon( 0, latitude, longitude )

                                    latitude  = point.latitude()  - (radius / r_earth) * (180.0 / Math.PI)
                                    longitude = point.longitude() - (radius / r_earth) * (180.0 / Math.PI) / Math.cos(latitude * Math.PI/180.0)
                                    val southWest = LatLon( 0, latitude, longitude )

                                    mapTileRegion = MapTileRegion( northEast, southWest, "" )

                                    mapTileRegion?.let {
                                        addPolygon( it )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        return true
    }

    fun addPolygon( mapTileRegion: MapTileRegion )
    {
        val points = ArrayList<com.mapbox.geojson.Point>()
        val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

        val vertices = ArrayList<LatLon>()

        var creationDate = Date().time

        vertices.add( LatLon( creationDate++, mapTileRegion.southWest.latitude, mapTileRegion.southWest.longitude ))
        vertices.add( LatLon( creationDate++, mapTileRegion.northEast.latitude, mapTileRegion.southWest.longitude ))
        vertices.add( LatLon( creationDate++,mapTileRegion.northEast.latitude, mapTileRegion.northEast.longitude ))
        vertices.add( LatLon( creationDate++, mapTileRegion.southWest.latitude, mapTileRegion.northEast.longitude ))

        vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0, Color.BLACK )
    }

    override fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    {
        requireActivity().runOnUiThread {
            composableBusyIndicatorDialogHost.updateMessage("${numLoaded}/${numNeeded}")
        }
    }

    override fun tilePacksLoaded( error: String )
    {
        requireActivity().runOnUiThread {
            composableBusyIndicatorDialogHost.cancel()
            if (error.isNotEmpty())
            {
                Toast.makeText(requireActivity().applicationContext,  resources.getString(R.string.tile_pack_download_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_map, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            R.id.select_map_engine ->
            {
                selectMapEngine()
            }

            R.id.mapbox_streets ->
            {
                val editor = requireActivity().getSharedPreferences("default", 0).edit()
                editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                editor.commit()

                if (binding.mapboxMapView.visibility == View.VISIBLE)
                {
                    MapManager.instance().selectMapboxMap( requireActivity(), binding.mapboxMapView, null ) { mapView ->
                        this.mapView = mapView
                        if (centerOnLocation)
                        {
                            MapManager.instance().startCenteringOnLocation( requireActivity(), binding.mapboxMapView )
                        }
                    }
                }
                else
                {
                    MapManager.instance().selectOsmMap( requireActivity(), binding.osmMapView, binding.northUpImageView ) { mapView ->
                        this.mapView = mapView
                        mapView.post {
                            binding.osmMapView.tileProvider.clearTileCache()
                            binding.osmMapView.invalidate()
                        }
                        if (centerOnLocation)
                        {
                            if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                            {
                                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    if (location != null)
                                    {
                                        val point = com.mapbox.geojson.Point.fromLngLat( location.longitude, location.latitude )
                                        MapManager.instance().centerMap( point, mapView )
                                        MapManager.instance().enableLocationUpdates( requireActivity(), mapView )
                                        MapManager.instance().startCenteringOnLocation( requireActivity(), mapView )
                                        binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            R.id.satellite_streets ->
            {
                val editor = requireActivity().getSharedPreferences("default", 0).edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                if (binding.mapboxMapView.visibility == View.VISIBLE)
                {
                    MapManager.instance().selectMapboxMap( requireActivity(), binding.mapboxMapView, null ) { mapView ->
                        this.mapView = mapView
                        if (centerOnLocation)
                        {
                            MapManager.instance().startCenteringOnLocation( requireActivity(), binding.mapboxMapView )
                        }
                    }
                }
                else
                {
                    MapManager.instance().selectOsmMap( requireActivity(), binding.osmMapView, binding.northUpImageView ) { mapView ->
                        this.mapView = mapView
                        mapView.post {
                            binding.osmMapView.tileProvider.clearTileCache()
                            binding.osmMapView.invalidate()
                        }
                        if (centerOnLocation)
                        {
                            if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                            {
                                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    if (location != null)
                                    {
                                        val point = com.mapbox.geojson.Point.fromLngLat( location.longitude, location.latitude )
                                        MapManager.instance().centerMap( point, mapView )
                                        MapManager.instance().enableLocationUpdates( requireActivity(), mapView )
                                        MapManager.instance().startCenteringOnLocation( requireActivity(), mapView )
                                        binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            R.id.import_map_tiles ->
            {
                filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
            }

            R.id.select_map_tiles ->
            {
                val cachedFiles = TileServer.getCachedFiles( requireActivity() )
                if (cachedFiles.isNotEmpty())
                {
                    composableSelectionDialogHost.show(
                        title = resources.getString(R.string.select_map_tiles),
                        message = null,
                        items = cachedFiles,
                    ) { selection ->
                        val mbTilesPath = requireActivity().cacheDir.toString() + "/" + selection
                        TileServer.startServer( requireActivity(), null, mbTilesPath, binding.mapboxMapView.getMapboxMap()) {
                            TileServer.centerMap( binding.mapboxMapView.getMapboxMap(), MapManager.zoomLevel() )
                        }
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun selectMapEngine()
    {
        val mapEngines = resources.getTextArray( R.array.map_engines )

        composableSelectionDialogHost.show(
            title = resources.getString(R.string.select_map_engine),
            message = null,
            items = listOf( mapEngines[0].toString(), mapEngines[1].toString()),
        ) { selection ->
            if (selection == mapEngines[0])
            {
                binding.osmLabel.visibility = View.VISIBLE
                binding.osmMapView.visibility = View.VISIBLE
                binding.mapboxMapView.visibility = View.GONE
                MapManager.instance().selectOsmMap( requireActivity(), binding.osmMapView, binding.northUpImageView ) { mapView ->
                    this.mapView = mapView

                    if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null)
                            {
                                val point = com.mapbox.geojson.Point.fromLngLat( location.longitude, location.latitude )
                                MapManager.instance().centerMap( point, mapView )
                                MapManager.instance().enableLocationUpdates( requireActivity(), mapView )
                                MapManager.instance().startCenteringOnLocation( requireActivity(), mapView )
                                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                            }
                        }
                    }
                }
            }
            else if (selection == mapEngines[1])
            {
                binding.osmLabel.visibility = View.GONE
                binding.osmMapView.visibility = View.GONE
                binding.northUpImageView.visibility = View.GONE
                binding.mapboxMapView.visibility = View.VISIBLE
                MapManager.instance().selectMapboxMap( requireActivity(), binding.mapboxMapView, null ) { mapView ->
                    this.mapView = mapView
                    MapManager.instance().enableLocationUpdates( requireActivity(), mapView )
                    MapManager.instance().startCenteringOnLocation( requireActivity(), mapView )
                    binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                    MapManager.instance().setMapZoomLevel( mapView, MapManager.zoomLevel())
                }
            }
        }
    }
    val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
//            TileServer.startServer( activity!!, uri, "", binding.mapView.getMapboxMap()) {
//                createAnnotationManagers()
//                TileServer.centerMap( binding.mapView.getMapboxMap(), sharedViewModel.currentZoomLevel?.value )
//            }
        }
    }

    override fun onDestroyView()
    {
        _binding = null

        super.onDestroyView()
    }
}
