package edu.gtri.gpssample.fragments.map

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import edu.gtri.gpssample.databinding.FragmentMapBinding
import edu.gtri.gpssample.dialogs.BusyIndicatorDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.R

class MapFragment : Fragment(), MapboxManager.MapTileCacheDelegate, BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var centerOnLocation = true
    private var busyIndicatorDialog: BusyIndicatorDialog? = null

    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel: ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentMapBinding.inflate( inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner
        }

        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    initLocationComponent()
                }
            }
        )

        binding.centerOnLocationButton.backgroundTintList?.let {
            defaultColorList = it
        }

        val locationComponentPlugin = binding.mapView.location
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.addOnMoveListener(onMoveListener)
        binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));

        sharedViewModel.currentConfiguration?.value?.let { config ->
                if (config.mapTileRegions.size > 0)
                {
                    busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.downloading_map_tiles), this )
                    MapboxManager.loadStylePack( activity!!, this )
                }
        }

        binding.centerOnLocationButton.setOnClickListener {
            centerOnLocation = !centerOnLocation
            if (centerOnLocation)
            {
                val locationComponentPlugin = binding.mapView.location
                locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
                binding.mapView.gestures.addOnMoveListener(onMoveListener)
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
                binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                binding.mapView.gestures.removeOnMoveListener(onMoveListener)
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
            }
        }
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        binding.mapView.gestures.focalPoint = binding.mapView.getMapboxMap().pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    private fun onCameraTrackingDismissed() {
        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = binding.mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    activity!!,
                    com.mapbox.maps.R.drawable.mapbox_user_puck_icon,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    activity!!,
                    com.mapbox.maps.R.drawable.mapbox_user_icon_shadow,
                ),
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
    }

    override fun didPressCancelButton()
    {
        MapboxManager.cancelStylePackDownload()
        MapboxManager.cancelTilePackDownload()
    }

    override fun stylePackLoaded( error: String )
    {
        activity!!.runOnUiThread {
            if (error.isNotEmpty())
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                    Toast.makeText(activity!!.applicationContext,  resources.getString(edu.gtri.gpssample.R.string.style_pack_download_failed), Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    MapboxManager.loadTilePacks( activity!!, config.mapTileRegions, this )
                }
            }
        }
    }

    override fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    {
        busyIndicatorDialog?.let {
            activity!!.runOnUiThread {
                it.updateProgress(resources.getString(edu.gtri.gpssample.R.string.downloading_map_tiles) + " ${numLoaded}/${numNeeded}")
            }
        }
    }

    override fun tilePacksLoaded( error: String )
    {
        activity!!.runOnUiThread {
            if (error.isNotEmpty())
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                    Toast.makeText(activity!!.applicationContext,  resources.getString(edu.gtri.gpssample.R.string.tile_pack_download_failed), Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)

        _binding = null
    }
}