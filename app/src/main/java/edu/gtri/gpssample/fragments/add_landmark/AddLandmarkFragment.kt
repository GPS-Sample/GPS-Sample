/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.add_landmark

import android.os.Bundle
import android.view.*
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentAddLandmarkBinding
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.utils.CameraUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.Date
import java.util.UUID

class AddLandmarkFragment : Fragment()
{
    private var _binding: FragmentAddLandmarkBinding? = null
    private val binding get() = _binding!!
    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var enumArea : EnumArea
    private lateinit var location: Location
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost

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

        composableConfirmationDialogHost = ComposableConfirmationDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableConfirmationDialogHost.Content()
        }

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let{
            enumArea = it
        }

        enumArea.locations.find { it.uuid == sharedViewModel.currentLocationUuid }?.let { location: Location ->
            this.location = location
        }

        val components = location.uuid.split("-" )

        binding.UUIDEditText.setText( components[0] )
        binding.latitudeEditText.setText( String.format( "%.6f", location.latitude ))
        binding.longitudeEditText.setText( String.format( "%.6f", location.longitude ))
        binding.descriptionEditText.setText( location.description )

        ImageDAO.instance().getImage( location )?.let { image ->
            CameraUtils.decodeString( image.data )?.let { bitmap ->
                binding.landmarkImageView.setImageBitmap( bitmap )
            }
        }

        binding.deleteImageView.setOnClickListener {
            composableConfirmationDialogHost.show(
                title = resources.getString(R.string.please_confirm),
                message = resources.getString(R.string.delete_landmark_message),
                leftButtonText = resources.getString(R.string.no),
                rightButtonText = resources.getString(R.string.yes),
                destructive = true
            ) { selection ->
                if (selection == resources.getString(R.string.yes)) {
                    enumArea.locations.remove(location)

                    DAO.locationDAO.delete( location )

                    findNavController().popBackStack()
                }
            }
        }

        binding.addPhotoImageView.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CameraFragment)
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            location.description = binding.descriptionEditText.text.toString()

            location.creationDate = Date().time
            DAO.locationDAO.createOrUpdateLocation( location, enumArea, UUID.randomUUID().toString())

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.AddLandmarkFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}