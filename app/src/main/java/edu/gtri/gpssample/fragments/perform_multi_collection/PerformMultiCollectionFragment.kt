/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.perform_multi_collection

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location
import edu.gtri.gpssample.databinding.FragmentAddMultiHouseholdBinding
import edu.gtri.gpssample.databinding.FragmentPerformMultiCollectionBinding
import edu.gtri.gpssample.dialogs.AdditionalInfoDialog
import edu.gtri.gpssample.dialogs.LaunchSurveyDialog
import edu.gtri.gpssample.dialogs.SurveyLaunchNotificationDialog
import edu.gtri.gpssample.fragments.perform_collection.PerformCollectionFragment
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

class PerformMultiCollectionFragment : Fragment(),
    AdditionalInfoDialog.AdditionalInfoDialogDelegate,
    SurveyLaunchNotificationDialog.SurveyLaunchNotificationDialogDelegate
{
    private lateinit var location: Location
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var performMultiCollectionAdapter: PerformMultiCollectionAdapter

    private var gpsAccuracyIsGood = false
    private var gpsLocationIsGood = false
    private var _binding: FragmentPerformMultiCollectionBinding? = null
    private val binding get() = _binding!!

    private val fragmentResultListener = "PerformMultiCollectionFragment"

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setFragmentResultListener( fragmentResultListener ) { key, bundle ->
            bundle.getString( Keys.kRequest.value )?.let { request ->
                sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
                    if (gpsAccuracyIsGood && gpsLocationIsGood)
                    {
                        when (request)
                        {
                            Keys.kAdditionalInfoRequest.value -> AdditionalInfoDialog(activity, "", "", this)
                            Keys.kLaunchSurveyRequest.value ->
                            {
                                sharedViewModel.locationViewModel.currentEnumerationItem?.value?.let { enumerationItem ->
                                    if (enumerationItem.odkRecordUri.isNotEmpty())
                                    {
                                        val uri = Uri.parse( enumerationItem.odkRecordUri )
                                        val intent = Intent(Intent.ACTION_EDIT)
                                        intent.setData(uri)
                                        odk_result.launch(intent)
                                    }
                                    else
                                    {
                                        // This will create a new ODK instance record
                                        SurveyLaunchNotificationDialog(activity!!, this)
                                    }
                                }
                            }
                        }
                    }
                    else if (!gpsAccuracyIsGood)
                    {
                        Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.gps_accuracy_error), Toast.LENGTH_LONG).show()
                    }
                    else
                    {
                        Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.gps_location_error), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentPerformMultiCollectionBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getBoolean(Keys.kGpsAccuracyIsGood.value)?.let { gpsAccuracyIsGood ->
            this.gpsAccuracyIsGood = gpsAccuracyIsGood
        }

        arguments?.getBoolean(Keys.kGpsLocationIsGood.value)?.let { gpsLocationIsGood ->
            this.gpsLocationIsGood = gpsLocationIsGood
        }

        sharedViewModel.locationViewModel.currentLocation?.value?.let {
            location = it
        }

        val enumerationItems = ArrayList<EnumerationItem>()

        for (enumurationItem in location.enumerationItems)
        {
            if (enumurationItem.samplingState == SamplingState.Sampled)
            {
                enumerationItems.add( enumurationItem )
            }
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
            performMultiCollectionAdapter = PerformMultiCollectionAdapter( enumerationItems, enumArea.name )
        }

        performMultiCollectionAdapter.didSelectEnumerationItem = this::didSelectEnumerationItem

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performMultiCollectionAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformMultiCollectionFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectEnumerationItem( enumerationItem: EnumerationItem)
    {
        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
            (this.activity!!.application as? MainApplication)?.currentEnumerationItemUUID = enumerationItem.uuid
            (this.activity!!.application as? MainApplication)?.currentEnumerationAreaName = enumArea.name
            (this.activity!!.application as? MainApplication)?.currentSubAddress = enumerationItem.subAddress
            sharedViewModel.locationViewModel.setCurrentEnumerationItem( enumerationItem )

            val bundle = Bundle()
            bundle.putBoolean( Keys.kEditMode.value, false )
            bundle.putBoolean( Keys.kCollectionMode.value, true )
            bundle.putString( Keys.kFragmentResultListener.value, fragmentResultListener )
            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)
        }
    }

    override fun didSelectCancelButton()
    {
    }

    override fun didSelectSaveButton( incompleteReason: String, notes: String )
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->

            sharedViewModel.locationViewModel.currentEnumerationItem?.value?.let { sampledItem ->

                sampledItem.collectionNotes = notes
                sampledItem.collectionDate = Date().time
                sampledItem.syncCode = sampledItem.syncCode + 1
                sampledItem.collectionState = CollectionState.Complete

                (activity!!.application as MainApplication).user?.let { user ->
                    sampledItem.collectorName = user.name
                }

                if (incompleteReason.isNotEmpty())
                {
                    sampledItem.collectionState = CollectionState.Incomplete
                    sampledItem.collectionIncompleteReason = incompleteReason
                }

                DAO.enumerationItemDAO.createOrUpdateEnumerationItem( sampledItem, location )

                sharedViewModel.currentConfiguration?.value?.let { config ->
                    DAO.configDAO.getConfig( config.uuid )?.let {
                        sharedViewModel.setCurrentConfig( it )
                    }
                }

                sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
                    DAO.enumAreaDAO.getEnumArea( enumArea.uuid )?.let {
                        sharedViewModel.enumAreaViewModel.setCurrentEnumArea( it )
                    }
                }

                sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                    DAO.studyDAO.getStudy( study.uuid )?.let {
                        sharedViewModel.createStudyModel.setStudy( it )
                    }
                }

                val enumerationItems = ArrayList<EnumerationItem>()

                for (enumurationItem in location.enumerationItems)
                {
                    if (enumurationItem.samplingState == SamplingState.Sampled)
                    {
                        enumerationItems.add( enumurationItem )
                    }
                }

                performMultiCollectionAdapter.updateEnumerationItems( enumerationItems )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun shouldLaunchODK()
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.type = "vnd.android.cursor.dir/vnd.odk.form"
            odk_result.launch(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val odk_result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK)
        {
            result.data?.data?.let { uri ->
                sharedViewModel.locationViewModel.currentEnumerationItem?.value?.let { enumerationItem ->
                    if (enumerationItem.odkRecordUri.isEmpty())
                    {
                        enumerationItem.odkRecordUri = uri.toString()
                        didSelectSaveButton( "Other", "User canceled action, ODK record saved.")
                    }
                }
            }

            val mainApplication = activity!!.application as MainApplication

            mainApplication.currentSubAddress = mainApplication.defaultSubAddress
            mainApplication.currentEnumerationItemUUID = mainApplication.defaultEnumerationItemUUID
            mainApplication.currentEnumerationAreaName = mainApplication.defaultEnumerationAreaName

            AdditionalInfoDialog( activity, "", "", this)
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}