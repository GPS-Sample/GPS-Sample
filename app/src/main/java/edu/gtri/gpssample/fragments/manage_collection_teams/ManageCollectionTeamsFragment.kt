package edu.gtri.gpssample.fragments.manage_collection_teams

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentManageCollectionTeamsBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import kotlin.collections.ArrayList

class ManageCollectionTeamsFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var study: Study
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var manageCollectionTeamsAdapter: ManageCollectionTeamsAdapter

    private var _binding: FragmentManageCollectionTeamsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        val samplingVm : SamplingViewModel by activityViewModels()

        samplingViewModel = samplingVm
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentManageCollectionTeamsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        if (study.selectedCollectionTeamId > 0) // if teamId is valid, then filter out all teams except this one
        {
            val teams = study.collectionTeams.filter { collectionTeam ->
                collectionTeam.id?.let { id ->
                    id == study.selectedCollectionTeamId
                } ?: false
            }

            if (teams.isNotEmpty())
            {
                val collectionTeams = ArrayList<CollectionTeam>()
                collectionTeams.add( teams[0] )
                manageCollectionTeamsAdapter = ManageCollectionTeamsAdapter( collectionTeams )
            }
        }

        if (!this::manageCollectionTeamsAdapter.isInitialized)
        {
            manageCollectionTeamsAdapter = ManageCollectionTeamsAdapter( study.collectionTeams )
        }

        manageCollectionTeamsAdapter.didSelectTeam = this::didSelectTeam
        manageCollectionTeamsAdapter.shouldDeleteTeam = this::shouldDeleteTeam

        binding.teamRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.teamRecyclerView.adapter = manageCollectionTeamsAdapter
        binding.teamRecyclerView.layoutManager = LinearLayoutManager(activity)

        binding.titleTextView.text = getString(R.string.collection_teams)

        binding.addButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CreateCollectionTeamFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageCollectionTeamsFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectTeam(collectionTeam: CollectionTeam)
    {
        sharedViewModel.teamViewModel.setCurrentCollectionTeam( collectionTeam )

//        collectionTeam.id?.let {
//            study.selectedCollectionTeamId = it
//        }

        findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment)
    }

    fun shouldDeleteTeam(collectionTeam: CollectionTeam)
    {
        ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_team_message),
            resources.getString(R.string.no), resources.getString(R.string.yes), collectionTeam, this)
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        val collectionTeam = tag as CollectionTeam
        study.collectionTeams.remove(collectionTeam)
        manageCollectionTeamsAdapter.updateTeams(study.collectionTeams)
        DAO.collectionTeamDAO.deleteTeam( collectionTeam )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}