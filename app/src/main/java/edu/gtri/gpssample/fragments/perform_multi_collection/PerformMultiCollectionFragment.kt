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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location
import edu.gtri.gpssample.databinding.FragmentAddMultiHouseholdBinding
import edu.gtri.gpssample.databinding.FragmentPerformMultiCollectionBinding
import edu.gtri.gpssample.dialogs.AdditionalInfoDialog
import edu.gtri.gpssample.dialogs.LaunchSurveyDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class PerformMultiCollectionFragment : Fragment(), LaunchSurveyDialog.LaunchSurveyDialogDelegate, AdditionalInfoDialog.AdditionalInfoDialogDelegate
{
    private lateinit var location: Location
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var performMultiCollectionAdapter: PerformMultiCollectionAdapter

    private var _binding: FragmentPerformMultiCollectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

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

        sharedViewModel.locationViewModel.currentLocation?.value?.let {
            location = it
        }

        performMultiCollectionAdapter = PerformMultiCollectionAdapter( location.enumerationItems )
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
        (this.activity!!.application as? MainApplication)?.currentEnumerationItemUUID = enumerationItem.uuid
        sharedViewModel.locationViewModel.setCurrentEnumerationItem( enumerationItem )
        LaunchSurveyDialog( activity, this@PerformMultiCollectionFragment)
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

                sampledItem.collectionState = CollectionState.Complete
                sampledItem.notes = notes

                if (incompleteReason.isNotEmpty())
                {
                    sampledItem.collectionState = CollectionState.Incomplete
                }

                performMultiCollectionAdapter.updateEnumerationItems( location.enumerationItems )

                DAO.enumerationItemDAO.updateEnumerationItem( sampledItem, location )
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}