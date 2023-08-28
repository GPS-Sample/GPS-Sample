package edu.gtri.gpssample.fragments.create_collection_team

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
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
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

class CreateCollectionTeamFragment : Fragment()
{
    private lateinit var map: GoogleMap
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var sampleArea: SampleArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var defaultColorList: ColorStateList
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager

    private var createMode = false
    private var selectionGeometry: Geometry? = null
    private var selectionPolygon: Polygon? = null
    private var selectionMarkers = ArrayList<Marker>()
    private var _binding: FragmentCreateCollectionTeamBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm

        val samplingVm : SamplingViewModel by activityViewModels()
        samplingViewModel = samplingVm
        samplingViewModel.currentFragment = this
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy
        samplingViewModel.config = sharedViewModel.currentConfiguration?.value

        samplingVm.currentSampleArea?.value?.let { sampleArea ->
            this.sampleArea = sampleArea
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateCollectionTeamBinding.inflate(inflater, container, false)
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
        mapboxManager = MapboxManager( activity!!, pointAnnotationManager, polygonAnnotationManager )

        binding.dropPinButton.backgroundTintList?.let {
            defaultColorList = it
        }

        binding.dropPinButton.setOnClickListener {

            if (createMode)
            {
                createMode = false
            }
            else
            {
                createMode = true
                binding.dropPinButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
        }

        binding.drawPolygonButton.setOnClickListener {

            if (createMode && selectionMarkers.size > 2)
            {
                createMode = false
                binding.dropPinButton.setBackgroundTintList(defaultColorList);

                val points1 = ArrayList<Coordinate>()
                val points2 = ArrayList<Coordinate>()

                enumArea.vertices.map {
                    points1.add( Coordinate( it.toLatLng().longitude, it.toLatLng().latitude ))
                }

                // close the poly
                points1.add( points1[0] )

                selectionMarkers.map { marker ->
                    points2.add( Coordinate(marker.position.longitude, marker.position.latitude))
                    marker.remove()
                }

                selectionMarkers.clear()

                // close the poly
                points2.add( points2[0] )

                val geometryFactory = GeometryFactory()
                val geometry1: Geometry = geometryFactory.createPolygon(points1.toTypedArray())
                val geometry2: Geometry = geometryFactory.createPolygon(points2.toTypedArray())

                geometry1.intersection(geometry2)?.let { polygon ->
                    val vertices = ArrayList<LatLng>()

                    selectionGeometry = polygon

                    polygon.boundary?.coordinates?.map {
                        vertices.add( LatLng( it.y, it.x ))  // latitude is th Y axis in JTS
                    }

                    if (vertices.isNotEmpty())
                    {
                        val polygonOptions = PolygonOptions()
                            .fillColor(0x40ff0000.toInt())
                            .clickable(false)
                            .addAll( vertices )

                        selectionPolygon = map.addPolygon( polygonOptions )
                    }
                }
            }
        }

        binding.clearSelectionsButton.setOnClickListener {
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

            selectionPolygon?.points?.map {
                polygon.add( LatLon( it.latitude, it.longitude ))
            }

            study.id?.let { studyId ->
                DAO.teamDAO.createOrUpdateTeam( Team( studyId, binding.teamNameEditText.text.toString(), false, polygon ), enumArea)?.let { team ->
                    sampleArea.collectionTeams.add(team)
                }

                findNavController().popBackStack()
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateCollectionTeamFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun refreshMap()
    {
        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty())
        {
            mapboxManager.addPolygon(pointList)

            var currentZoomLevel = sharedViewModel.performEnumerationModel.currentZoomLevel?.value

            if (currentZoomLevel == null)
            {
                currentZoomLevel = 14.0
                sharedViewModel.performEnumerationModel.setCurrentZoomLevel(currentZoomLevel)
            }

            currentZoomLevel?.let { currentZoomLevel ->
                val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
                val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
                val cameraPosition = CameraOptions.Builder()
                    .zoom(currentZoomLevel)
                    .center(point)
                    .build()

                binding.mapView.getMapboxMap().setCamera(cameraPosition)
            }

            for (location in sampleArea.locations)
            {
                if (!location.isLandmark && location.items.isNotEmpty())
                {
                    // assuming only 1 enumeration item per location, for now...
                    val sampledItem = location.items[0] as? SampledItem

                    sampledItem?.let { sampledItem ->
                        if (sampledItem.samplingState == SamplingState.Sampled)
                        {
                            val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                            mapboxManager.addMarker( point, R.drawable.home_black )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}