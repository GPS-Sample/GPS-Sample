/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.manage_collection_teams

import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.constants.SamplingMethod
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
    private lateinit var enumArea: EnumArea
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

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed()
            {
                (activity!!.application as? MainApplication)?.user?.let { user ->
                    if (user.role == Role.Admin.value || user.role == Role.Supervisor.value)
                    {
                        findNavController().popBackStack(R.id.ConfigurationFragment, false)
                    }
                }
            }
        })

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        if (!this::manageCollectionTeamsAdapter.isInitialized)
        {
            manageCollectionTeamsAdapter = ManageCollectionTeamsAdapter( enumArea.collectionTeams )
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

        binding.reviewAllTeamsButton.setOnClickListener {
            if (enumArea.collectionTeams.isNotEmpty())
            {
                findNavController().navigate(R.id.action_navigate_to_ReviewCollectionFragment)
            }
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

        enumArea.selectedEnumerationTeamUuid = ""
        enumArea.selectedCollectionTeamUuid = collectionTeam.uuid

        findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment)
    }

    fun shouldDeleteTeam(collectionTeam: CollectionTeam)
    {
        ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_team_message),
            resources.getString(R.string.no), resources.getString(R.string.yes), collectionTeam, this)
    }

    override fun didSelectFirstButton(tag: Any?)
    {
    }

    override fun didSelectSecondButton(tag: Any?)
    {
        val collectionTeam = tag as CollectionTeam
        enumArea.collectionTeams.remove(collectionTeam)
        manageCollectionTeamsAdapter.updateTeams(enumArea.collectionTeams)
        DAO.collectionTeamDAO.deleteTeam( collectionTeam )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            16908332-> // TODO: use R.id.?
            {
                (activity!!.application as? MainApplication)?.user?.let { user ->
                    if (user.role == Role.Admin.value || user.role == Role.Supervisor.value)
                    {
                        findNavController().navigate(R.id.action_navigate_to_ConfigurationFragment)
                        return false
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}