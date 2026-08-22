/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.create_collection_team

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateCollectionTeamBinding
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.ui.compose.ComposableSelectionDialogHost
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

class CreateCollectionTeamFragment : Fragment(), View.OnTouchListener
{
    private lateinit var study: Study
    private lateinit var mapView: View
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel : ConfigurationViewModel
    private var fingerPolyline: Any? = null
    private var _binding: FragmentCreateCollectionTeamBinding? = null
    private val binding get() = _binding!!
    private var intersectionPolygon: Any? = null
    private val locationUuids = ArrayList<String>()
    private val polyLinePoints = ArrayList<Point>()
    private lateinit var composableSelectionDialogHost: ComposableSelectionDialogHost
    enum class TapType {
        None,
        DrawBoundary,
        TapBoundary,
    }

    private var currentTapType = TapType.None

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        val samplingVm : SamplingViewModel by activityViewModels()

        samplingViewModel = samplingVm
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateCollectionTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        composableSelectionDialogHost = ComposableSelectionDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableSelectionDialogHost.Content()
        }

        sharedViewModel.currentConfiguration?.value?.let {_config ->
            config = _config
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {_enumArea ->
            enumArea = _enumArea
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let {_study ->
            study = _study
        }

        if (enumArea.mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( enumArea.mbTilesPath )
        }

        MapManager.instance().selectMap( requireActivity(), config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
            this.mapView = mapView

            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

            MapManager.instance().centerMap( enumArea, mapView )

            refreshMap()
        }

        binding.mapOverlayView.visibility = View.GONE

        binding.drawPolygonButton.setOnClickListener {
            if (currentTapType != TapType.None)
            {
                currentTapType = TapType.None
                binding.mapOverlayView.visibility = View.GONE
                binding.drawPolygonButton.setBackgroundResource( R.drawable.draw )
                binding.tapPolygonButton.setBackgroundResource( R.drawable.add_location_blue )

                refreshMap()
            }
            else
            {
                refreshMap()

                intersectionPolygon?.let {
                    MapManager.instance().removePolygon( mapView, it )
                    intersectionPolygon = null
                }

                polyLinePoints.clear()
                currentTapType = TapType.DrawBoundary
                binding.mapOverlayView.visibility = View.VISIBLE
                binding.drawPolygonButton.setBackgroundResource( R.drawable.save_blue )

                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.draw_boundary), Toast.LENGTH_SHORT).show()
            }
        }

        binding.tapPolygonButton.setOnClickListener {
            if (currentTapType != TapType.None)
            {
                currentTapType = TapType.None
                binding.mapOverlayView.visibility = View.GONE
                binding.drawPolygonButton.setBackgroundResource( R.drawable.draw )
                binding.tapPolygonButton.setBackgroundResource( R.drawable.add_location_blue )

                refreshMap()

                if (polyLinePoints.size >  2)
                {
                    createIntersectionPolygon()
                }
            }
            else
            {
                refreshMap()

                intersectionPolygon?.let {
                    MapManager.instance().removePolygon( mapView, it )
                    intersectionPolygon = null
                }

                polyLinePoints.clear()
                currentTapType = TapType.TapBoundary
                binding.mapOverlayView.visibility = View.VISIBLE
                binding.tapPolygonButton.setBackgroundResource( R.drawable.save_blue )

                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.tap_boundary), Toast.LENGTH_SHORT).show()
            }
        }

        binding.selectPolygonButton.setOnClickListener {
            val items = ArrayList<String>()

            for (team in enumArea.enumerationTeams)
            {
                items.add( team.name )
            }

            composableSelectionDialogHost.show(
                title = resources.getString(R.string.select_an_existing_team),
                message = null,
                items = items,
            ) { selection ->
                if (selection.isNotEmpty())
                {
                    polyLinePoints.clear()

                    for (team in enumArea.enumerationTeams)
                    {
                        if (selection == team.name)
                        {
                            for (latLon in team.polygon)
                            {
                                polyLinePoints.add( Point.fromLngLat(latLon.longitude, latLon.latitude ))
                            }

                            binding.teamNameEditText.setText( team.name )
                            createIntersectionPolygon()

                            break
                        }
                    }
                }
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

            binding.saveButton.isEnabled = false
            binding.cancelButton.isEnabled = false
            binding.busyView.visibility = View.VISIBLE

            Thread {
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

                val collectionTeam = CollectionTeam( enumArea.uuid, binding.teamNameEditText.text.toString(), polygonPoints, locationUuids )

                DAO.collectionTeamDAO.createOrUpdateCollectionTeam( collectionTeam, collectionTeam.version )
                enumArea.collectionTeams.add(collectionTeam)
                activity!!.runOnUiThread {
                    findNavController().popBackStack()
                }
            }.start()
        }

        binding.mapOverlayView.setOnTouchListener(this)
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateCollectionTeamFragment.value.toString() + ": " + this.javaClass.simpleName
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
            MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x30)

            for (collectionTeam in enumArea.collectionTeams)
            {
                val pts = java.util.ArrayList<Point>()
                val ptList = java.util.ArrayList<java.util.ArrayList<Point>>()

                collectionTeam.polygon.map {
                    pts.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
                }

                ptList.add( pts )

                if (ptList.isNotEmpty() && ptList[0].isNotEmpty())
                {
                    MapManager.instance().createPolygon( mapView, ptList, Color.BLACK, 0x30, Color.RED, collectionTeam.name )
                }
            }

            val markerProperties = ArrayList<MapManager.MarkerProperty>()

            for (location in enumArea.locations)
            {
                if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                {
                    if (location.enumerationItems.size == 1)
                    {
                        val sampledItem = location.enumerationItems[0]

                        if (sampledItem.samplingState == SamplingState.Sampled || sampledItem.subsetSamplingState == SamplingState.Sampled)
                        {
                            if (!locationBelongsToTeam( location ))
                            {
                                val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                                markerProperties.add( MapManager.MarkerProperty( location, R.drawable.home_light_blue, sampledItem.subAddress ))
                            }
                        }
                    }
                    else
                    {
                        for (sampledItem in location.enumerationItems)
                        {
                            if (sampledItem.samplingState == SamplingState.Sampled || sampledItem.subsetSamplingState == SamplingState.Sampled)
                            {
                                if (!locationBelongsToTeam( location ))
                                {
                                    val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                                    markerProperties.add( MapManager.MarkerProperty( location, R.drawable.multi_home_light_blue, sampledItem.subAddress ))
                                    break
                                }
                            }
                        }
                    }
                }
            }

            if (markerProperties.isNotEmpty())
            {
                MapManager.instance().loadMarkers( activity!!, mapView, markerProperties, null )
            }
        }
    }

    fun locationBelongsToTeam( location: Location ) : Boolean
    {
        for (team in enumArea.collectionTeams)
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
        try
        {
            p1?.let { p1 ->

                val point = MapManager.instance().getLocationFromPixelPoint( mapView, p1 )

                if (currentTapType == TapType.TapBoundary)
                {
                    if (p1.action == MotionEvent.ACTION_UP)
                    {
                        polyLinePoints.add( point )
                        MapManager.instance().createMarker( requireActivity(), mapView, point, R.drawable.location_blue, "" )

                        p0?.performClick()
                    }
                }
                else if (currentTapType == TapType.DrawBoundary)
                {
                    if (p1.action == MotionEvent.ACTION_UP)
                    {
                        currentTapType = TapType.None
                        binding.mapOverlayView.visibility = View.GONE
                        binding.drawPolygonButton.setBackgroundResource( R.drawable.draw )

                        fingerPolyline?.let {
                            MapManager.instance().removePolyline( mapView, it )
                            fingerPolyline = null
                        }

                        createIntersectionPolygon()
                    }
                    else if (p1.action == MotionEvent.ACTION_MOVE)
                    {
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
            }
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
        }

        return true
    }

    fun createIntersectionPolygon()
    {
        val points1 = GeoUtils.ArrayListOfLatLonToArrayListOfCoordinate( enumArea.vertices )

        // close the polygon, if necessary
        if (!points1.first().equals(points1.last()))
        {
            points1.add( points1[0])
        }

        // create a copy of the newly drawn polyline
        val polyList = ArrayList<Point>( polyLinePoints )

        // close the polygon
        polyList.add( polyLinePoints[0])

        val points2 = GeoUtils.ArrayListOfPointToArrayListOfCoordinate( polyList )

        // compute the intersection of points1 & points2
        val geometryFactory = GeometryFactory()
        val enumAreaPolygon: Geometry = geometryFactory.createPolygon(points1.toTypedArray())
        val selectionPolygon: Geometry = geometryFactory.createPolygon(points2.toTypedArray())

        try {
            enumAreaPolygon.intersection(selectionPolygon)?.let { intersectedPolygon ->
                if (!intersectedPolygon.isEmpty())
                {
                    var finalSelectedPolygon = intersectedPolygon.copy()

                    // subtract any existing teams from the selection
                    for (collectionTeam in enumArea.collectionTeams)
                    {
                        val points = GeoUtils.ArrayListOfLatLonToArrayListOfCoordinate(collectionTeam.polygon)

                        GeoUtils.createValidPolygon(points.toTypedArray())?.let { teamPolygon ->
                            // compute the intersection of the selected polygon with the existing team polygon
                            finalSelectedPolygon.intersection(teamPolygon)?.let { intersection ->
                                if (!intersection.isEmpty())
                                {
                                    // subtract the intersected polygon from the selectionPolygon
                                    finalSelectedPolygon.difference(intersection)?.let { remainder ->
                                        GeoUtils.createValidPolygon( remainder )?.let { validPolygon ->
                                            finalSelectedPolygon = validPolygon
                                        } ?: {
                                            throw Exception("Polygon difference failed" )
                                        }
                                    }
                                }
                            }
                        } ?: {
                            throw Exception( "Create valid team polygon failed" )
                        }
                    }

                    finalSelectedPolygon.boundary?.coordinates?.let { coordinates ->

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

                        val count = locationUuids.size

                        // now look for HH's that are in the selectionPolygon but outside of the EA
                        for (location in enumArea.locations)
                        {
                            val geometry3 = geometryFactory.createPoint( Coordinate( location.longitude, location.latitude))
                            if (selectionPolygon.contains(geometry3))
                            {
                                if (!locationBelongsToTeam( location ))
                                {
                                    if (!locationUuids.contains( location.uuid ))
                                    {
                                        locationUuids.add(location.uuid)
                                    }
                                }
                            }
                        }

                        if (locationUuids.size == count) // all HH's are within the EA
                        {
                            val vertices = GeoUtils.ArrayListOfCoordinateToArrayListOfPoint( coordinates )

                            val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()
                            pointList.add( vertices )

                            intersectionPolygon = MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x30 )
                        }
                        else // some HH's are outside of the EA, use the selectionPolygon
                        {
                            val vertices = GeoUtils.ArrayListOfCoordinateToArrayListOfPoint( selectionPolygon.coordinates )

                            val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()
                            pointList.add( vertices )

                            intersectionPolygon = MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x30 )
                        }
                    }
                }
            }
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
        }
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

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, enumArea ) { mapView ->
                    refreshMap()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView()
    {
        _binding = null

        super.onDestroyView()
    }
}
