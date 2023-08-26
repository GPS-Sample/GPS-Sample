package edu.gtri.gpssample.fragments.create_enumeration_area

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.LocationType
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateEnumerationAreaBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.MultiPolygon
import io.github.dellisd.spatialk.geojson.Point
import kotlinx.coroutines.launch
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

class CreateEnumerationAreaFragment : Fragment(),
    OnMapClickListener,
    ConfirmationDialog.ConfirmationDialogDelegate,
    InputDialog.InputDialogDelegate
{
    private lateinit var config: Config
    private lateinit var mapboxManager: MapboxManager
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager

    private var editMode = false
    private var dropMode = false
    private var createMode = false
    private val binding get() = _binding!!
    private val unsavedEnumAreas = ArrayList<EnumArea>()
    private val pointHashMap = HashMap<Long,Location>()
    private val polygonHashMap = HashMap<Long,EnumArea>()
    private lateinit var defaultColorList : ColorStateList
    private var _binding: FragmentCreateEnumerationAreaBinding? = null
    private var droppedPointAnnotations = ArrayList<PointAnnotation?>()
    private var allPolygonAnnotations = ArrayList<PolygonAnnotation>()
    private var allPointAnnotations = ArrayList<PointAnnotation>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateEnumerationAreaBinding.inflate( inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = this.viewModel

            // Assign the fragment
            createEnumerationAreaFragment = this@CreateEnumerationAreaFragment
        }

        sharedViewModel.currentConfiguration?.value?.let { config ->
            this.config = config
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        arguments?.getBoolean(Keys.kEditMode.toString())?.let { editMode ->
            this.editMode = editMode
        }

        if (!editMode)
        {
            binding.toolbarTitle.visibility = View.GONE
            binding.toolbarLayout.visibility = View.GONE
            binding.buttonLayout.visibility = View.GONE
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

        binding.mapView.gestures.addOnMapClickListener(this )

        binding.importButton.setOnClickListener {
            dropMode = false
            createMode = false
            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);

            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_enumeration)), 1023)
        }

        binding.createButton.setOnClickListener {
            dropMode = false
            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);

            if (createMode)
            {
                if (droppedPointAnnotations.size > 2)
                {
                    createMode = false
                    InputDialog( activity!!, resources.getString(R.string.enter_enum_area_name), "", null, this )
                    binding.createButton.setBackgroundResource( R.drawable.edit_blue )
                }
            }
            else
            {
                droppedPointAnnotations.clear()
                createMode = true
                binding.createButton.setBackgroundResource( R.drawable.save_blue )
            }
        }

        binding.addHouseholdButton.backgroundTintList?.let {
            defaultColorList = it
        }

        binding.addHouseholdButton.setOnClickListener {
            createMode = false
            if (dropMode)
            {
                dropMode = false
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }
            else
            {
                dropMode = true
                removeAllPolygonOnClickListeners()
                binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            config.enumAreas.addAll( unsavedEnumAreas )
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.DefineEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onMapClick(point: com.mapbox.geojson.Point): Boolean
    {
        if (editMode)
        {
            if (createMode)
            {
                droppedPointAnnotations.add( mapboxManager.addMarker( point, R.drawable.location_blue ))

                return true
            }
            else if (dropMode)
            {
                dropMode = false
                createLocation( LatLng( point.latitude(), point.longitude()))
                refreshMap()
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
                return true
            }
        }

        return false
    }

    private fun refreshMap()
    {
        for (polygonAnnotation in allPolygonAnnotations)
        {
            polygonAnnotationManager.delete( polygonAnnotation )
        }

        allPolygonAnnotations.clear()

        for (pointAnnotation in allPointAnnotations)
        {
            pointAnnotationManager.delete( pointAnnotation )
        }

        allPointAnnotations.clear()

        val allEnumAreas = getAllEnumAreas()

        for (enumArea in allEnumAreas)
        {
            addPolygon(enumArea)

            if (enumArea.locations.isNotEmpty())
            {
                removeAllPolygonOnClickListeners()
            }

            for (location in enumArea.locations)
            {
                if (location.isLandmark)
                {
                    val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                    mapboxManager.addMarker( point, R.drawable.location_blue )
                }
                else
                {
                    var resourceId = R.drawable.home_black

                    var numComplete = 0

                    for (item in location.items)
                    {
                        val enumerationItem = item as EnumerationItem?
                        if(enumerationItem != null)
                        {
                            if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                            {
                                resourceId = R.drawable.home_red
                                break
                            }
                            else if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                            {
                                numComplete++
                            }
                        }
                    }

                    if (numComplete > 0 && numComplete == location.items.size)
                    {
                        resourceId = R.drawable.home_green
                    }

                    val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                    val pointAnnotation = mapboxManager.addMarker( point, resourceId )

                    pointAnnotation?.let {
                        pointHashMap[pointAnnotation.id] = location
                        allPointAnnotations.add( pointAnnotation )
                    }

                    if (editMode)
                    {
                        pointAnnotationManager?.apply {
                            addClickListener(
                                OnPointAnnotationClickListener { pointAnnotation ->
                                    ConfirmationDialog( activity, resources.getString(R.string.please_confirm),
                                        "${resources.getString(R.string.delete_household_message)}?",
                                        resources.getString(R.string.no), resources.getString(R.string.yes), pointAnnotation, this@CreateEnumerationAreaFragment)
                                    true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (allEnumAreas.isNotEmpty())
        {
            val enumArea = allEnumAreas[0]
            val point = com.mapbox.geojson.Point.fromLngLat( enumArea.vertices[0].longitude, enumArea.vertices[0].latitude )
            val cameraPosition = CameraOptions.Builder()
                .zoom(10.0)
                .center(point)
                .build()

            binding.mapView.getMapboxMap().setCamera(cameraPosition)
        }
    }

    fun getAllEnumAreas() : ArrayList<EnumArea>
    {
        val allEnumAreas = ArrayList<EnumArea>()
        allEnumAreas.addAll( config.enumAreas )
        allEnumAreas.addAll( unsavedEnumAreas )
        return allEnumAreas
    }

    fun createLocation( latLng: LatLng )
    {
        var enumArea = findEnumAreaOfLocation( config.enumAreas, latLng )

        if (enumArea == null)
        {
            enumArea = findEnumAreaOfLocation( unsavedEnumAreas, latLng )
        }

        enumArea?.let{  enumArea ->
            latLng?.let { latLng ->
                val location = Location( LocationType.Enumeration, latLng.latitude, latLng.longitude, false)
                enumArea.locations.add(location)
                refreshMap()
            }
        }
    }

    fun findEnumAreaOfLocation( enumAreas: ArrayList<EnumArea>, latLng: LatLng ) : EnumArea?
    {
        for (enumArea in enumAreas)
        {
            val points = ArrayList<Coordinate>()

            enumArea.vertices.map {
                points.add( Coordinate( it.toLatLng().longitude, it.toLatLng().latitude ))
            }

            points.add( points[0])

            val geometryFactory = GeometryFactory()
            val geometry: Geometry = geometryFactory.createPolygon(points.toTypedArray())

            val coordinate = Coordinate( latLng.longitude, latLng.latitude )
            val geometry1 = geometryFactory.createPoint( coordinate )
            if (geometry.contains( geometry1 ))
            {
                return enumArea
            }
        }

        return null
    }

    fun removeAllPolygonOnClickListeners()
    {
        polygonAnnotationManager?.apply {
            polygonAnnotationManager.clickListeners.removeAll {
                true
            }
        }
    }

    fun addPolygon( enumArea: EnumArea )
    {
        val points = ArrayList<com.mapbox.geojson.Point>()
        val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        val polygonAnnotation = mapboxManager.addPolygon( pointList )

        polygonAnnotation?.let { polygonAnnotation ->
            polygonHashMap[polygonAnnotation.id] = enumArea
            allPolygonAnnotations.add( polygonAnnotation)
        }

        if (editMode)
        {
            polygonAnnotationManager?.apply {
                addClickListener(
                    OnPolygonAnnotationClickListener { polygonAnnotation ->
                        polygonHashMap[polygonAnnotation.id]?.let { ea ->
                            ConfirmationDialog( activity, resources.getString(R.string.please_confirm),
                                "${resources.getString(R.string.delete_enum_area_message)} ${ea.name}?",
                                resources.getString(R.string.no), resources.getString(R.string.yes), polygonAnnotation, this@CreateEnumerationAreaFragment)
                        }
                        true
                    }
                )
            }
        }
    }

    override fun didEnterText( name: String, tag: Any? )
    {
        createMode = false

        val vertices = ArrayList<LatLon>()

        droppedPointAnnotations.map { pointAnnotation ->
            pointAnnotation?.let{ pointAnnotation ->
                vertices.add( LatLon( pointAnnotation.point.latitude(), pointAnnotation.point.longitude()))
                pointAnnotationManager.delete( pointAnnotation )
            }
        }

        val enumArea = EnumArea(  name, vertices )

        unsavedEnumAreas.add(enumArea)

        refreshMap()
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        if (tag is PolygonAnnotation)
        {
            val polygonAnnotation = tag as PolygonAnnotation

            polygonHashMap[polygonAnnotation.id]?.let { enumArea ->
                unsavedEnumAreas.remove( enumArea )
                config.enumAreas.remove( enumArea )
                DAO.enumAreaDAO.delete( enumArea )
            }
        }
        else if (tag is PointAnnotation)
        {
            val pointAnnotation = tag as PointAnnotation
            pointHashMap[pointAnnotation.id]?.let { location ->
                val allEnumAreas = getAllEnumAreas()

                val enumArea = findEnumAreaOfLocation( allEnumAreas, LatLng( location.latitude, location.longitude ))

                enumArea?.let {
                    enumArea.locations.remove(location)
                }

                DAO.locationDAO.delete(location)
            }
        }

        refreshMap()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1023 && resultCode == Activity.RESULT_OK)
        {
            val uri = data?.data

            uri?.let { uri ->

                try
                {
                    Thread {
                        val inputStream = activity!!.getContentResolver().openInputStream(uri)

                        inputStream?.let { inputStream ->
                            parseGeoJson( inputStream.bufferedReader().readText())
                        }
                    }.start()
                }
                catch( ex: java.lang.Exception )
                {
                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun parseGeoJson( text: String )
    {
        Log.d( "xxx", text )

        var points = ArrayList<Point>()
        val featureCollection = FeatureCollection.fromJson( text )

        featureCollection.forEach { feature ->

            var name = resources.getString(R.string.undefined)

            feature.getStringProperty("ClusterL")?.let {
                name = it
            }

            feature.geometry?.let { geometry ->
                when( geometry ) {
                    is MultiPolygon -> {
                        val enumArea = EnumArea(name, ArrayList<LatLon>())
                        val multiPolygon = geometry as MultiPolygon

                        multiPolygon.coordinates[0][0].forEach { position ->
                            enumArea.vertices.add( LatLon( position.latitude, position.longitude ))
                        }

                        unsavedEnumAreas.add(enumArea)
                    }
                    is Point -> {
                        val point = geometry as Point
                        points.add( point )
                    }
                    else -> {}
                }
            }
        }

        // figure out which enumArea contains each point

        var count = 0

        for (point in points)
        {
            Log.d( "xxx", "${count}/${points.size}")
            count += 1

            val allEnumAreas = ArrayList<EnumArea>()

            if (config.enumAreas.isNotEmpty())
            {
                allEnumAreas.addAll( config.enumAreas)
            }

            if (unsavedEnumAreas.isNotEmpty())
            {
                allEnumAreas.addAll( unsavedEnumAreas )
            }

            for (enumArea in allEnumAreas)
            {
                val enumAreaPoints = ArrayList<Coordinate>()

                enumArea.vertices.map {
                    enumAreaPoints.add( Coordinate( it.toLatLng().longitude, it.toLatLng().latitude ))
                }

                val geometryFactory = GeometryFactory()
                val geometry: Geometry = geometryFactory.createPolygon(enumAreaPoints.toTypedArray())

                val coordinate = Coordinate( point.coordinates.longitude, point.coordinates.latitude )
                val geometry1 = geometryFactory.createPoint( coordinate )
                if (geometry.contains( geometry1 ))
                {
                    val location = Location( LocationType.Enumeration, point.coordinates.latitude, point.coordinates.longitude, false )
                    DAO.locationDAO.createOrUpdateLocation( location, enumArea )

                    enumArea.locations.add( location )
                    break // found! assuming that it can only exist in a single EA, for now!
                }
            }
        }

        lifecycleScope.launch {
            refreshMap()
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}