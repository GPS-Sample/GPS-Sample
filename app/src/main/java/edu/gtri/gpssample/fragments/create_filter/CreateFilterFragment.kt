/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.create_filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.databinding.FragmentCreateFilterBinding
import edu.gtri.gpssample.fragments.ManageStudies.CreateFilterAdapter
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class CreateFilterFragment : Fragment()
{
    private var _binding: FragmentCreateFilterBinding? = null
    private val binding get() = _binding!!
    private lateinit var createFilterAdapter: CreateFilterAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel
    private var isSubsetRule : Boolean = true
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateFilterBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createFilterFragment = this@CreateFilterFragment
            this.executePendingBindings()
        }

        composableConfirmationDialogHost = ComposableConfirmationDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableConfirmationDialogHost.Content()
        }

        arguments?.getBoolean( Keys.kIsSubsetRule.value)?.let { isSubsetRule ->
            this.isSubsetRule = isSubsetRule
        }

        createFilterAdapter = sharedViewModel.createFilterModel.createFilterAdapter
        createFilterAdapter.shouldEditFilterRule = this::shouldEditFilterRule
        createFilterAdapter.shouldDeleteFilterRule = this::shouldDeleteFilterRule

        sharedViewModel.createFilterRuleModel.createFilterAdapter = createFilterAdapter

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        //binding.recyclerView.adapter = createFilterAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.addRuleButton.setOnClickListener {
            if (isSubsetRule)
            {
                sharedViewModel.createNewSubsetFilterRule()
            }
            else
            {
                sharedViewModel.createNewPrimaryFilterRule()
            }

            val bundle = Bundle()
            findNavController().navigate(R.id.action_navigate_to_SelectRuleDialogFragment, bundle)
        }

        binding.deleteImageView.setOnClickListener {
            sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                sharedViewModel.createFilterModel.deleteSelectedFilter( study )
                findNavController().popBackStack()
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            if (binding.nameEditText.text.toString().length == 0)
            {
                Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()

                return@setOnClickListener
            }

            sharedViewModel.createFilterModel.currentFilter?.value?.let { filter ->
                filter.rule?.let { rule ->
                    if (rule.isSubsetRule)
                    {
                        sharedViewModel.addSubsetFilter()
                    }
                    else
                    {
                        sharedViewModel.addPrimaryFilter()
                    }
                }
            }

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.CreateFilterFragment.value.toString() + ": " + this.javaClass.simpleName
        //val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter.uuid )
       // createFilterAdapter.updateFilterRules(filterRules)
    }

    fun shouldEditFilterRule( filterRule: Rule )
    {
       // SelectRuleDialog( activity!!, study_uuid, filter.uuid, filterRule,this )
    }

    fun shouldDeleteFilterRule( firstRule: Rule, previousRule: Rule? )
    {
        var isOnlyRule = false
        var isLastRule = false

        if (previousRule == null && firstRule.filterOperator == null)
        {
            isOnlyRule = true
        }
        else if (previousRule != null && firstRule.filterOperator == null)
        {
            isLastRule = true
        }

        if (isOnlyRule || isLastRule)
        {
            composableConfirmationDialogHost.show(
                title = resources.getString(R.string.please_confirm),
                message = resources.getString(R.string.delete_filter_message),
                leftButtonText = resources.getString(R.string.no),
                rightButtonText = resources.getString(R.string.yes),
                destructive = true
            ) { selection ->
                if (selection == resources.getString(R.string.yes)) {
                    if (isOnlyRule)
                    {
                        sharedViewModel.createFilterModel.currentFilter?.value?.let{ filter->
                            filter.rule = null
                            createFilterAdapter.updateRules(filter.rule )
                        }
                    }
                    else if (isLastRule)
                    {
                        previousRule?.filterOperator = null
                        sharedViewModel.createFilterModel.currentFilter?.value?.let{ filter->
                            createFilterAdapter.updateRules(filter.rule )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView()
    {
        binding.recyclerView.adapter = null
        _binding = null

        super.onDestroyView()
    }
}