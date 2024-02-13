package edu.gtri.gpssample.fragments.perform_multi_collection

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
    LaunchSurveyDialog.LaunchSurveyDialogDelegate,
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

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(PerformCollectionFragment.LaunchSurveyRequest) { key, bundle ->
            sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
                if (gpsAccuracyIsGood && gpsLocationIsGood)
                {
                    SurveyLaunchNotificationDialog( activity!!, this )
                }
                else
                {
                    LaunchSurveyDialog( activity, gpsAccuracyIsGood && gpsLocationIsGood, this@PerformMultiCollectionFragment)
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

        arguments?.getBoolean(Keys.kGpsAccuracyIsGood.toString())?.let { gpsAccuracyIsGood ->
            this.gpsAccuracyIsGood = gpsAccuracyIsGood
        }

        arguments?.getBoolean(Keys.kGpsLocationIsGood.toString())?.let { gpsLocationIsGood ->
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

        performMultiCollectionAdapter = PerformMultiCollectionAdapter( enumerationItems )
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

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageEnumerationTeamsFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectEnumerationItem( enumerationItem: EnumerationItem)
    {
        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
            (this.activity!!.application as? MainApplication)?.currentEnumerationItemUUID = enumerationItem.uuid
            (this.activity!!.application as? MainApplication)?.currentEnumerationAreaName = enumArea.name
            (this.activity!!.application as? MainApplication)?.currentSubAddress = enumerationItem.subAddress
            sharedViewModel.locationViewModel.setCurrentEnumerationItem( enumerationItem )
            LaunchSurveyDialog( activity, gpsAccuracyIsGood, this@PerformMultiCollectionFragment)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun launchSurveyButtonPressed()
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.type = "vnd.android.cursor.dir/vnd.odk.form"
            odk_result.launch(intent)
        }
    }

    override fun markAsIncompleteButtonPressed()
    {
        AdditionalInfoDialog( activity, "", "", this)
    }

    override fun showInfoButtonPressed()
    {
        val bundle = Bundle()
        bundle.putBoolean( Keys.kEditMode.toString(), false )
        bundle.putBoolean( Keys.kCollectionMode.toString(), true )
        findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)
    }

    override fun didSelectCancelButton()
    {
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val odk_result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        AdditionalInfoDialog( activity, "", "", this)
    }

    override fun didSelectSaveButton( incompleteReason: String, notes: String )
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->

            sharedViewModel.locationViewModel.currentEnumerationItem?.value?.let { sampledItem ->

                sampledItem.collectionNotes = notes
                sampledItem.modificationDate = DAO.updateCreationDate( sampledItem.modificationDate )
                sampledItem.collectionDate = sampledItem.modificationDate
                sampledItem.collectionState = CollectionState.Complete

                (activity!!.application as MainApplication).user?.let { user ->
                    sampledItem.collectorName = user.name
                }

                if (incompleteReason.isNotEmpty())
                {
                    sampledItem.collectionState = CollectionState.Incomplete
                }

                DAO.enumerationItemDAO.createOrUpdateEnumerationItem( sampledItem, location )

                sharedViewModel.currentConfiguration?.value?.let { config ->
                    DAO.configDAO.getConfig( config.id!! )?.let {
                        sharedViewModel.setCurrentConfig( it )
                    }
                }

                sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
                    DAO.enumAreaDAO.getEnumArea( enumArea.id!! )?.let {
                        sharedViewModel.enumAreaViewModel.setCurrentEnumArea( it )
                    }
                }

                sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                    DAO.studyDAO.getStudy( study.id!! )?.let {
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

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}