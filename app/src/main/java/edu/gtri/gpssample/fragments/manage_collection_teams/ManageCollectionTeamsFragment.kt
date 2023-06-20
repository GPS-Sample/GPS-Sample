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
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.Team
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.databinding.FragmentManageCollectionTeamsBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import kotlin.collections.ArrayList

class ManageCollectionTeamsFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var manageCollectionTeamsAdapter: ManageCollectionTeamsAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var _binding: FragmentManageCollectionTeamsBinding? = null
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
        _binding = FragmentManageCollectionTeamsBinding.inflate(inflater, container, false)

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

        enumArea.id?.let { enumAreaId ->
            sharedViewModel.currentConfiguration?.value?.let { config ->
                var teams = ArrayList<Team>()

                if (config.teamId > 0) // if teamId is valid, then filter out all teams except this one
                {
                    val team = DAO.teamDAO.getTeam( config.teamId )
                    team?.let {
                        teams.add( it )
                    }
                }
                else  // otherwise show all teams
                {
                    teams = DAO.teamDAO.getCollectionTeams( enumAreaId )
                }

                manageCollectionTeamsAdapter = ManageCollectionTeamsAdapter( teams )
            }
        }

        manageCollectionTeamsAdapter.didSelectTeam = this::didSelectTeam
        manageCollectionTeamsAdapter.shouldDeleteTeam = this::shouldDeleteTeam

        binding.teamRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.teamRecyclerView.adapter = manageCollectionTeamsAdapter
        binding.teamRecyclerView.layoutManager = LinearLayoutManager(activity)

        binding.titleTextView.text = enumArea.name + " Teams"

        binding.addButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CreateCollectionTeamFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectTeam( team: Team)
    {
        sharedViewModel.teamViewModel.setCurrentTeam( team )

        findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment)
    }

    fun shouldDeleteTeam( team: Team)
    {
        ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this team?", "No", "Yes", team, this)
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        val team = tag as Team

        DAO.teamDAO.deleteTeam( team )

        enumArea.id?.let {
            manageCollectionTeamsAdapter.updateTeams( DAO.teamDAO.getEnumerationTeams( it ))
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}