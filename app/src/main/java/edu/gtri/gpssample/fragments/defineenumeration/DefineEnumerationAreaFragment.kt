package edu.gtri.gpssample.fragments.defineenumeration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.constants.Shape
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Rectangle
import edu.gtri.gpssample.databinding.FragmentDefineEnumerationAreaBinding
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

class DefineEnumerationAreaFragment : Fragment(), OnMapReadyCallback
{
    private lateinit var config: Config
    private var quickStart = false
    private var _binding: FragmentDefineEnumerationAreaBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: GoogleMap
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var enumAreas : List<EnumArea>? = null
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
        // required: configId
        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        val config_uuid = arguments!!.getString( Keys.kConfig_uuid.toString(), "");

        if (config_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        sharedViewModel.currentConfiguration?.value.let {
            this.config = it!!
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Config with id $config_uuid not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val quick_start = arguments?.getBoolean( Keys.kQuickStart.toString(), false )

        quick_start?.let {
            quickStart = it
        }

        if (quickStart)
        {
            binding.saveButton.setText( "NEXT" )
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        binding.saveButton.setOnClickListener {

            // TODO: add enumeration area to current configuration
            sharedViewModel.addEnumerationAreas(enumAreas  )
            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
//            sharedViewModel.saveNewConfiguration()
//            if (quickStart)
//            {
//                val bundle = Bundle()
//                bundle.putBoolean( Keys.kQuickStart.toString(), quickStart )
//                bundle.putString( Keys.kConfig_uuid.toString(), config_uuid )
//
//                findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
//            }
//            else
//            {
//                findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
//            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.DefineEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        enumAreas = DAO.enumAreaDAO.getEnumAreas(config.uuid)

        val user = (activity!!.application as? MainApplication)?.user

        if (user!!.role == Role.Admin.toString() && enumAreas!!.isEmpty())
        {
            createTestAreas()
        }

        enumAreas = DAO.enumAreaDAO.getEnumAreas()
        enumAreas?.let {enumAreas->
            for (enumArea in enumAreas)
            {
                if (enumArea.shape == Shape.Rectangle.toString())
                {
                    val rectangle = DAO.rectangleDAO.getRectangle( enumArea.shape_uuid )

                    rectangle?.let { rectangle
                        googleMap.addPolyline(
                            PolylineOptions()
                                .clickable(true)
                                .add(
                                    LatLng( rectangle.topLeft_lat, rectangle.topLeft_lon ),
                                    LatLng( rectangle.topRight_lat, rectangle.topRight_lon ),
                                    LatLng( rectangle.botRight_lat, rectangle.botRight_lon ),
                                    LatLng( rectangle.botLeft_lat, rectangle.botLeft_lon ),
                                    LatLng( rectangle.topLeft_lat, rectangle.topLeft_lon ),
                                ))
                        if (user!!.role == Role.Supervisor.toString())
                        {
                            val latLng = getCenter( rectangle )
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom( latLng, 17.0f))
                        }
                    }
                }
            }

            if (user!!.role == Role.Admin.toString())
            {
                val srb = LatLng(30.330603,-86.165004 )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom( srb, 15.0f))
            }
        }

    }

    fun getCenter( rectangle: Rectangle ) : LatLng
    {
        var sumLat = rectangle.topLeft_lat + rectangle.topRight_lat + rectangle.botRight_lat + rectangle.botLeft_lat
        var sumLon = rectangle.topLeft_lon + rectangle.topRight_lon + rectangle.botRight_lon + rectangle.botLeft_lon

        return LatLng( sumLat/4.0, sumLon/4.0 )
    }

    fun createTestAreas()
    {
        var topLeft = doubleArrayOf( 30.343716828800115, -86.16672319932157 )
        var topRight = doubleArrayOf( 30.343716828800115, -86.1627468263619 )
        var botRight = doubleArrayOf( 30.3389857458543, -86.1627468263619 )
        var botLeft = doubleArrayOf( 30.3389857458543, -86.16662109919962 )

        var rectangle = Rectangle( UUID.randomUUID().toString(),
            topLeft[0], topLeft[1],
            topRight[0], topRight[1],
            botRight[0], botRight[1],
            botLeft[0], botLeft[1])

        DAO.rectangleDAO.createRectangle( rectangle )

        var enumArea = EnumArea( UUID.randomUUID().toString(), config.uuid, "EA NORTH", Shape.Rectangle.toString(), rectangle.uuid )
        DAO.enumAreaDAO.createEnumArea( enumArea )

        topLeft = doubleArrayOf( 30.332241965564545, -86.16700455200723 )
        topRight = doubleArrayOf( 30.332241965564545, -86.16187525250082 )
        botRight = doubleArrayOf( 30.328614485534533, -86.16187525250082 )
        botLeft = doubleArrayOf( 30.328614485534533, -86.16700455200723 )

        rectangle = Rectangle( UUID.randomUUID().toString(),
            topLeft[0], topLeft[1],
            topRight[0], topRight[1],
            botRight[0], botRight[1],
            botLeft[0], botLeft[1])

        DAO.rectangleDAO.createRectangle( rectangle )

        enumArea = EnumArea( UUID.randomUUID().toString(), config.uuid, "EA SOUTH", Shape.Rectangle.toString(), rectangle.uuid )
        DAO.enumAreaDAO.createEnumArea( enumArea )

        topLeft = doubleArrayOf( 30.337854506153278, -86.1716915111437 )
        topRight = doubleArrayOf( 30.337854506153278, -86.16865136128406 )
        botRight = doubleArrayOf( 30.33380130566348, -86.16865136128406 )
        botLeft = doubleArrayOf( 30.33380130566348, -86.171641010582 )

        rectangle = Rectangle( UUID.randomUUID().toString(),
            topLeft[0], topLeft[1],
            topRight[0], topRight[1],
            botRight[0], botRight[1],
            botLeft[0], botLeft[1])

        DAO.rectangleDAO.createRectangle( rectangle )

        enumArea = EnumArea( UUID.randomUUID().toString(), config.uuid, "EA WEST", Shape.Rectangle.toString(), rectangle.uuid )
        DAO.enumAreaDAO.createEnumArea( enumArea )

        topLeft = doubleArrayOf( 30.33747998467289, -86.16498340055223 )
        topRight = doubleArrayOf( 30.33747998467289, -86.16076498381355 )
        botRight = doubleArrayOf( 30.334410467082552, -86.16076498381355 )
        botLeft = doubleArrayOf( 30.334410467082552, -86.16498340055223 )

        rectangle = Rectangle( UUID.randomUUID().toString(),
            topLeft[0], topLeft[1],
            topRight[0], topRight[1],
            botRight[0], botRight[1],
            botLeft[0], botLeft[1])

        DAO.rectangleDAO.createRectangle( rectangle )

        enumArea = EnumArea( UUID.randomUUID().toString(), config.uuid, "EA EAST", Shape.Rectangle.toString(), rectangle.uuid )
        DAO.enumAreaDAO.createEnumArea( enumArea )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}