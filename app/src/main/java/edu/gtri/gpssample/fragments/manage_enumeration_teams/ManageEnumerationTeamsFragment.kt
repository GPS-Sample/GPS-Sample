/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.manage_enumeration_teams

import android.os.Bundle
import android.view.*
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.EnumerationTeam
import edu.gtri.gpssample.databinding.FragmentManageEnumerationTeamsBinding
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageEnumerationTeamsFragment : Fragment()
{
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var manageEnumerationTeamsAdapter: ManageEnumerationTeamsAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost
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

        composableConfirmationDialogHost = ComposableConfirmationDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableConfirmationDialogHost.Content()
        }

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

        binding.titleTextView.text = enumArea.name + " " + resources.getString(R.string.teams)

        binding.addButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CreateEnumerationTeamFragment)
        }

        binding.reviewAllTeamsButton.setOnClickListener {
            if (enumArea.enumerationTeams.isNotEmpty())
            {
                findNavController().navigate(R.id.action_navigate_to_ReviewEnumerationFragment)
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageEnumerationTeamsFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectTeam( enumerationTeam: EnumerationTeam )
    {
        enumArea.selectedCollectionTeamUuid = ""
        enumArea.selectedEnumerationTeamUuid = enumerationTeam.uuid
        sharedViewModel.currentEnumerationTeamUuid = enumerationTeam.uuid

        enumArea.locations.clear()

        binding.progressOverlayView.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO)
            {
                for (uuid in enumerationTeam.locationUuids)
                {
                    DAO.locationDAO.getLocation( uuid )?.let {
                        enumArea.locations.add( it )
                    }
                }
            }

            // back on the main thread...
            binding.progressOverlayView.visibility = View.GONE

            findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
        }
    }

    private fun shouldDeleteTeam(enumerationTeam: EnumerationTeam)
    {
        composableConfirmationDialogHost.show(
            title = resources.getString(R.string.delete_team_message),
            message = resources.getString(R.string.delete_team_message),
            leftButtonText = resources.getString(R.string.no),
            rightButtonText = resources.getString(R.string.yes),
            destructive = true
        ) { selection ->
            if (selection == resources.getString(R.string.yes))
            {
                enumArea.enumerationTeams.remove( enumerationTeam )
                manageEnumerationTeamsAdapter.updateTeams(enumArea.enumerationTeams)
                DAO.enumerationTeamDAO.deleteTeam( enumerationTeam )
            }
        }
    }

    override fun onDestroyView()
    {
        binding.teamRecyclerView.adapter = null
        _binding = null

        super.onDestroyView()
    }
}