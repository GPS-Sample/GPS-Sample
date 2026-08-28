/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.createconfiguration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Point
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentCreateConfigurationBinding
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.ui.compose.ComposableNotificationDialogHost
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.views.MapView
import java.util.UUID

class CreateConfigurationFragment : Fragment(), View.OnTouchListener
{
    private var _binding: FragmentCreateConfigurationBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var manageStudiesAdapter: ManageStudiesAdapter
    private lateinit var composableNotificationDialogHost: ComposableNotificationDialogHost

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createConfigurationFragment = this@CreateConfigurationFragment
        }

        composableNotificationDialogHost = ComposableNotificationDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableNotificationDialogHost.Content()
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.minGpsPrecisionTip.setOnClickListener {
            composableNotificationDialogHost.show(title = resources.getString(R.string.info), message = resources.getString(R.string.gpsaccuracy_hint))
        }

        binding.encryptionPasswordTip.setOnClickListener {
            composableNotificationDialogHost.show(title = resources.getString(R.string.info), message = resources.getString(R.string.encryption_hint))
        }

        binding.supervisorEditTip.setOnClickListener {
            composableNotificationDialogHost.show(title = resources.getString(R.string.info), message = resources.getString(R.string.supervisor_edit_tip))
        }

        binding.manualEntryTip.setOnClickListener {
            composableNotificationDialogHost.show(title = resources.getString(R.string.info), message = resources.getString(R.string.manual_hint))
        }

        binding.subaddressTip.setOnClickListener {
            composableNotificationDialogHost.show(title = resources.getString(R.string.info), message = resources.getString(R.string.subaddress_hint))
        }

        binding.autoIncrementTip.setOnClickListener {
            composableNotificationDialogHost.show(title = resources.getString(R.string.info), message = resources.getString(R.string.autoincrement_hint))
        }

        binding.proximityWarningHint.setOnClickListener {
            composableNotificationDialogHost.show(title = resources.getString(R.string.info), message = resources.getString(R.string.proximity_hint))
        }

        binding.geofenceHint.setOnClickListener {
            composableNotificationDialogHost.show(title = resources.getString(R.string.info), message = resources.getString(R.string.geofence_hint))
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        val items = ArrayList<String>()
        val mapEngines = resources.getTextArray( R.array.map_engines )

        for (mapEngine in mapEngines)
        {
            items.add( mapEngine.toString() )
        }

        binding.mapEngineSpinner.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, items )

        binding.mapEngineSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
            {
                // Note! OnItemSelected fires automatically when the fragment is created
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    config.mapEngineIndex = position
                    MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null ) { mapView ->
                        binding.osmLabel.visibility = if (mapView is MapView) View.VISIBLE else View.GONE

                        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        {
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null)
                                {
                                    val point = Point.fromLngLat( location.longitude, location.latitude )
                                    MapManager.instance().centerMap( point, mapView )
                                }
                            }
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.mapOverlayView.setOnTouchListener(this)

        sharedViewModel.currentConfiguration?.value?.let { config ->
            binding.mapEngineSpinner.setSelection(config.mapEngineIndex)

            if (!config.proximityWarningIsEnabled)
            {
                binding.proximityWarningLayout.visibility = View.GONE
            }
            if (!config.geofenceIsEnabled)
            {
                binding.geofenceLayout.visibility = View.GONE
            }
        }

        binding.saveButton.setOnClickListener {

            if (binding.configNameEditText.text.toString().isEmpty())
            {
                Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.minGpsPrecisionEditText.text.toString().isEmpty())
            {
                Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.desired_gps_position), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.encryptionPasswordEditText.text.toString().length < 6)
            {
                Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.min_password_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedViewModel.currentConfiguration?.value?.let { config ->
                if (DAO.configDAO.nameExists( config ))
                {
                    Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.config_name_already_exists), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (config.minGpsPrecision == 0)
                {
                    Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.desired_gps_position), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                binding.progressOverlayView.visibility = View.VISIBLE

                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO)
                    {
                        DAO.configDAO.createOrUpdateConfig( config,UUID.randomUUID().toString())
                    }

                    // back on the main thread...
                    binding.progressOverlayView.visibility = View.GONE

                    findNavController().popBackStack()
                }
            }
        }

        binding.proximityWarningEnabledSwitch.setOnClickListener {
            if (binding.proximityWarningEnabledSwitch.isChecked)
            {
                binding.proximityWarningLayout.visibility = View.VISIBLE
            }
            else
            {
                binding.proximityWarningLayout.visibility = View.GONE
            }
        }

        binding.geofenceSwitch.setOnClickListener {
            if (binding.geofenceSwitch.isChecked)
            {
                binding.geofenceLayout.visibility = View.VISIBLE
            }
            else
            {
                binding.geofenceLayout.visibility = View.GONE
            }
        }

        binding.addStudyButton.setOnClickListener{
            sharedViewModel.createStudyModel.createNewStudy()
            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment)
        }

        manageStudiesAdapter = ManageStudiesAdapter(listOf<Study>())
        manageStudiesAdapter.didSelectStudy = this::didSelectStudy

        binding.studiesRecycler.itemAnimator = DefaultItemAnimator()
        binding.studiesRecycler.adapter = manageStudiesAdapter
        binding.studiesRecycler.layoutManager = LinearLayoutManager(activity )
    }

    override fun onResume()
    {
        super.onResume()

        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.CreateConfigurationFragment.value.toString() + ": " + this.javaClass.simpleName

        manageStudiesAdapter.updateStudies(sharedViewModel.currentConfiguration?.value?.studies)
    }

    private fun didSelectStudy(study: Study)
    {
        sharedViewModel.createStudyModel.setCurrentStudy(study)
        findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment)
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        motionEvent?.let {
            if (it.action == MotionEvent.ACTION_UP) {
                val bundle = Bundle()
                bundle.putBoolean( Keys.kEditMode.value, true )
                sharedViewModel.currentConfiguration?.value?.let { config ->

                    binding.mapOverlayView.visibility = View.VISIBLE

                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO)
                        {
                            if (config.enumAreas.isEmpty())
                            {
                                config.enumAreas = DAO.enumAreaDAO.getEnumAreas( config )
                            }
                        }

                        // back on the main thread...
                        binding.mapOverlayView.visibility = View.GONE

                        if (config.mapEngineIndex == MapEngine.OpenStreetMap.value)
                        {
                            findNavController().navigate(R.id.action_navigate_to_CreateOsmEnumerationAreaFragment, bundle)
                        }
                        else if (config.mapEngineIndex == MapEngine.MapBox.value)
                        {
                            findNavController().navigate(R.id.action_navigate_to_CreateEnumerationAreaFragment, bundle)
                        }
                    }
                }
            }
        }

        view?.performClick()

        return true
    }

    override fun onDestroyView()
    {
        binding.studiesRecycler.adapter = null

        _binding = null

        super.onDestroyView()
    }
}