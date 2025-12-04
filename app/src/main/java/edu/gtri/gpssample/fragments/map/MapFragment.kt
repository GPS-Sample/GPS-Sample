/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.map

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mapbox.maps.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.models.LatLon
import edu.gtri.gpssample.database.models.MapTileRegion
import edu.gtri.gpssample.databinding.FragmentMapBinding
import edu.gtri.gpssample.dialogs.BusyIndicatorDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.MapHelpDialog
import edu.gtri.gpssample.dialogs.SelectionDialog
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.ArrayList
import java.util.Date

class MapFragment : Fragment(),
    View.OnTouchListener,
    MapManager.MapTileCacheDelegate,
    SelectionDialog.SelectionDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var centerOnLocation = true
    private var defineMapRegion = false
    private var mapTileRegion: MapTileRegion? = null
    private var busyIndicatorDialog: BusyIndicatorDialog? = null

    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel: ConfigurationViewModel

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

        MapManager.instance().selectOsmMap( activity!!, binding.osmMapView, binding.northUpImageView ) { mapView ->
            MapManager.instance().enableLocationUpdates( activity!!, mapView )
            MapManager.instance().startCenteringOnLocation( activity!!, binding.osmMapView )
            binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                MapManager.instance().setZoomLevel( mapView, currentZoomLevel )
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
                Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.define_center), Toast.LENGTH_SHORT).show()
            }
        }

        binding.cacheMapTilesButton.setOnClickListener {
            mapTileRegion?.let {
                defineMapRegion = false
                binding.mapOverlayView.visibility = View.GONE
                binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);
                busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.downloading_map_tiles), this )
                val mapTileRegions = ArrayList<MapTileRegion>()
                mapTileRegions.add(it)
                MapManager.instance().cacheMapTiles(activity!!, binding.osmMapView, mapTileRegions, this )
            }
        }

        binding.helpButton.setOnClickListener {
            MapHelpDialog( activity!! )
        }

        binding.clearMapButton.setOnClickListener {
            defineMapRegion = false
            binding.mapOverlayView.visibility = View.GONE
            binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);
            MapManager.instance().clearMap( binding.osmMapView )
        }

        binding.centerOnLocationButton.setOnClickListener {
            defineMapRegion = false
            binding.mapOverlayView.visibility = View.GONE
            binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);

            centerOnLocation = !centerOnLocation

            if (centerOnLocation)
            {
                MapManager.instance().startCenteringOnLocation( activity!!, binding.osmMapView )
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                MapManager.instance().stopCenteringOnLocation( binding.osmMapView )
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.MapFragment.value.toString() + ": " + this.javaClass.simpleName
    }
    
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean
    {
        if (defineMapRegion)
        {
            p1?.let { p1 ->
                if (p1.action == MotionEvent.ACTION_DOWN)
                {
                    val point = MapManager.instance().getLocationFromPixelPoint(binding.osmMapView, p1 )
                    defineMapRegion = false
                    binding.mapOverlayView.visibility = View.GONE
                    binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);

                    val inputDialog = InputDialog( activity!!, false, resources.getString(R.string.map_tile_boundary), "", resources.getString(R.string.cancel), resources.getString(R.string.save), null )  { action, text, tag ->
                        when (action) {
                            InputDialog.Action.DidCancel -> {
                                defineMapRegion = false
                            }
                            InputDialog.Action.DidEnterText -> {
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

                                    mapTileRegion = MapTileRegion( northEast, southWest )

                                    mapTileRegion?.let {
                                        addPolygon( it )
                                    }
                                }
                            }

                            InputDialog.Action.DidPressQRButton -> {}
                        }
                    }

                    inputDialog.editText?.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
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

        MapManager.instance().createPolygon( binding.osmMapView, pointList, Color.BLACK, 0, Color.BLACK )
    }

    override fun didPressCancelButton()
    {
        MapManager.instance().cancelTilePackDownload()
    }

    override fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    {
        busyIndicatorDialog?.let {
            activity!!.runOnUiThread {
                it.updateProgress(resources.getString(edu.gtri.gpssample.R.string.downloading_map_tiles) + " ${numLoaded}/${numNeeded}")
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
                    Toast.makeText(activity!!.applicationContext,  resources.getString(edu.gtri.gpssample.R.string.tile_pack_download_failed), Toast.LENGTH_SHORT).show()
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
                val mapEngines = resources.getTextArray( R.array.map_engines )

                ConfirmationDialog( activity, resources.getString(R.string.select_map_engine), "", mapEngines[0].toString(), mapEngines[1].toString(), null, false ) { buttonPressed, tag ->
                    when( buttonPressed )
                    {
                        ConfirmationDialog.ButtonPress.Left -> {
                            binding.mapboxMapView.visibility = View.GONE
                            MapManager.instance().selectOsmMap( activity!!, binding.osmMapView, binding.northUpImageView ) { mapView ->
                                MapManager.instance().enableLocationUpdates( activity!!, mapView )
                                MapManager.instance().startCenteringOnLocation( activity!!, mapView )
                                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                                sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                                    MapManager.instance().setZoomLevel( mapView, currentZoomLevel )
                                }
                            }
                        }
                        ConfirmationDialog.ButtonPress.Right -> {
                            binding.osmLabel.visibility = View.GONE
                            binding.osmMapView.visibility = View.GONE
                            binding.northUpImageView.visibility = View.GONE
                            binding.mapboxMapView.visibility = View.VISIBLE
                            MapManager.instance().selectMapboxMap( activity!!, binding.mapboxMapView ) { mapView ->
                                MapManager.instance().enableLocationUpdates( activity!!, mapView )
                                MapManager.instance().startCenteringOnLocation( activity!!, mapView )
                                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                                sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                                    MapManager.instance().setZoomLevel( mapView, currentZoomLevel )
                                }
                            }
                        }
                        ConfirmationDialog.ButtonPress.None -> {
                        }
                    }
                }
            }

            R.id.mapbox_streets ->
            {
                val editor = activity!!.getSharedPreferences("default", 0).edit()
                editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                editor.commit()

                MapManager.instance().selectOsmMap( activity!!, binding.osmMapView, binding.northUpImageView ) { mapView ->
                    if (centerOnLocation)
                    {
                        MapManager.instance().startCenteringOnLocation( activity!!, binding.osmMapView )
                    }
                }
            }

            R.id.satellite_streets ->
            {
                val editor = activity!!.getSharedPreferences("default", 0).edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                MapManager.instance().selectOsmMap( activity!!, binding.osmMapView, binding.northUpImageView ) { mapView ->
                    if (centerOnLocation)
                    {
                        MapManager.instance().startCenteringOnLocation( activity!!, binding.osmMapView )
                    }
                }
            }

            R.id.import_map_tiles ->
            {
                filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
            }

            R.id.select_map_tiles ->
            {
                SelectionDialog( activity!!, TileServer.getCachedFiles( activity!! ), this)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
//            TileServer.startServer( activity!!, uri, "", binding.mapView.getMapboxMap()) {
//                createAnnotationManagers()
//                TileServer.centerMap( binding.mapView.getMapboxMap(), sharedViewModel.currentZoomLevel?.value )
//            }
        }
    }

    override fun didMakeSelection( selection: String, tag: Int )
    {
        val mbTilesPath = activity!!.cacheDir.toString() + "/" + selection

//        TileServer.startServer( activity!!, null, mbTilesPath, binding.mapView.getMapboxMap()) {
//            createAnnotationManagers()
//            TileServer.centerMap( binding.mapView.getMapboxMap(), sharedViewModel.currentZoomLevel?.value )
//        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}