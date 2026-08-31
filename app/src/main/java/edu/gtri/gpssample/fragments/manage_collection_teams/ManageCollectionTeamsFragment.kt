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
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentManageCollectionTeamsBinding
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageCollectionTeamsFragment : Fragment()
{
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var manageCollectionTeamsAdapter: ManageCollectionTeamsAdapter
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost
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

        composableConfirmationDialogHost = ComposableConfirmationDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableConfirmationDialogHost.Content()
        }

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed()
            {
                (requireActivity().application as? MainApplication)?.user?.let { user ->
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

        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.ManageCollectionTeamsFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectTeam(collectionTeam: CollectionTeam)
    {
        enumArea.selectedEnumerationTeamUuid = ""
        enumArea.selectedCollectionTeamUuid = collectionTeam.uuid
        sharedViewModel.currentCollectionTeamUuid = collectionTeam.uuid

        enumArea.locations.clear()

        binding.progressOverlayView.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO)
            {
                for (uuid in collectionTeam.locationUuids)
                {
                    DAO.locationDAO.getLocation( uuid )?.let {
                        enumArea.locations.add( it )
                    }
                }
            }

            // back on the main thread...
            binding.progressOverlayView.visibility = View.GONE

            findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment)
        }
    }

    fun shouldDeleteTeam(collectionTeam: CollectionTeam)
    {
        composableConfirmationDialogHost.show(
            title = resources.getString(R.string.please_confirm),
            message = resources.getString(R.string.delete_team_message),
            leftButtonText = resources.getString(R.string.no),
            rightButtonText = resources.getString(R.string.yes),
            destructive = true
        ) { selection ->
            if (selection == resources.getString(R.string.yes))
            {
                enumArea.collectionTeams.remove( collectionTeam )
                manageCollectionTeamsAdapter.updateTeams(enumArea.collectionTeams)
                DAO.collectionTeamDAO.deleteTeam( collectionTeam )
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            16908332-> // TODO: use R.id.?
            {
                (requireActivity().application as? MainApplication)?.user?.let { user ->
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
        binding.teamRecyclerView.adapter = null
        _binding = null

        super.onDestroyView()
    }
}