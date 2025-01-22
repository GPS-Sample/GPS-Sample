/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.add_landmark

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentAddLandmarkBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.NotificationDialog
import edu.gtri.gpssample.utils.CameraUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class AddLandmarkFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentAddLandmarkBinding? = null
    private val binding get() = _binding!!

    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var enumArea : EnumArea
    private lateinit var location: Location
    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View
    {
        _binding = FragmentAddLandmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        sharedViewModel.locationViewModel.currentLocation?.value?.let {
            location = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let{
            enumArea = it
        }

        val components = location.uuid.split("-" )

        binding.UUIDEditText.setText( components[0] )
        binding.latitudeEditText.setText( String.format( "%.6f", location.latitude ))
        binding.longitudeEditText.setText( String.format( "%.6f", location.longitude ))
        binding.descriptionEditText.setText( location.description )

        if (location.imageData.isNotEmpty())
        {
            try
            {
                binding.landmarkImageView.setImageBitmap( CameraUtils.decodeString( location.imageData ))
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTrace.toString())
            }
        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_landmark_message),
                resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
        }

        binding.addPhotoImageView.setOnClickListener {

            // get the total size of all image data
            var size = 0

            for (location in enumArea.locations)
            {
                size += location.imageData.length
            }

            if (size > 25 * 1024 * 1024)
            {
                NotificationDialog( activity!!, resources.getString( R.string.warning), resources.getString( R.string.image_size_warning))
            }

            findNavController().navigate(R.id.action_navigate_to_CameraFragment)
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            location.description = binding.descriptionEditText.text.toString()

            DAO.locationDAO.updateLocation( location, enumArea )

            DAO.configDAO.getConfig( config.uuid )?.let {
                sharedViewModel.setCurrentConfig( it )
            }

            DAO.enumAreaDAO.getEnumArea( enumArea.uuid )?.let {
                sharedViewModel.enumAreaViewModel.setCurrentEnumArea( it )
            }

            DAO.studyDAO.getStudy( study.uuid )?.let {
                sharedViewModel.createStudyModel.setStudy( it )
            }

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.AddLandmarkFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun didSelectFirstButton(tag: Any?)
    {
    }

    override fun didSelectSecondButton(tag: Any?)
    {
        enumArea.locations.remove(location)

        DAO.locationDAO.delete( location )

        findNavController().popBackStack()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}