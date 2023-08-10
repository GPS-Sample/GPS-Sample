package edu.gtri.gpssample.fragments.create_enumeration_area

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
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
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.MultiPolygon
import io.github.dellisd.spatialk.geojson.Point
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

class CreateEnumerationAreaFragment : Fragment(), OnMapReadyCallback, ConfirmationDialog.ConfirmationDialogDelegate, InputDialog.InputDialogDelegate
{
    private lateinit var config: Config
    private lateinit var map: GoogleMap
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var createMode = false
    private var vertexMarkers = ArrayList<Marker>()
    private var _binding: FragmentCreateEnumerationAreaBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateEnumerationAreaBinding.inflate(inflater, container, false)
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
            Toast.makeText(activity!!.applicationContext, "Fatal! Config not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        binding.imortButton.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select an Enumeration"), 1023)
        }

        binding.createButton.setOnClickListener {

            if (createMode)
            {
                if (vertexMarkers.size > 2)
                {
                    createMode = false
                    InputDialog( activity!!, "Enter the Enumeration Area name", "", null, this )
                    binding.createButton.setBackgroundResource( R.drawable.edit_blue )
                }
            }
            else
            {
                vertexMarkers.clear()
                createMode = true
                binding.createButton.setBackgroundResource( R.drawable.save_blue )
            }
        }
    }

    override fun didEnterText( name: String, tag: Any? )
    {
        createEnumArea( name )
    }

    fun createEnumArea( name: String )
    {
        createMode = false

        var vertices = ArrayList<LatLon>()

        vertexMarkers.map {
            vertices.add( LatLon( it.position.latitude, it.position.longitude ))
        }

        var enumArea = EnumArea(  name, vertices )
        //DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea )
        config.enumAreas.add(enumArea) // = DAO.enumAreaDAO.getEnumAreas( config )

        addPolygon( enumArea )

        vertexMarkers.map {
            it.remove()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.DefineEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onMapReady(googleMap: GoogleMap)
    {
        map = googleMap

        map.clear()

        map.setOnMapClickListener {
            if (createMode)
            {
                val marker = map.addMarker( MarkerOptions().position(it))
                marker?.let {
                    vertexMarkers.add( marker )
                }
            }
        }
        for (enumArea in config.enumAreas)
        {
            addPolygon( enumArea )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom( enumArea.vertices[0].toLatLng(), 14.0f ))

            for (location in enumArea.locations)
            {
                if (location.isLandmark)
                {
                    val icon = BitmapDescriptorFactory.fromResource(R.drawable.location_blue)

                    map.addMarker( MarkerOptions()
                        .position( LatLng( location.latitude, location.longitude ))
                        .icon( icon )
                    )
                }
                else
                {
                    var icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)

                    var numComplete = 0

                    for (item in location.items)
                    {
                        val enumerationItem = item as EnumerationItem?
                        //enumerationItem?.let{enumerationItem ->
                        if(enumerationItem != null)
                        {

                            if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                            {
                                icon = BitmapDescriptorFactory.fromResource(R.drawable.home_red)
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
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.home_green)
                    }

                    map.addMarker( MarkerOptions()
                        .position( LatLng( location.latitude, location.longitude ))
                        .icon( icon )
                    )
                }
            }
        }
    }

    fun addPolygon( enumArea: EnumArea )
    {
        val points = ArrayList<LatLng>()

        enumArea.vertices.map {
            points.add( it.toLatLng())
        }

        val polygonOptions = PolygonOptions()
            .clickable(true)
            .addAll( points )

        val polygon = map.addPolygon( polygonOptions )
        polygon.tag = enumArea

        map.setOnPolygonClickListener {polygon ->
            val ea = polygon.tag as EnumArea
            ConfirmationDialog( activity, resources.getString(R.string.please_confirm),
                "${resources.getString(R.string.delete_enum_area_message)} ${ea.name}?",
                resources.getString(R.string.no), resources.getString(R.string.yes), polygon, this)
        }
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        val polygon = tag as Polygon
        val enumArea = polygon.tag as EnumArea
        DAO.enumAreaDAO.delete( enumArea )
        polygon.remove()

        onMapReady(map)
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
                        config.enumAreas.add(enumArea)
                        //DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea )
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

            for (enumArea in config.enumAreas)
            {
                val points1 = ArrayList<Coordinate>()

                enumArea.vertices.map {
                    points1.add( Coordinate( it.toLatLng().longitude, it.toLatLng().latitude ))
                }

                val geometryFactory = GeometryFactory()
                val geometry: Geometry = geometryFactory.createPolygon(points1.toTypedArray())

                val coordinate = Coordinate( point.coordinates.longitude, point.coordinates.latitude )
                val geometry1 = geometryFactory.createPoint( coordinate )
                if (geometry.contains( geometry1 ))
                {
                    val location = Location( LocationType.Enumeration, point.coordinates.latitude, point.coordinates.longitude, false )
                    DAO.locationDAO.createOrUpdateLocation( location, enumArea )

                    enumArea.locations.add( location )
//                    DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea, config )
                    break // found! assuming that it can only exist in a single EA, for now!
                }
            }
        }

        lifecycleScope.launch {
            onMapReady( map )
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}