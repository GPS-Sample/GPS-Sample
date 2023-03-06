package edu.gtri.gpssample.fragments.ManageEnumerationTeams

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
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
import java.util.*

class ManageEnumerationTeamsFragment : Fragment(), CreateTeamDialog.CreateTeamDialogDelegate
{
    private var _binding: FragmentManageEnumerationTeamsBinding? = null
    private val binding get() = _binding!!

    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var viewModel: ManageSamplesViewModel
    private lateinit var manageEnumerationTeamsAdapter: ManageEnumerationTeamsAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ManageSamplesViewModel::class.java)
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

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.",Toast.LENGTH_SHORT).show()
            return
        }

        val study_uuid = arguments!!.getString(Keys.kStudy_uuid.toString(), "");

        if (study_uuid.isEmpty()) {
            Toast.makeText( activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.studyDAO.getStudy( study_uuid )?.let { study ->
            this.study = study
        }

        if (!this::study.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Study with id $study_uuid not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val enumArea_uuid = arguments!!.getString(Keys.kEnumArea_uuid.toString(), "");

        if (enumArea_uuid.isEmpty()) {
            Toast.makeText( activity!!.applicationContext, "Fatal! Missing required parameter: enum_area_uuid.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.enumAreaDAO.getEnumArea( enumArea_uuid )?.let { enumArea ->
            this.enumArea = enumArea
        }

        if (!this::enumArea.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Study with id $enumArea_uuid not found.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.titleTextView.setText( enumArea.name + " Teams")
        val teams = DAO.teamDAO.getTeams( enumArea_uuid )

        manageEnumerationTeamsAdapter = ManageEnumerationTeamsAdapter( teams )
        manageEnumerationTeamsAdapter.didSelectTeam = this::didSelectTeam

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageEnumerationTeamsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        binding.addButton.setOnClickListener {
            CreateTeamDialog( activity!!, this )
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

    override fun shouldCreateTeamNamed( name: String )
    {
        val team = Team( UUID.randomUUID().toString(), enumArea.uuid, name )
        DAO.teamDAO.createTeam( team )
        manageEnumerationTeamsAdapter.updateTeams( DAO.teamDAO.getTeams( enumArea.uuid ))
    }

    fun didSelectTeam( team: Team )
    {
        Log.d( "xxx", team.name )
        val bundle = Bundle()
        bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
        bundle.putString( Keys.kEnumArea_uuid.toString(), enumArea.uuid )
        bundle.putString( Keys.kTeam_uuid.toString(), team.uuid )
        findNavController().navigate( R.id.action_navigate_to_ManageEnumerationTeamFragment, bundle )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }}