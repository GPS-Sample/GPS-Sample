package edu.gtri.gpssample.fragments.create_team

import android.content.ContentValues
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Environment
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
import edu.gtri.gpssample.databinding.FragmentCreateTeamBinding
import edu.gtri.gpssample.databinding.FragmentPerformEnumerationBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.fragments.manageconfigurations.ManageConfigurationsAdapter
import edu.gtri.gpssample.fragments.perform_enumeration.PerformEnumerationAdapter
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList

class CreateTeamFragment : Fragment(), OnMapReadyCallback
{
    private lateinit var team: Team
    private lateinit var map: GoogleMap
    private lateinit var enumArea: EnumArea
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var selectedEnumDataList = ArrayList<EnumData>()
    private var _binding: FragmentCreateTeamBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enum_area ->
            enumArea = enum_area
        }

        sharedViewModel.teamViewModel.currentTeam?.value?.let {
            team = it
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            if (selectedEnumDataList.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "You have not selected any households", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.teamNameEditText.text.toString().length == 0)
            {
                Toast.makeText(activity!!.applicationContext, "You must enter a team name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            enumArea.id?.let { enumAreaId ->
                Log.d( "xxx", selectedEnumDataList.toString())
                val team = Team( enumAreaId, binding.teamNameEditText.text.toString() )
                DAO.teamDAO.createOrUpdateTeam( team )
            }

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName
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

        val enumDataList = DAO.enumDataDAO.getEnumData( enumArea )

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
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.home_green))
                    selectedEnumDataList.add( enum_data )
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