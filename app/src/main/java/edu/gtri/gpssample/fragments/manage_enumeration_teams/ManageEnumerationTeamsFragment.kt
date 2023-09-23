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
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.Team
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.databinding.FragmentManageEnumerationTeamsBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import kotlin.collections.ArrayList
class ManageEnumerationTeamsFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var manageEnumerationTeamsAdapter: ManageEnumerationTeamsAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var _binding: FragmentManageEnumerationTeamsBinding? = null
    private val binding get() = _binding!!
    private var users = ArrayList<User>()
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

        enumArea?.let { enumArea ->
            sharedViewModel.currentConfiguration?.value?.let { config ->

                manageEnumerationTeamsAdapter = ManageEnumerationTeamsAdapter( enumArea.enumerationTeams )
            }
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

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectTeam( team: Team )
    {
        sharedViewModel.teamViewModel.setCurrentTeam( team )

        findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
    }

    private fun shouldDeleteTeam(team: Team)
    {
        ConfirmationDialog( activity, resources.getString(R.string.delete_team_message),
            resources.getString(R.string.delete_team_message), resources.getString(R.string.no),
            resources.getString(R.string.yes), team, this)
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        val team = tag as Team
        enumArea.enumerationTeams.remove(team)
        manageEnumerationTeamsAdapter.updateTeams(enumArea.enumerationTeams)
        DAO.teamDAO.deleteTeam( team )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}