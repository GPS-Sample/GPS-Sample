package edu.gtri.gpssample.fragments.define_enumeration_area

import android.app.Activity
import android.content.Intent
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
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentDefineEnumerationAreaBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.NotificationDialog
import edu.gtri.gpssample.fragments.createstudy.DeleteMode
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class DefineEnumerationAreaFragment : Fragment(), OnMapReadyCallback, ConfirmationDialog.ConfirmationDialogDelegate, InputDialog.InputDialogDelegate
{
    private lateinit var config: Config
    private lateinit var map: GoogleMap
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var configId: Int = 0
    private var createMode = false
    private var vertexMarkers = ArrayList<Marker>()
    private var enumDataMarkers = ArrayList<Marker>()
    private var _binding: FragmentDefineEnumerationAreaBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentDefineEnumerationAreaBinding.inflate(inflater, container, false)
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
            defineEnumerationAreaFragment = this@DefineEnumerationAreaFragment
        }

        sharedViewModel.currentConfiguration?.value.let { config ->
            this.config = config!!
            this.config.id?.let {
                configId = it
            }
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Config not found.", Toast.LENGTH_SHORT).show()
            return
        }

        arguments?.let {
            if (!it.getBoolean( Keys.kEditMode.toString(), true))
            {
//                binding.createSaveButton.visibility = View.GONE
            }
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
//        binding.createSaveButton.text = "Create Polygon"

        var vertices = ArrayList<LatLon>()

        vertexMarkers.map {
            vertices.add( LatLon( it.position.latitude, it.position.longitude ))
        }

        var enumArea = EnumArea( config.id!!, name, vertices )
        DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea )

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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.setOnMapClickListener {
            if (createMode)
            {
                val marker = map.addMarker( MarkerOptions().position(it))
                marker?.let {
                    vertexMarkers.add( marker )
                }
            }
        }

        val user = (activity!!.application as? MainApplication)?.user

        config.enumAreas = DAO.enumAreaDAO.getEnumAreas( config )

        for (enumArea in config.enumAreas)
        {
            addPolygon( enumArea )

//            if (user!!.role == Role.Supervisor.toString())
//            {
//                val latLng = getCenter( enumArea )
//                map.moveCamera(CameraUpdateFactory.newLatLngZoom( latLng, 17.0f))
//            }

            enumArea.enumDataList = DAO.enumDataDAO.getEnumData(enumArea)

            for (enumData in enumArea.enumDataList)
            {
                var icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)

                if (enumData.incomplete)
                {
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.home_red)
                }
                else if (enumData.valid)
                {
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.home_green)
                }

                if (enumData.isLocation)
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.location_blue)

                val marker = map.addMarker( MarkerOptions()
                    .position( LatLng( enumData.latitude, enumData.longitude ))
                    .icon( icon )
                )

                marker?.let {marker ->
                    marker.tag = enumData
                    enumDataMarkers.add( marker )
                }

                map.setOnMarkerClickListener { marker ->
                    marker.tag?.let {tag ->
                        val enum_data = tag as EnumData
                        sharedViewModel.enumDataViewModel.setCurrentEnumData(enum_data)

                        if (enum_data.isLocation)
                        {
                            findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
                        }
                        else
                        {
                            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
                        }
                    }

                    false
                }
            }
        }

//        if (user!!.role == Role.Admin.toString())
//        {
            val atl = LatLng( 33.774881, -84.396341 )
            val srb = LatLng(30.330603,-86.165004 )
            val demo = LatLng( 33.982973122594785, -84.31252665817738 )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom( demo, 14.0f))
//        }
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
            ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this Enumeration Area?", "No", "Yes", polygon, this)
        }
    }

    fun getCenter( enumArea: EnumArea ) : LatLng
    {
        var sumLat: Double = 0.0
        var sumLon: Double = 0.0

        for (latLon in enumArea.vertices)
        {
            sumLat += latLon.latitude
            sumLon += latLon.longitude
        }

        return LatLng( sumLat/enumArea.vertices.size, sumLon/enumArea.vertices.size )
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

        enumDataMarkers.map {
            it.remove()
        }
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
                    val inputStream = activity!!.getContentResolver().openInputStream(uri)

                    inputStream?.let {  inputStream ->
                        val text = inputStream.bufferedReader().readText()

                        Log.d( "xxx", text )

                        val jsonObject = JSONObject(text)

                        if (jsonObject.getString("type" ) == "FeatureCollection")
                        {
                            val jsonArray = jsonObject.getJSONArray("features" )

                            val enumArea = getPolygon( jsonArray )

                            enumArea?.let { enumArea ->
                                enumArea.id?.let {  enumAreaId ->
                                    enumArea.enumDataList = getPoints( jsonArray, enumAreaId )
                                }
                            }

                            onMapReady(map)
                        }
                    }
                }
                catch( ex: java.lang.Exception )
                {
                    Toast.makeText(activity!!.applicationContext, "Oops! The import failed.  Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getPolygon( jsonArray: JSONArray ) : EnumArea?
    {
        var enumArea : EnumArea? = null

        for (i in 0..jsonArray.length()-1)
        {
            val jsonObject = jsonArray.getJSONObject(i)
            val featureType = jsonObject.getString("type")

            if (featureType == "Feature")
            {
                val geometry = jsonObject.getJSONObject("geometry")

                if (geometry.getString("type") == "Polygon")
                {
                    var name: String = "undefined"

                    try {
                        val properties = jsonObject.getJSONObject("properties" )
                        name = properties.getString("name" )
                    }
                    catch( ex: java.lang.Exception) {
                    }

                    enumArea = EnumArea( configId, name, ArrayList<LatLon>())

                    val coordinates = geometry.getJSONArray("coordinates")
                    val coordinateArray = coordinates.getJSONArray(0 )

                    for (j in 0..coordinateArray.length()-1)
                    {
                        val coordinate = coordinateArray.getJSONArray(j)
                        val lat = coordinate[0] as Double
                        val lon = coordinate[1] as Double
                        val latLon = LatLon(lat, lon)
                        enumArea.vertices.add( latLon )
                    }

                    DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea )
                }
            }
        }

        return enumArea
    }

    fun getPoints( jsonArray: JSONArray, enumAreaId: Int ) : ArrayList<EnumData>
    {
        var enumDataList = ArrayList<EnumData>()

        for (i in 0..jsonArray.length()-1)
        {
            val jsonObject = jsonArray.getJSONObject(i)
            val featureType = jsonObject.getString("type")

            if (featureType == "Feature")
            {
                val geometry = jsonObject.getJSONObject("geometry")

                if (geometry.getString("type") == "Point")
                {
                    val coordinates = geometry.getJSONArray("coordinates")
                    val lat = coordinates[0] as Double
                    val lon = coordinates[1] as Double

                    val enumData = EnumData( -1, enumAreaId, false, false, "", "", lat, lon )
                    DAO.enumDataDAO.createOrUpdateEnumData( enumData )
                    enumDataList.add( enumData )
                }
            }
        }

        return enumDataList
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}