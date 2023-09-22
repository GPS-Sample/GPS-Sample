package edu.gtri.gpssample.fragments.create_collection_team

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateCollectionTeamBinding
import edu.gtri.gpssample.databinding.FragmentCreateEnumerationTeamBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

class CreateCollectionTeamFragment : Fragment(),
    OnMapClickListener,
    View.OnTouchListener
{
    private lateinit var study: Study
    private lateinit var sampleArea: SampleArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var polylineAnnotation: PolylineAnnotation
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager

    private var createMode = false
    private var intersectionPolygon: PolygonAnnotation? = null
    private var _binding: FragmentCreateEnumerationTeamBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        val samplingVm : SamplingViewModel by activityViewModels()

        samplingViewModel = samplingVm
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy
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

        samplingViewModel.currentSampleArea?.value?.let { sampleArea ->
            this.sampleArea = sampleArea
        }

        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    refreshMap()
                }
            }
        )

        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager(binding.mapView)
        polygonAnnotationManager = binding.mapView.annotations.createPolygonAnnotationManager()
        polylineAnnotationManager = binding.mapView.annotations.createPolylineAnnotationManager(binding.mapView)
        mapboxManager = MapboxManager( activity!!, pointAnnotationManager, polygonAnnotationManager )

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
                    polygonAnnotationManager.delete( it )
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

            study.id?.let { studyId ->
                val polygon = ArrayList<LatLon>()

                intersectionPolygon?.points?.map { points ->
                    points.map { point ->
                        polygon.add( LatLon( point.latitude(), point.longitude()))
                    }
                }

                if (polygon.isEmpty())
                {
                    sampleArea.vertices.map {
                        polygon.add( LatLon( it.latitude, it.longitude ))
                    }
                }

                val team = DAO.teamDAO.createOrUpdateTeam( Team( studyId, binding.teamNameEditText.text.toString(), polygon ), sampleArea)

                team?.let { team ->
                    sampleArea.collectionTeams.add(team)
                    findNavController().popBackStack()
                }
            }
        }

        binding.overlayView.setOnTouchListener(this)
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

        sampleArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty())
        {
            mapboxManager.addPolygon(pointList,"#000000")

            var currentZoomLevel = sharedViewModel.performEnumerationModel.currentZoomLevel?.value

            if (currentZoomLevel == null)
            {
                currentZoomLevel = 14.0
                sharedViewModel.performEnumerationModel.setCurrentZoomLevel(currentZoomLevel)
            }

            currentZoomLevel?.let { currentZoomLevel ->
                val latLngBounds = GeoUtils.findGeobounds(sampleArea.vertices)
                val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
                val cameraPosition = CameraOptions.Builder()
                    .zoom(currentZoomLevel)
                    .center(point)
                    .build()

                binding.mapView.getMapboxMap().setCamera(cameraPosition)
            }

            for (location in sampleArea.locations)
            {
                if (!location.isLandmark && location.enumerationItems.isNotEmpty()) {
                    // assuming only 1 enumeration item per location, for now...
                    val sampledItem = location.enumerationItems[0]

                    if (sampledItem.samplingState == SamplingState.Sampled) {
                        val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                        mapboxManager.addMarker( point, R.drawable.home_black )
                    }
                }
            }
        }
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
                val points1 = ArrayList<Coordinate>()
                val points2 = ArrayList<Coordinate>()

                // convert ArrayList<LatLon> to ArrayList<Coordinate>
                sampleArea.vertices.map {
                    points1.add( Coordinate( it.toLatLng().longitude, it.toLatLng().latitude ))
                }

                // close the polygon
                points1.add( points1[0])

                // create a copy of the newly drawn polyline
                val polyList = ArrayList<Point>( polyLinePoints )

                // close the polygon
                polyList.add( polyLinePoints[0])

                // convert ArrayList<Point> to ArrayList<Coordinate>
                polyList.map {
                    points2.add( Coordinate( it.longitude(), it.latitude()))
                }

                // compute the intersection of points1 & points2
                val geometryFactory = GeometryFactory()
                val geometry1: Geometry = geometryFactory.createPolygon(points1.toTypedArray())
                val geometry2: Geometry = geometryFactory.createPolygon(points2.toTypedArray())

                try {
                    geometry1.intersection(geometry2)?.let { polygon ->
                        val vertices = ArrayList<Point>()

                        polygon.boundary?.coordinates?.map {
                            vertices.add( Point.fromLngLat(it.x, it.y))
                        }

                        if (vertices.isNotEmpty())
                        {
                            val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()
                            pointList.add( vertices )
                            intersectionPolygon = mapboxManager.addPolygon(pointList,"#ff0000")
                        }
                    }
                }
                catch( ex: Exception )
                {
                    Log.d( "xxx", ex.stackTrace.toString())
                }

                polyLinePoints.clear()
                polylineAnnotation.points = polyLinePoints
                polylineAnnotationManager.update(polylineAnnotation)

                createMode = false
                binding.overlayView.visibility = View.GONE
                binding.drawPolygonButton.setBackgroundResource( R.drawable.draw )
            }
            else
            {
                val point = binding.mapView.getMapboxMap().coordinateForPixel(ScreenCoordinate(p1.x.toDouble(),p1.y.toDouble()))
                polyLinePoints.add( point )

                if (!this::polylineAnnotation.isInitialized)
                {
                    val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                        .withPoints(polyLinePoints)
                        .withLineColor("#ee4e8b")
                        .withLineWidth(5.0)

                    polylineAnnotation = polylineAnnotationManager.create(polylineAnnotationOptions)
                }
                else
                {
                    polylineAnnotation.points = polyLinePoints
                    polylineAnnotationManager.update(polylineAnnotation)
                }
            }
        }

        return true
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}