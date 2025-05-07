/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.createconfiguration

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.LatLon
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentCreateConfigurationBinding
import edu.gtri.gpssample.dialogs.BusyIndicatorDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.ArrayList

class CreateConfigurationFragment : Fragment(),
    ConfirmationDialog.ConfirmationDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private var _binding: FragmentCreateConfigurationBinding? = null
    private val binding get() = _binding!!
    private var selectedStudy: Study? = null

    private lateinit var mapboxManager: MapboxManager
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var manageStudiesAdapter: ManageStudiesAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        manageStudiesAdapter = ManageStudiesAdapter(listOf<Study>())
        manageStudiesAdapter.didSelectStudy = this::didSelectStudy
        manageStudiesAdapter.shouldDeleteStudy = this::shouldDeleteStudy
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createConfigurationFragment = this@CreateConfigurationFragment
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        sharedViewModel.currentConfiguration?.value?.let { config ->
            if (!config.proximityWarningIsEnabled)
            {
                binding.proximityWarningEditText.visibility = View.GONE
            }
        }

        binding.saveButton.setOnClickListener {

            if (binding.configNameEditText.text.toString().isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.minGpsPrecisionEditText.text.toString().isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.desired_gps_position), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.encryptionPasswordEditText.text.toString().length < 6)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.min_password_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedViewModel.currentConfiguration?.value?.let {config ->
                if (config.minGpsPrecision == 0)
                {
                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.desired_gps_position), Toast.LENGTH_SHORT).show()
                }
                else
                {
                    val busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.saving_configuration), this, false )

                    Thread {
                        sharedViewModel.updateConfiguration()

                        activity!!.runOnUiThread {
                            busyIndicatorDialog.alertDialog.cancel()
                            findNavController().popBackStack()
                        }
                    }.start()
                }
            }
        }

        binding.proximityWarningEnabledSwitch.setOnClickListener {
            if (binding.proximityWarningEnabledSwitch.isChecked)
            {
                binding.proximityWarningEditText.visibility = View.VISIBLE
            }
            else
            {
                binding.proximityWarningEditText.visibility = View.GONE
            }
        }

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
        var style = Style.MAPBOX_STREETS
        sharedPreferences.getString( Keys.kMapStyle.value, null)?.let {
            style = it
        }

        binding.mapView.getMapboxMap().loadStyleUri(
            style,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    initLocationComponent()
                    refreshMap()
                }
            }
        )

        binding.mapView.getMapboxMap().addOnMapClickListener {
            val bundle = Bundle()
            bundle.putBoolean( Keys.kEditMode.value, true )
            findNavController().navigate(R.id.action_navigate_to_CreateEnumerationAreaFragment, bundle)
            return@addOnMapClickListener true
        }

        val pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager(binding.mapView)
        val polygonAnnotationManager = binding.mapView.annotations.createPolygonAnnotationManager()
        val polylineAnnotationManager = binding.mapView.annotations.createPolylineAnnotationManager()
        mapboxManager = MapboxManager( activity!!, pointAnnotationManager, polygonAnnotationManager, polylineAnnotationManager )

        binding.addStudyButton.setOnClickListener{
            sharedViewModel.createStudyModel.createNewStudy()
            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment)
        }

        binding.studiesRecycler.itemAnimator = DefaultItemAnimator()
        binding.studiesRecycler.adapter = manageStudiesAdapter
        binding.studiesRecycler.layoutManager = LinearLayoutManager(activity )
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateConfigurationFragment.value.toString() + ": " + this.javaClass.simpleName

        manageStudiesAdapter.updateStudies(sharedViewModel.currentConfiguration?.value?.studies)
    }

    private fun didSelectStudy(study: Study)
    {
        sharedViewModel.createStudyModel.setStudy(study)
        findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment)
    }
    private fun shouldDeleteStudy(study: Study)
    {
        selectedStudy = study
        ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.delete_study_message),
            resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
    }

    fun refreshMap()
    {
        sharedViewModel.currentConfiguration?.value?.let {config ->

            val enumVerts = ArrayList<LatLon>()

            for (enumArea in config.enumAreas)
            {
                val points = ArrayList<com.mapbox.geojson.Point>()
                val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

                enumArea.vertices.map {
                    enumVerts.add(it)
                    points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
                }

                pointList.add( points )

                mapboxManager.addPolygon( pointList, "#000000", 0.25 )
                mapboxManager.addPolyline( pointList[0], "#ff0000" )
            }

//            val latLngBounds = GeoUtils.findGeobounds(enumVerts)
//            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
//            val cameraPosition = CameraOptions.Builder()
//                .zoom(10.0)
//                .center(point)
//                .build()
//
//            binding.mapView.getMapboxMap().setCamera(cameraPosition)
        }
    }

    override fun didSelectFirstButton(tag: Any?)
    {
    }

    override fun didSelectSecondButton(tag: Any?)
    {
        sharedViewModel.removeStudy(selectedStudy)
        manageStudiesAdapter.updateStudies(sharedViewModel.currentConfiguration?.value?.studies)
    }

    override fun didPressCancelButton()
    {
    }

    private fun initLocationComponent() {
        binding.mapView.location.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }

        binding.mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)

        _binding = null
    }
}