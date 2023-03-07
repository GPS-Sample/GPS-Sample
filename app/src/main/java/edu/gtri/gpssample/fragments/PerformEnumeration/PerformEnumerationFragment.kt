package edu.gtri.gpssample.fragments.PerformEnumeration

import android.os.Bundle
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
import com.google.android.gms.maps.model.PolylineOptions
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.constants.Shape
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.RectangleDAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentDefineEnumerationAreaBinding
import edu.gtri.gpssample.databinding.FragmentPerformEnumerationBinding
import edu.gtri.gpssample.fragments.DefineEnumerationArea.DefineEnumerationAreaViewModel
import java.util.*

class PerformEnumerationFragment : Fragment(), OnMapReadyCallback
{
    private lateinit var team: Team
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var map: GoogleMap
    private lateinit var viewModel: DefineEnumerationAreaViewModel

    private var _binding: FragmentPerformEnumerationBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DefineEnumerationAreaViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentPerformEnumerationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        val study_uuid = arguments!!.getString(Keys.kStudy_uuid.toString(), "");

        if (study_uuid.isEmpty()) {
            Toast.makeText( activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.studyDAO.getStudy( study_uuid )?.let { study ->
            this.study = study
        }

        if (!this::study.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Study with id ${study_uuid} not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // required enumArea_uuid
        val enumArea_uuid = arguments!!.getString(Keys.kEnumArea_uuid.toString(), "");

        if (enumArea_uuid.isEmpty()) {
            Toast.makeText( activity!!.applicationContext, "Fatal! Missing required parameter: enumArea_uuid.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.enumAreaDAO.getEnumArea( enumArea_uuid )?.let {
            this.enumArea = it
        }

        if (!this::enumArea.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! EnumArea with id ${enumArea_uuid} not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // required team_uuid
        val team_uuid = arguments!!.getString(Keys.kTeam_uuid.toString(), "");

        if (team_uuid.isEmpty()) {
            Toast.makeText( activity!!.applicationContext, "Fatal! Missing required parameter: team_uuid.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.teamDAO.getTeam( team_uuid )?.let {
            this.team = it
        }

        if (!this::team.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Team with id ${team_uuid} not found.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.titleTextView.text = enumArea.name + " (" + team.name + ")"

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        binding.addHouseholdButton.setOnClickListener {

            val bundle = Bundle()
            bundle.putString( Keys.kStudy_uuid.toString(), study_uuid )
            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment, bundle)
        }

        binding.addLocationButton.setOnClickListener {

            findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
        }

        binding.finishButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

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
                val latLng = getCenter( rectangle )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom( latLng, 16.0f))
            }
        }
    }

    fun getCenter( rectangle: Rectangle ) : LatLng
    {
        var sumLat = rectangle.topLeft_lat + rectangle.topRight_lat + rectangle.botRight_lat + rectangle.botLeft_lat
        var sumLon = rectangle.topLeft_lon + rectangle.topRight_lon + rectangle.botRight_lon + rectangle.botLeft_lon

        return LatLng( sumLat/4.0, sumLon/4.0 )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}