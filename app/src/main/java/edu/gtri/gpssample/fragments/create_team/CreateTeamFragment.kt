package edu.gtri.gpssample.fragments.create_team

import android.content.res.ColorStateList
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
import com.google.android.gms.maps.model.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateTeamBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

class CreateTeamFragment : Fragment(), OnMapReadyCallback, ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var map: GoogleMap
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var defaultColorList: ColorStateList
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var createMode = false
    private var selectionPolygon: Polygon? = null
    private var householdMarkers = ArrayList<Marker>()
    private var selectedHouseholdMarkers = ArrayList<Marker>()
    private var vertexMarkers = java.util.ArrayList<Marker>()
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

        sharedViewModel.createStudyModel.currentStudy?.value?.let {_study ->
            study = _study
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { _enumArea ->
            enumArea = _enumArea
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        binding.dropPinButton.backgroundTintList?.let {
            defaultColorList = it
        }

        binding.dropPinButton.setOnClickListener {

            if (createMode)
            {
                createMode = false
                clearSelections()
            }
            else
            {
                if (selectedHouseholdMarkers.isNotEmpty())
                {
                    Toast.makeText(activity!!.applicationContext, "You must clear the current selections before you can assign new households", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    clearSelections()
                    createMode = true
                    binding.dropPinButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                }
            }
        }

        binding.drawPolygonButton.setOnClickListener {

            if (createMode && vertexMarkers.size > 2)
            {
                createMode = false
                binding.dropPinButton.setBackgroundTintList(defaultColorList);

                val points1 = ArrayList<Coordinate>()
                val points2 = ArrayList<Coordinate>()

                enumArea.vertices.map {
                    points1.add( Coordinate( it.toLatLng().longitude, it.toLatLng().latitude ))
                }

                // close the poly
                points1.add( points1[0] )

                vertexMarkers.map { marker ->
                    points2.add( Coordinate(marker.position.longitude, marker.position.latitude))
                    marker.remove()
                }

                vertexMarkers.clear()

                // close the poly
                points2.add( points2[0] )

                val geometryFactory = GeometryFactory()
                val geometry1: Geometry = geometryFactory.createPolygon(points1.toTypedArray())
                val geometry2: Geometry = geometryFactory.createPolygon(points2.toTypedArray())

                geometry1.intersection(geometry2)?.let { geometry ->
                    val vertices = ArrayList<LatLng>()

                    geometry.boundary?.coordinates?.map {
                        vertices.add( LatLng( it.y, it.x ))
                    }

                    if (vertices.isNotEmpty())
                    {
                        val polygonOptions = PolygonOptions()
                            .fillColor(0x40ff0000.toInt())
                            .clickable(false)
                            .addAll( vertices )

                        selectionPolygon = map.addPolygon( polygonOptions )
                    }
                }
            }
        }

        binding.assignHouseholdsButton.setOnClickListener {

            selectionPolygon?.let {

                it.remove()
                selectionPolygon = null

                val vertices = java.util.ArrayList<LatLng>()

                vertexMarkers.map {
                    vertices.add( it.position )
                    it.remove()
                }

                val latLngBounds = vertices.fold ( LatLngBounds.builder(), { builder, it -> builder.include(it) } ).build()

                householdMarkers.map { marker ->

                    var icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)

                    if (latLngBounds.contains( marker.position ))
                    {
                        selectedHouseholdMarkers.add( marker )
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.home_green)
                    }

                    marker.setIcon( icon )
                }
            }
        }

        binding.clearSelectionsButton.setOnClickListener {
            ConfirmationDialog( activity, "Please Confirm", "Are you sure you want clear all selections?", "No", "Yes", null, this)
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            if (binding.teamNameEditText.text.toString().length == 0)
            {
                Toast.makeText(activity!!.applicationContext, "You must enter a team name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            study.id?.let { studyId ->
                enumArea.id?.let { enumAreaId ->
                    val team = DAO.teamDAO.createOrUpdateTeam( Team( studyId, enumAreaId, binding.teamNameEditText.text.toString()))

                    team?.id?.let { team_id ->

                        selectedHouseholdMarkers.map { marker ->
                            val enumData = marker.tag as EnumData
                            enumData.teamId = team_id
                            DAO.enumDataDAO.updateEnumData( enumData )
                        }
                    }

                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateTeamFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun clearSelections()
    {
        createMode = false

        householdMarkers.map { marker ->
            marker.setIcon( BitmapDescriptorFactory.fromResource(R.drawable.home_black) )
        }

        selectedHouseholdMarkers.clear()

        selectionPolygon?.let {
            it.remove()
            selectionPolygon = null
        }

        vertexMarkers.map {
            it.remove()
        }

        vertexMarkers.clear()

        binding.dropPinButton.setBackgroundTintList(defaultColorList);
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        clearSelections()
    }

    var once = true

    override fun onMapReady(googleMap: GoogleMap) {
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

        householdMarkers.clear()

        for (enumData in enumDataList)
        {
            if (!enumData.isLocation)
            {
                val icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)

                val marker = map.addMarker( MarkerOptions()
                    .position( LatLng( enumData.latitude, enumData.longitude ))
                    .icon( icon )
                )

                marker?.let {marker ->
                    marker.tag = enumData
                    householdMarkers.add( marker)
                }
            }
        }
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