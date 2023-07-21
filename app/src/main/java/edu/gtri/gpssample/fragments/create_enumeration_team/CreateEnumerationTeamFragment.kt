package edu.gtri.gpssample.fragments.create_enumeration_team

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
import edu.gtri.gpssample.databinding.FragmentCreateEnumerationTeamBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

class CreateEnumerationTeamFragment : Fragment(), OnMapReadyCallback, ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var map: GoogleMap
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var defaultColorList: ColorStateList
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var createMode = false
    private var selectionGeometry: Geometry? = null
    private var selectionPolygon: Polygon? = null
    private var selectionMarkers = ArrayList<Marker>()
    private var _binding: FragmentCreateEnumerationTeamBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateEnumerationTeamBinding.inflate(inflater, container, false)
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
                clearSelections()
                createMode = true
                binding.dropPinButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
        }

        binding.drawPolygonButton.setOnClickListener {

            if (createMode && selectionMarkers.size > 2)
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

                selectionMarkers.map { marker ->
                    points2.add( Coordinate(marker.position.longitude, marker.position.latitude))
                    marker.remove()
                }

                selectionMarkers.clear()

                // close the poly
                points2.add( points2[0] )

                val geometryFactory = GeometryFactory()
                val geometry1: Geometry = geometryFactory.createPolygon(points1.toTypedArray())
                val geometry2: Geometry = geometryFactory.createPolygon(points2.toTypedArray())

                geometry1.intersection(geometry2)?.let { polygon ->
                    val vertices = ArrayList<LatLng>()

                    selectionGeometry = polygon

                    polygon.boundary?.coordinates?.map {
                        vertices.add( LatLng( it.y, it.x ))  // latitude is th Y axis in JTS
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

                    val polygon = ArrayList<LatLon>()

                    selectionPolygon?.points?.map {
                        polygon.add( LatLon( it.latitude, it.longitude ))
                    }

                    val team = DAO.teamDAO.createOrUpdateTeam( Team( studyId, enumAreaId, binding.teamNameEditText.text.toString(), true, polygon ))

                    team?.id?.let { teamId ->

                        for (location in enumArea.locations)
                        {
                            selectionGeometry?.let {
                                val point = GeometryFactory().createPoint( Coordinate( location.longitude, location.latitude ))
                                if (it.contains( point ))
                                {
//                                    location.enumerationTeamId = teamId
//                                    DAO.locationDAO.updateLocation( location )
                                }
                            }
                        }

                        // refresh the shared config
//                        val config = DAO.configDAO.getConfig( enumArea.configId )
//                        config?.let {
//                            sharedViewModel.setCurrentConfig( it )
//                        }
                    }

                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateEnumerationTeamFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun clearSelections()
    {
        createMode = false

        selectionPolygon?.let {
            it.remove()
            selectionPolygon = null
        }

        selectionMarkers.map {
            it.remove()
        }

        selectionMarkers.clear()

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

    override fun onMapReady(googleMap: GoogleMap)
    {
        map = googleMap

        map.clear()

        map.setOnMapClickListener {
            if (createMode)
            {
                val marker = map.addMarker( MarkerOptions().position(it))
                marker?.let {
                    selectionMarkers.add( marker )
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

        if (once)
        {
            once = false
            val latLng = getCenter()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom( latLng, 14.0f))
        }

        val locations = DAO.locationDAO.getLocations( enumArea )

        for (location in locations)
        {
            if (!location.isLandmark)
            {
                val icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)

                map.addMarker( MarkerOptions()
                    .position( LatLng( location.latitude, location.longitude ))
                    .icon( icon )
                )
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