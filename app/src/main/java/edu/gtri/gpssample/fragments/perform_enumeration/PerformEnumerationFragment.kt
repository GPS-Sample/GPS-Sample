package edu.gtri.gpssample.fragments.perform_enumeration

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Environment
import android.renderscript.ScriptGroup.Input
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformEnumerationBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.fragments.manageconfigurations.ManageConfigurationsAdapter
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList

class PerformEnumerationFragment : Fragment(),
    OnMapReadyCallback,
    InputDialog.InputDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var team: Team
    private lateinit var map: GoogleMap
    private lateinit var enumArea: EnumArea
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var performEnumerationAdapter: PerformEnumerationAdapter

    private var userId = 0
    private var enumAreaId = 0
    private var dropMode = false
    private var location: LatLng? = null
    private var enumDataList = ArrayList<EnumData>()
    private var _binding: FragmentPerformEnumerationBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentPerformEnumerationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enum_area ->
            enumArea = enum_area
            enumArea.id?.let {id ->
                enumAreaId = id
            }
        }

        sharedViewModel.teamViewModel.currentTeam?.value?.let {
            team = it
        }

        val user = (activity!!.application as? MainApplication)?.user

        user?.id?.let {
            userId = it
        }

        performEnumerationAdapter = PerformEnumerationAdapter( enumDataList )
        performEnumerationAdapter.didSelectEnumData = this::didSelectEnumData

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performEnumerationAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.titleTextView.text =  "Configuration " + enumArea.name + " (" + team.name + " team)"

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        val defaultColorList = binding.dropPinButton.backgroundTintList

        binding.dropPinButton.setOnClickListener {

            if (!dropMode)
            {
                dropMode = true
                location = null
                addMapObjects()
                binding.dropPinButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));

                map.setOnMapClickListener {
                    if (dropMode)
                    {
                        location = it
                        map.addMarker(MarkerOptions().position(it))
                        dropMode = false
                        binding.dropPinButton.setBackgroundTintList(defaultColorList);
                    }
                }
            }
            else
            {
                dropMode = false
                binding.dropPinButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.addHouseholdButton.setOnClickListener {

            location?.let { location ->
                var enumData = EnumData(userId, enumAreaId, false, false, "", "", location.latitude, location.longitude)
                sharedViewModel.enumDataViewModel.setCurrentEnumData(enumData)

                findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
            } ?: kotlin.run {
                Toast.makeText(activity!!.applicationContext, "Location not found", Toast.LENGTH_SHORT).show()
            }

            location = null
        }

        binding.addLocationButton.setOnClickListener {

            location?.let { location ->
                var enumData = EnumData(userId, enumAreaId, false, false, "", "", location.latitude, location.longitude)
                enumData.isLocation = true
                sharedViewModel.enumDataViewModel.setCurrentEnumData(enumData)

                findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
            } ?: kotlin.run {
                Toast.makeText(activity!!.applicationContext, "Location not found", Toast.LENGTH_SHORT).show()
            }

            location = null
        }

        binding.exportButton.setOnClickListener {
            ConfirmationDialog( activity, "Select Upload Content", "", "Config Data", "Enum Data", team, this)
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName

        enumDataList = DAO.enumDataDAO.getEnumData(enumArea, team)

        performEnumerationAdapter.updateEnumDataList( enumDataList )
        if (this::map.isInitialized)
        {
            addMapObjects()
        }
    }

    var once = true

    fun addMapObjects()
    {
        map.clear()

        val points = ArrayList<LatLng>()

        enumArea.vertices.map {
            points.add( it.toLatLng())
        }

        val polygon = PolygonOptions()
            .clickable(false)
            .addAll( points )

        map.addPolygon(polygon)

        if (once)
        {
            once = false
            val latLng = getCenter()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom( latLng, 14.0f))
        }

        for (enumData in enumDataList)
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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        addMapObjects()
    }

    fun getCenter() : LatLng
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

    private fun didSelectEnumData( enumData: EnumData )
    {
        sharedViewModel.enumDataViewModel.setCurrentEnumData(enumData)

        if (enumData.isLocation)
        {
            findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
        }
        else
        {
            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
        }
    }

    private val kConfigExport = 1
    private val kEnumExport = 2

    override fun didSelectLeftButton(tag: Any?)
    {
        InputDialog( activity!!, "Enter a file name for the export", "", kConfigExport, this@PerformEnumerationFragment )
    }

    override fun didSelectRightButton(tag: Any?)
    {
        InputDialog( activity!!, "Enter a file name for the export", "", kEnumExport, this@PerformEnumerationFragment )
    }

    override fun didEnterText( name: String, tag: Any? )
    {
        val t = tag as Int

        if (t == kEnumExport)
        {
            // Sync the enumeration data back to the admin.
            // For this scenario, we're trying to export the enumeration data only,
            // not the entire configuration.
            // On the admin side, the app will need to be able to add
            // the enumeration data only, not the entire EnumArea container.
            // EnumData ids on the admin side will need to be autogenerated
            // during the import to accommodate uploads from multiple enumerators.
            // We'll also need to handle duplicate updates from the same enumerator.

            enumArea.enumDataList = DAO.enumDataDAO.getEnumData(enumArea,team)
            val packedEnumArea = enumArea.pack()
            Log.d( "xxx", packedEnumArea )

            val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
            val file = File(root, "$name.${Date().time}.json")
            val writer = FileWriter(file)
            writer.append(packedEnumArea)
            writer.flush()
            writer.close()

            Toast.makeText(activity!!.applicationContext, "Enumeration data has been saved to the Documents directory.", Toast.LENGTH_SHORT).show()
        }
        else
        {
            sharedViewModel.currentConfiguration?.value?.let { config ->
                DAO.configDAO.updateAllLists( config )

                team.id?.let {
                    config.teamId = it
                }

                val packedConfig = config.pack()
                Log.d( "xxx", packedConfig )

                config.teamId = 0

                val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
                val file = File(root, "$name.${Date().time}.json")
                val writer = FileWriter(file)
                writer.append(packedConfig)
                writer.flush()
                writer.close()

                Toast.makeText(activity!!.applicationContext, "The configuration has been saved to the Documents directory.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}