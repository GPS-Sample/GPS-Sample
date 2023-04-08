package edu.gtri.gpssample.fragments.defineenumeration

import android.os.Bundle
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
//            sharedViewModel.addEnumerationAreas(enumAreas  )

            findNavController().popBackStack()

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

        val user = (activity!!.application as? MainApplication)?.user

        config.id?.let {id ->
            enumAreas = DAO.enumAreaDAO.getEnumAreas( id )

            if (user!!.role == Role.Admin.toString() && enumAreas!!.isEmpty())
            {
                createTestAreas()
            }

            enumAreas = DAO.enumAreaDAO.getEnumAreas( id )
        }

        enumAreas?.let {enumAreas->
            for (enumArea in enumAreas)
            {
                googleMap.addPolyline(
                    PolylineOptions()
                        .clickable(true)
                        .add(
                            LatLng( enumArea.topLeft.latitude, enumArea.topLeft.longitude ),
                            LatLng( enumArea.topRight.latitude, enumArea.topRight.longitude ),
                            LatLng( enumArea.botRight.latitude, enumArea.botRight.longitude ),
                            LatLng( enumArea.botLeft.latitude, enumArea.botLeft.longitude ),
                            LatLng( enumArea.topLeft.latitude, enumArea.topLeft.longitude ),
                        )
                )

                if (user!!.role == Role.Supervisor.toString())
                {
                    val latLng = getCenter( enumArea )
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom( latLng, 17.0f))
                }
            }

            if (user!!.role == Role.Admin.toString())
            {
                val srb = LatLng(30.330603,-86.165004 )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom( srb, 15.0f))
            }
        }
    }

    fun getCenter( enumArea: EnumArea ) : LatLng
    {
        var sumLat = enumArea.topLeft.latitude + enumArea.topRight.latitude + enumArea.botRight.latitude + enumArea.botLeft.latitude
        var sumLon = enumArea.topLeft.longitude + enumArea.topRight.longitude + enumArea.botRight.longitude + enumArea.botLeft.longitude

        return LatLng( sumLat/4.0, sumLon/4.0 )
    }

    fun createTestAreas()
    {
        var topLeft = LatLng( 30.343716828800115, -86.16672319932157 )
        var topRight = LatLng( 30.343716828800115, -86.1627468263619 )
        var botRight = LatLng( 30.3389857458543, -86.1627468263619 )
        var botLeft = LatLng( 30.3389857458543, -86.16662109919962 )

        var enumArea = EnumArea( config.id!!, "EA NORTH", topLeft, topRight, botRight, botLeft )
        DAO.enumAreaDAO.createEnumArea( enumArea )

        topLeft = LatLng( 30.332241965564545, -86.16700455200723 )
        topRight = LatLng( 30.332241965564545, -86.16187525250082 )
        botRight = LatLng( 30.328614485534533, -86.16187525250082 )
        botLeft = LatLng( 30.328614485534533, -86.16700455200723 )

        enumArea = EnumArea( config.id!!, "EA SOUTH", topLeft, topRight, botRight, botLeft )
        DAO.enumAreaDAO.createEnumArea( enumArea )

        topLeft = LatLng( 30.337854506153278, -86.1716915111437 )
        topRight = LatLng( 30.337854506153278, -86.16865136128406 )
        botRight = LatLng( 30.33380130566348, -86.16865136128406 )
        botLeft = LatLng( 30.33380130566348, -86.171641010582 )

        enumArea = EnumArea( config.id!!, "EA WEST", topLeft, topRight, botRight, botLeft )
        DAO.enumAreaDAO.createEnumArea( enumArea )

        topLeft = LatLng( 30.33747998467289, -86.16498340055223 )
        topRight = LatLng( 30.33747998467289, -86.16076498381355 )
        botRight = LatLng( 30.334410467082552, -86.16076498381355 )
        botLeft = LatLng( 30.334410467082552, -86.16498340055223 )

        enumArea = EnumArea( config.id!!, "EA EAST", topLeft, topRight, botRight, botLeft )
        DAO.enumAreaDAO.createEnumArea( enumArea )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}