package edu.gtri.gpssample.fragments.DefineEnumerationArea

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
import com.google.android.gms.maps.model.MarkerOptions
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.databinding.FragmentDefineEnumerationAreaBinding
import edu.gtri.gpssample.fragments.AdminSelectRole.AdminSelectRoleViewModel

class DefineEnumerationAreaFragment : Fragment(), OnMapReadyCallback
{
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
                Toast.makeText(activity!!.applicationContext, "DefineEnumerationAreaFragment", Toast.LENGTH_SHORT).show()
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        binding.backButton.setOnClickListener {

            findNavController().popBackStack()
        }

        binding.nextButton.setOnClickListener {

            findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val srb = LatLng(30.330603,-86.165004 )

        map.addMarker( MarkerOptions().position(srb).title("Grayton Beach, FL"))

        map.moveCamera(CameraUpdateFactory.newLatLngZoom( srb, 15.0f))
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}