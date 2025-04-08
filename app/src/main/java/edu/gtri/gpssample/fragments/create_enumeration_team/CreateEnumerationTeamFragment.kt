/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.create_enumeration_team

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateEnumerationTeamBinding
import edu.gtri.gpssample.dialogs.SelectMapTilesDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.managers.TileServer.Companion.mbTilesPath
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.io.File
import java.util.*

class CreateEnumerationTeamFragment : Fragment(),
    OnCameraChangeListener,
    OnMapClickListener,
    OnTouchListener,
    SelectMapTilesDialog.SelectMapTilesDialogDelegate
{
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var _binding: FragmentCreateEnumerationTeamBinding? = null
    private val binding get() = _binding!!

    private var createMode = false
    private val locationUuids = ArrayList<String>()
    private var polylineAnnotation: PolylineAnnotation? = null
    private var intersectionPolygon: PolygonAnnotation? = null
    private var intersectionPolyline: PolylineAnnotation? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var polygonAnnotationManager: PolygonAnnotationManager? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateEnumerationTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.createStudyModel.currentStudy?.value?.let {_study ->
            study = _study
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { _enumArea ->
            enumArea = _enumArea
        }

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)

        if (enumArea.mbTilesPath.isNotEmpty())
        {
            val mbTilesPath = sharedPreferences.getString( Keys.kMBTilesPath.value, null)

            if (mbTilesPath != enumArea.mbTilesPath)
            {
                TileServer.stopServer()
            }

            val editor = sharedPreferences.edit()
            editor.putString( Keys.kMBTilesPath.value, enumArea.mbTilesPath )
            editor.commit()
        }

        sharedPreferences.getString( Keys.kMBTilesPath.value, null)?.let { mbTilesPath ->
            if (TileServer.started)
            {
                TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                    createAnnotationManagers()
                    refreshMap()
                }
            } else
            {
                TileServer.startServer( activity!!, mbTilesPath, binding.mapView.getMapboxMap()) {
                    createAnnotationManagers()
                    refreshMap()
                }
            }
        } ?: run {
            // no tiles have been loaded, no need to start the server, just load the default map style
            TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                createAnnotationManagers()
                refreshMap()
            }
        }

        mapboxManager = MapboxManager.instance( activity!! )

        val currentZoomLevel = sharedViewModel.currentZoomLevel?.value
        if (currentZoomLevel == null)
        {
            sharedViewModel.setCurrentZoomLevel( 14.0 )
        }

        binding.mapView.gestures.addOnMapClickListener(this )

        binding.drawPolygonButton.setOnClickListener {

            if (createMode)
            {
                createMode = false
                binding.overlayView.visibility = View.GONE
                binding.drawPolygonButton.setBackgroundResource( R.drawable.draw )
            }
            else
            {
                intersectionPolygon?.let {
                    polygonAnnotationManager?.delete( it )
                    intersectionPolygon = null
                }

                intersectionPolyline?.let {
                    polylineAnnotation?.let {
                        polylineAnnotationManager?.delete(it)
                    }
                    intersectionPolyline = null
                }

                createMode = true
                binding.overlayView.visibility = View.VISIBLE
                binding.drawPolygonButton.setBackgroundResource( R.drawable.save_blue )
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            if (binding.teamNameEditText.text.toString().length == 0)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.team_name_message), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val polygon = ArrayList<LatLon>()

            var creationDate = Date().time

            intersectionPolygon?.points?.map { points ->
                points.map { point ->
                    polygon.add( LatLon( creationDate++, point.latitude(), point.longitude()))
                }
            }

            if (polygon.isEmpty())
            {
                enumArea.vertices.map {
                    polygon.add( LatLon( creationDate++, it.latitude, it.longitude ))
                }
            }

            val enumerationTeam = DAO.enumerationTeamDAO.createOrUpdateEnumerationTeam( EnumerationTeam( enumArea.uuid, binding.teamNameEditText.text.toString(), polygon, locationUuids ))

            enumerationTeam?.let { team ->
                enumArea.enumerationTeams.add(team)
                findNavController().popBackStack()
            }
        }

        binding.overlayView.setOnTouchListener(this)
    }

    fun createAnnotationManagers()
    {
        pointAnnotationManager = mapboxManager.createPointAnnotationManager( pointAnnotationManager, binding.mapView )
        polygonAnnotationManager = mapboxManager.createPolygonAnnotationManager( polygonAnnotationManager, binding.mapView )
        polylineAnnotationManager = mapboxManager.createPolylineAnnotationManager( polylineAnnotationManager, binding.mapView )
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateEnumerationTeamFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun refreshMap()
    {
        binding.mapView.getMapboxMap().removeOnCameraChangeListener( this )

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty())
        {
            mapboxManager.addPolygon( polygonAnnotationManager, pointList,"#000000", 0.25)
            mapboxManager.addPolyline( polylineAnnotationManager, pointList[0], "#ff0000" )

            sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
                val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
                val cameraPosition = CameraOptions.Builder()
                    .zoom(currentZoomLevel)
                    .center(point)
                    .build()

                binding.mapView.getMapboxMap().setCamera(cameraPosition)
            }

            for (enumerationTeam in enumArea.enumerationTeams)
            {
                val pts = java.util.ArrayList<Point>()
                val ptList = java.util.ArrayList<java.util.ArrayList<Point>>()

                enumerationTeam.polygon.map {
                    pts.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
                }

                ptList.add( pts )

                if (ptList.isNotEmpty() && ptList[0].isNotEmpty())
                {
                    mapboxManager.addPolygon( polygonAnnotationManager, ptList, "#000000", 0.25)
                    mapboxManager.addPolyline( polylineAnnotationManager, ptList[0], "#ff0000" )

                    val latLngBounds = GeoUtils.findGeobounds(enumerationTeam.polygon)
                    val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
                    mapboxManager.addViewAnnotationToPoint( binding.mapView.viewAnnotationManager, point, enumerationTeam.name, "#80FFFFFF" )
                }
            }

            for (location in enumArea.locations)
            {
                if (!location.isLandmark)
                {
                    if (!locationBelongsToTeam( location ))
                    {
                        val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                        mapboxManager.addMarker( pointAnnotationManager, point, R.drawable.home_black )
                    }
                }
            }
        }

        binding.mapView.getMapboxMap().addOnCameraChangeListener( this )
    }

    fun locationBelongsToTeam( location: Location ) : Boolean
    {
        for (team in enumArea.enumerationTeams)
        {
            for (locationUuid in team.locationUuids)
            {
                if (location.uuid == locationUuid)
                {
                    return true
                }
            }
        }

        return false
    }

    override fun onMapClick(point: Point): Boolean
    {
        return false
    }

    private val polyLinePoints = ArrayList<Point>()

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean
    {
        p1?.let { p1 ->

            if (p1.action == MotionEvent.ACTION_UP)
            {
                val points1 = MapboxManager.ArrayListOfLatLonToArrayListOfCoordinate( enumArea.vertices )

                // close the polygon, if necessary
                if (!points1.first().equals(points1.last()))
                {
                    points1.add( points1[0])
                }

                // create a copy of the newly drawn polyline
                val polyList = ArrayList<Point>( polyLinePoints )

                // close the polygon
                polyList.add( polyLinePoints[0])

                val points2 = MapboxManager.ArrayListOfPointToArrayListOfCoordinate( polyList )

                // compute the intersection of the EA (points1) & the newly drawn polygon (points2)
                val geometryFactory = GeometryFactory()
                val enumAreaPolygon = geometryFactory.createPolygon(points1.toTypedArray())
                val selectionPolygon = geometryFactory.createPolygon(points2.toTypedArray())

                try {
                    enumAreaPolygon.intersection( selectionPolygon )?.let { intersectedPolygon ->
                        if (!intersectedPolygon.isEmpty())
                        {
                            var finalSelectedPolygon = intersectedPolygon.copy()

                            // subtract any existing teams from the selection
                            for (enumTeam in enumArea.enumerationTeams)
                            {
                                val points = MapboxManager.ArrayListOfLatLonToArrayListOfCoordinate( enumTeam.polygon )

                                val teamPolygon = geometryFactory.createPolygon(points.toTypedArray())

                                // compute the intersection of the selected polygon with the existing team polygon
                                finalSelectedPolygon.intersection(teamPolygon)?.let { intersection ->
                                    if (!intersection.isEmpty())
                                    {
                                        // subtract the intersected polygon from the selectionPolygon
                                        finalSelectedPolygon.difference( intersection )?.let { remainder ->
                                            finalSelectedPolygon = remainder
                                        }
                                    }
                                }
                            }

                            finalSelectedPolygon.boundary?.coordinates?.let { coordinates ->

                                val vertices = MapboxManager.ArrayListOfCoordinateToArrayListOfPoint( coordinates )

                                val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()
                                pointList.add( vertices )

                                intersectionPolygon = mapboxManager.addPolygon( polygonAnnotationManager, pointList,"#ff0000", 0.25 )
                                intersectionPolyline = mapboxManager.addPolyline( polylineAnnotationManager, vertices, "#0000ff" )

                                locationUuids.clear()

                                for (location in enumArea.locations)
                                {
                                    val geometry3 = geometryFactory.createPoint( Coordinate( location.longitude, location.latitude))
                                    if (finalSelectedPolygon.contains(geometry3))
                                    {
                                        if (!locationBelongsToTeam( location ))
                                        {
                                            locationUuids.add( location.uuid )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                catch( ex: Exception )
                {
                    Log.d( "xxx", ex.stackTraceToString())
                }

                polyLinePoints.clear()
                polylineAnnotation?.let { it ->
                    it.points = polyLinePoints
                    polylineAnnotationManager?.update(it)
                }

                createMode = false
                binding.overlayView.visibility = View.GONE
                binding.drawPolygonButton.setBackgroundResource( R.drawable.draw )
            }
            else
            {
                val point = binding.mapView.getMapboxMap().coordinateForPixel(ScreenCoordinate(p1.x.toDouble(),p1.y.toDouble()))
                polyLinePoints.add( point )

                if (polylineAnnotation == null)
                {
                    val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                        .withPoints(polyLinePoints)
                        .withLineColor("#ee4e8b")
                        .withLineWidth(5.0)

                    polylineAnnotation = polylineAnnotationManager?.create(polylineAnnotationOptions)
                }
                else
                {
                    polylineAnnotation?.let { polylineAnnotation ->
                        polylineAnnotation.points = polyLinePoints
                        polylineAnnotationManager?.update(polylineAnnotation)
                    }
                }
            }
        }

        return true
    }

    override fun onCameraChanged(eventData: CameraChangedEventData)
    {
        sharedViewModel.setCurrentZoomLevel( binding.mapView.getMapboxMap().cameraState.zoom )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
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

                TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                    createAnnotationManagers()
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                    createAnnotationManagers()
                    refreshMap()
                }
            }

            R.id.import_map_tiles ->
            {
                filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
            }

            R.id.select_map_tiles ->
            {
                SelectMapTilesDialog( activity!!, TileServer.getCachedFiles( activity!! ), this)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            TileServer.stopServer()

            TileServer.startServer( activity!!, uri, binding.mapView.getMapboxMap()) {
                createAnnotationManagers()
                refreshMap()
                MapboxManager.centerMap( activity!!, binding.mapView.getMapboxMap(), sharedViewModel.currentZoomLevel?.value )
            }
        }
    }

    override fun selectMapTilesDialogDidSelectSaveButton( selection: String )
    {
        val mbTilesPath = activity!!.cacheDir.toString() + "/" + selection

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
        val editor = sharedPreferences.edit()
        editor.putString( Keys.kMBTilesPath.value, mbTilesPath )
        editor.commit()

        TileServer.stopServer()

        TileServer.startServer( activity!!, mbTilesPath, binding.mapView.getMapboxMap()) {
            createAnnotationManagers()
            refreshMap()
            MapboxManager.centerMap( activity!!, binding.mapView.getMapboxMap(), sharedViewModel.currentZoomLevel?.value )
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}