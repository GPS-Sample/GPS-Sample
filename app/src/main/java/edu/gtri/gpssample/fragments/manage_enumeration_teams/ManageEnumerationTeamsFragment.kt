package edu.gtri.gpssample.fragments.manage_enumeration_teams

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
import edu.gtri.gpssample.database.models.CollectionTeam
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.EnumerationTeam
import edu.gtri.gpssample.databinding.FragmentManageEnumerationTeamsBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.fragments.manage_collection_teams.ManageCollectionTeamsAdapter
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class ManageEnumerationTeamsFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var manageEnumerationTeamsAdapter: ManageEnumerationTeamsAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var _binding: FragmentManageEnumerationTeamsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentManageEnumerationTeamsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        if (!this::manageEnumerationTeamsAdapter.isInitialized)
        {
            manageEnumerationTeamsAdapter = ManageEnumerationTeamsAdapter( enumArea.enumerationTeams )
        }

        manageEnumerationTeamsAdapter.didSelectTeam = this::didSelectTeam
        manageEnumerationTeamsAdapter.shouldDeleteTeam = this::shouldDeleteTeam

        binding.teamRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.teamRecyclerView.adapter = manageEnumerationTeamsAdapter
        binding.teamRecyclerView.layoutManager = LinearLayoutManager(activity)

        binding.titleTextView.text = enumArea.name + " Teams"

        binding.addButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CreateEnumerationTeamFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageEnumerationTeamsFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectTeam(enumerationTeam: EnumerationTeam )
    {
        sharedViewModel.teamViewModel.setCurrentEnumerationTeam( enumerationTeam )

        enumArea.selectedCollectionTeamUuid = ""
        enumArea.selectedEnumerationTeamUuid = enumerationTeam.uuid

        findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
    }

    private fun shouldDeleteTeam(enumerationTeam: EnumerationTeam)
    {
        ConfirmationDialog( activity, resources.getString(R.string.delete_team_message),
            resources.getString(R.string.delete_team_message), resources.getString(R.string.no),
            resources.getString(R.string.yes), enumerationTeam, this)
    }

    override fun didSelectFirstButton(tag: Any?)
    {
    }

    override fun didSelectSecondButton(tag: Any?)
    {
        val enumerationTeam = tag as EnumerationTeam
        enumArea.enumerationTeams.remove(enumerationTeam)
        manageEnumerationTeamsAdapter.updateTeams(enumArea.enumerationTeams)
        DAO.enumerationTeamDAO.deleteTeam( enumerationTeam )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}