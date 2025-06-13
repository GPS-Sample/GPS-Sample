/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.create_enumeration_team

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
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
import edu.gtri.gpssample.dialogs.SelectionDialog
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.osmdroid.views.overlay.Polygon
import java.util.*

class CreateEnumerationTeamFragment : Fragment(),
    OnTouchListener,
    MapManager.MapManagerDelegate
{
    private lateinit var study: Study
    private lateinit var mapView: View
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var _binding: FragmentCreateEnumerationTeamBinding? = null
    private val binding get() = _binding!!

    private var createMode = false
    private var fingerPolyline: Any? = null
    private val locationUuids = ArrayList<String>()
    private val polyLinePoints = ArrayList<Point>()
    private var intersectionPolygon: Any? = null

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

        sharedViewModel.currentConfiguration?.value?.let { _config ->
            config = _config
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let {_study ->
            study = _study
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { _enumArea ->
            enumArea = _enumArea
        }

        if (enumArea.mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( enumArea.mbTilesPath )
        }

        MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, this ) { mapView ->
            this.mapView = mapView

            sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                MapManager.instance().centerMap( enumArea, currentZoomLevel, mapView )
            }

            refreshMap()
        }

        binding.mapOverlayView.visibility = View.GONE

        binding.drawPolygonButton.setOnClickListener {

            if (createMode)
            {
                createMode = false
                binding.mapOverlayView.visibility = View.GONE
                binding.drawPolygonButton.setBackgroundResource( R.drawable.draw )
            }
            else
            {
                intersectionPolygon?.let {
                    MapManager.instance().removePolygon( mapView, it )
                    intersectionPolygon = null
                }

                createMode = true
                polyLinePoints.clear()
                binding.mapOverlayView.visibility = View.VISIBLE
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

            val polygonPoints = ArrayList<LatLon>()

            var creationDate = Date().time

            if (intersectionPolygon is MapManager.MapboxPolygon)
            {
                val mapboxPolygon = intersectionPolygon as MapManager.MapboxPolygon
                mapboxPolygon.polygonAnnotation?.points?.map { points ->
                    points.map { point ->
                        polygonPoints.add( LatLon( creationDate++, point.latitude(), point.longitude()))
                    }
                }
            }
            else if (intersectionPolygon is org.osmdroid.views.overlay.Polygon)
            {
                val osmPolygon = intersectionPolygon as org.osmdroid.views.overlay.Polygon
                osmPolygon.points.map { point ->
                    polygonPoints.add( LatLon( creationDate++, point.latitude, point.longitude ))
                }
            }

            if (polygonPoints.isEmpty())
            {
                enumArea.vertices.map {
                    polygonPoints.add( LatLon( creationDate++, it.latitude, it.longitude ))
                }
            }

            val enumerationTeam = DAO.enumerationTeamDAO.createOrUpdateEnumerationTeam( EnumerationTeam( enumArea.uuid, binding.teamNameEditText.text.toString(), polygonPoints, locationUuids ))

            enumerationTeam?.let { team ->
                enumArea.enumerationTeams.add(team)
                findNavController().popBackStack()
            }
        }

        binding.mapOverlayView.setOnTouchListener(this)
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateEnumerationTeamFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun refreshMap()
    {
        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        if (points.last() != points.first())
        {
            points.add( points.first())
        }

        pointList.add( points )

        if (pointList.isNotEmpty())
        {
            MapManager.instance().createPolygon( mapView, pointList,Color.BLACK, 0x40)

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
                    MapManager.instance().createPolygon( mapView, ptList, Color.BLACK, 0x40 )
                }
            }

            for (location in enumArea.locations)
            {
                if (!location.isLandmark)
                {
                    if (!locationBelongsToTeam( location ))
                    {
                        val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                        MapManager.instance().createMarker( activity!!, mapView, point, R.drawable.home_black )
                    }
                }
            }
        }
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

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean
    {
        p1?.let { p1 ->

            if (p1.action == MotionEvent.ACTION_UP)
            {
                fingerPolyline?.let {
                    MapManager.instance().removePolyline( mapView, it )
                    fingerPolyline = null
                }

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

                                intersectionPolygon = MapManager.instance().createPolygon( mapView, pointList,Color.BLACK, 0x40 )

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

                createMode = false
                binding.mapOverlayView.visibility = View.GONE
                binding.drawPolygonButton.setBackgroundResource( R.drawable.draw )
            }
            else
            {
                val point = MapManager.instance().getLocationFromPixelPoint( mapView, p1 )
                polyLinePoints.add( point )

                if (fingerPolyline == null)
                {
                    fingerPolyline = MapManager.instance().createPolyline( mapView, polyLinePoints, Color.rgb( 0xee, 0x4e,0x8b) )
                }
                else
                {
                    MapManager.instance().updatePolyline( mapView, fingerPolyline!!, point )
                }
            }
        }

        return true
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