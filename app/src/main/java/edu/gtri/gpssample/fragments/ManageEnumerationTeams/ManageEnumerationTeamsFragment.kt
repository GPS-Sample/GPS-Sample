package edu.gtri.gpssample.fragments.ManageEnumerationTeams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.Team
import edu.gtri.gpssample.databinding.FragmentManageEnumerationTeamsBinding
import edu.gtri.gpssample.dialogs.CreateTeamDialog
import edu.gtri.gpssample.fragments.ManageSamples.ManageSamplesViewModel
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class ManageEnumerationTeamsFragment : Fragment(), CreateTeamDialog.CreateTeamDialogDelegate
{
    private var _binding: FragmentManageEnumerationTeamsBinding? = null
    private val binding get() = _binding!!

    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var manageEnumerationTeamsAdapter: ManageEnumerationTeamsAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

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

        binding.titleTextView.setText( enumArea.name + " Teams")

        enumArea.id?.let {
            val teams = DAO.teamDAO.getTeams( it )
            manageEnumerationTeamsAdapter = ManageEnumerationTeamsAdapter( teams )
        }

        manageEnumerationTeamsAdapter.didSelectTeam = this::didSelectTeam

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageEnumerationTeamsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        binding.addButton.setOnClickListener {
            CreateTeamDialog( activity!!, null, this )
        }

        binding.finishButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageEnumerationTeamsFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun shouldUpdateTeam( team: Team )
    {
    }

    override fun shouldCreateTeamNamed( name: String )
    {
        enumArea.id?.let { enum_area_id ->
            val team = Team( enum_area_id, name )
            DAO.teamDAO.createTeam( team )
            manageEnumerationTeamsAdapter.updateTeams( DAO.teamDAO.getTeams( enum_area_id ))
        }
    }

    fun didSelectTeam( team: Team )
    {
        sharedViewModel.teamViewModel.setCurrentTeam( team )

        findNavController().navigate( R.id.action_navigate_to_ManageEnumerationTeamFragment )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }}