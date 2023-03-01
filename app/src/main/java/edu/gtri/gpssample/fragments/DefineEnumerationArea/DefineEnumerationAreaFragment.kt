package edu.gtri.gpssample.fragments.DefineEnumerationArea

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.EnumAreaDAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Coordinate
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.databinding.FragmentDefineEnumerationAreaBinding
import java.util.*

class DefineEnumerationAreaFragment : Fragment(), OnMapReadyCallback
{
    private lateinit var config: Config
    private var quickStart = false
    private var _binding: FragmentDefineEnumerationAreaBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: GoogleMap
    private lateinit var viewModel: DefineEnumerationAreaViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DefineEnumerationAreaViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentDefineEnumerationAreaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        // required: configId
        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        val config_uuid = arguments!!.getString( Key.kConfig_uuid.toString(), "");

        if (config_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.configDAO.getConfig( config_uuid )?.let { config ->
            this.config = config
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Config with id $config_uuid not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val quick_start = arguments?.getBoolean( Key.kQuickStart.toString(), false )

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
            if (quickStart)
            {
                val bundle = Bundle()
                bundle.putBoolean( Key.kQuickStart.toString(), quickStart )
                bundle.putString( Key.kConfig_uuid.toString(), config_uuid )
                findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
            }
            else
            {
                findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        var enumAreas = DAO.enumAreaDAO.getEnumAreas(config.uuid)

        if (enumAreas.isEmpty())
        {
            createTestAreas()
        }

        enumAreas = DAO.enumAreaDAO.getEnumAreas()

        for (enumArea in enumAreas)
        {
            val tl = DAO.coordinateDAO.getCoordinate( enumArea.topLeft_uuid )
            val tr = DAO.coordinateDAO.getCoordinate( enumArea.topRight_uuid )
            val br = DAO.coordinateDAO.getCoordinate( enumArea.botRight_uuid )
            val bl = DAO.coordinateDAO.getCoordinate( enumArea.botLeft_uuid )

            googleMap.addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .add(
                        LatLng( tl!!.lat, tl!!.lon ),
                        LatLng( tr!!.lat, tr!!.lon ),
                        LatLng( br!!.lat, br!!.lon ),
                        LatLng( bl!!.lat, bl!!.lon ),
                        LatLng( tl!!.lat, tl!!.lon ),
                    ))
        }

        val srb = LatLng(30.330603,-86.165004 )

        map.moveCamera(CameraUpdateFactory.newLatLngZoom( srb, 15.0f))
    }

    fun createTestAreas()
    {
        var topLeft = Coordinate(UUID.randomUUID().toString(), 30.343716828800115, -86.16672319932157 )
        var topRight = Coordinate(UUID.randomUUID().toString(), 30.343716828800115, -86.1627468263619 )
        var botRight = Coordinate(UUID.randomUUID().toString(), 30.3389857458543, -86.1627468263619 )
        var botLeft = Coordinate(UUID.randomUUID().toString(), 30.3389857458543, -86.16662109919962 )

        DAO.coordinateDAO.createCoordinate( topLeft )
        DAO.coordinateDAO.createCoordinate( topRight )
        DAO.coordinateDAO.createCoordinate( botRight )
        DAO.coordinateDAO.createCoordinate( botLeft )

        var enumArea = EnumArea( UUID.randomUUID().toString(), config.uuid, "EA NORTH", topLeft.uuid, topRight.uuid, botRight.uuid, botLeft.uuid )
        DAO.enumAreaDAO.createEnumArea( enumArea )

        topLeft = Coordinate(UUID.randomUUID().toString(), 30.332241965564545, -86.16700455200723 )
        topRight = Coordinate(UUID.randomUUID().toString(),  30.332241965564545, -86.16187525250082 )
        botRight = Coordinate(UUID.randomUUID().toString(), 30.328614485534533, -86.16187525250082 )
        botLeft = Coordinate(UUID.randomUUID().toString(), 30.328614485534533, -86.16700455200723 )

        DAO.coordinateDAO.createCoordinate( topLeft )
        DAO.coordinateDAO.createCoordinate( topRight )
        DAO.coordinateDAO.createCoordinate( botRight )
        DAO.coordinateDAO.createCoordinate( botLeft )

        enumArea = EnumArea( UUID.randomUUID().toString(), config.uuid, "EA SOUTH", topLeft.uuid, topRight.uuid, botRight.uuid, botLeft.uuid )
        DAO.enumAreaDAO.createEnumArea( enumArea )

        topLeft = Coordinate(UUID.randomUUID().toString(), 30.337854506153278, -86.1716915111437 )
        topRight = Coordinate(UUID.randomUUID().toString(), 30.337854506153278, -86.16865136128406 )
        botRight = Coordinate(UUID.randomUUID().toString(), 30.33380130566348, -86.16865136128406 )
        botLeft = Coordinate(UUID.randomUUID().toString(), 30.33380130566348, -86.171641010582 )

        DAO.coordinateDAO.createCoordinate( topLeft )
        DAO.coordinateDAO.createCoordinate( topRight )
        DAO.coordinateDAO.createCoordinate( botRight )
        DAO.coordinateDAO.createCoordinate( botLeft )

        enumArea = EnumArea( UUID.randomUUID().toString(), config.uuid, "EA WEST", topLeft.uuid, topRight.uuid, botRight.uuid, botLeft.uuid )
        DAO.enumAreaDAO.createEnumArea( enumArea )

        topLeft = Coordinate(UUID.randomUUID().toString(), 30.33747998467289, -86.16498340055223 )
        topRight = Coordinate(UUID.randomUUID().toString(), 30.33747998467289, -86.16076498381355 )
        botRight = Coordinate(UUID.randomUUID().toString(), 30.334410467082552, -86.16076498381355 )
        botLeft = Coordinate(UUID.randomUUID().toString(), 30.334410467082552, -86.16498340055223 )

        DAO.coordinateDAO.createCoordinate( topLeft )
        DAO.coordinateDAO.createCoordinate( topRight )
        DAO.coordinateDAO.createCoordinate( botRight )
        DAO.coordinateDAO.createCoordinate( botLeft )

        enumArea = EnumArea( UUID.randomUUID().toString(), config.uuid, "EA EAST", topLeft.uuid, topRight.uuid, botRight.uuid, botLeft.uuid )
        DAO.enumAreaDAO.createEnumArea( enumArea )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}