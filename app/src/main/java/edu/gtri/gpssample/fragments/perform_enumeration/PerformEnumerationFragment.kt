package edu.gtri.gpssample.fragments.perform_enumeration

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
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformEnumerationBinding
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class PerformEnumerationFragment : Fragment(), OnMapReadyCallback
{
    private lateinit var team: Team
    private lateinit var map: GoogleMap
    private lateinit var enumArea: EnumArea
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var userId = 0
    private var enumAreaId = 0
    private var dropMode = false
    private var location: LatLng? = null
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

        binding.titleTextView.text = enumArea.name + " (" + team.name + ")"

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
                var enumData = EnumData(userId, enumAreaId, location.latitude, location.longitude)
                sharedViewModel.enumDataViewModel.setCurrentEnumData(enumData)

                findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
            }

            location = null
        }

        binding.addLocationButton.setOnClickListener {

            location?.let { location ->
                var enumData = EnumData(userId, enumAreaId, location.latitude, location.longitude)
                enumData.isLocation = true
                sharedViewModel.enumDataViewModel.setCurrentEnumData(enumData)

                findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
            }

            location = null
        }

        binding.finishButton.setOnClickListener {

            findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName

        if (this::map.isInitialized)
        {
            addMapObjects()
        }
    }

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

        val latLng = getCenter()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom( latLng, 16.0f))

        val enumDataList = DAO.enumDataDAO.getEnumData(userId, enumAreaId)

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

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}