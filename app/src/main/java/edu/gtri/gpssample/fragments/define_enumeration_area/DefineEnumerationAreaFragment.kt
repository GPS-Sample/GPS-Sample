package edu.gtri.gpssample.fragments.define_enumeration_area

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
import java.util.*

class DefineEnumerationAreaFragment : Fragment(), OnMapReadyCallback, ConfirmationDialog.ConfirmationDialogDelegate, InputDialog.InputDialogDelegate
{
    private lateinit var config: Config
    private lateinit var map: GoogleMap
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var createMode = false
    private var notificationShown = false
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

        sharedViewModel.currentConfiguration?.value.let {
            this.config = it!!
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Config not found.", Toast.LENGTH_SHORT).show()
            return
        }

        arguments?.let {
            if (!it.getBoolean( Keys.kEditMode.toString(), true))
            {
                binding.createSaveButton.visibility = View.GONE
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        binding.createSaveButton.setOnClickListener {

            if (createMode)
            {
                if (vertexMarkers.size > 2)
                {
                    InputDialog( activity!!, "Enter Enumeration Area Name", null, this )
                }
            }
            else
            {
                if (!notificationShown)
                {
                    notificationShown = true
                    NotificationDialog( activity, "Note!", "Create a polygon by tapping on the screen to drop markers. Markers will need to be created starting from the top left most vertex and proceeding clockwise.")
                }

                vertexMarkers.clear()
                createMode = true
                binding.createSaveButton.text = "Save Polygon"
            }
        }
    }

    override fun didEnterText( name: String )
    {
        createEnumArea( name )
    }

    fun createEnumArea( name: String )
    {
        createMode = false
        binding.createSaveButton.text = "Create Polygon"

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

        val enumAreas = DAO.enumAreaDAO.getEnumAreas( config )

        for (enumArea in enumAreas)
        {
            addPolygon( enumArea )

            if (user!!.role == Role.Supervisor.toString())
            {
                val latLng = getCenter( enumArea )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom( latLng, 17.0f))
            }

            val enumDataList = DAO.enumDataDAO.getEnumData(enumArea)

            for (enumData in enumDataList)
            {
                var icon = BitmapDescriptorFactory.fromResource(R.drawable.home_red)

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
            }
        }

        if (user!!.role == Role.Admin.toString())
        {
            val atl = LatLng( 33.774881, -84.396341 )
            val srb = LatLng(30.330603,-86.165004 )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom( atl, 15.0f))
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
            ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this Enumeration Area?", polygon, this)
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

    override fun didAnswerNo() {
    }

    override fun didAnswerYes( tag: Any? )
    {
        val polygon = tag as Polygon
        val enumArea = polygon.tag as EnumArea
        DAO.enumAreaDAO.delete( enumArea )
        polygon.remove()

        enumDataMarkers.map {
            it.remove()
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}