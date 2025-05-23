/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.createfilter

import android.os.Bundle
import android.util.Log
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
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.databinding.FragmentCreateFilterBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog

import edu.gtri.gpssample.dialogs.SelectRuleDialogFragment
import edu.gtri.gpssample.fragments.ManageStudies.CreateFilterAdapter
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class CreateFilterFragment : Fragment() , ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentCreateFilterBinding? = null
    private val binding get() = _binding!!
    private lateinit var createFilterAdapter: CreateFilterAdapter
    private lateinit var filter: Filter
    private var sampleSizeIsVisible = true
    private lateinit var sharedViewModel : ConfigurationViewModel

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
        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createFilterFragment = this@CreateFilterFragment
            this.executePendingBindings()
        }

//
//            if (sampleSizeIsVisible)
//            {
//                if (filter.sampleSize > 0)
//                {
//                    when(filter.sampleSizeIndex)
//                    {
//                        0 -> binding.sampleSize1EditText.setText(filter.sampleSize.toString())
//                        1 -> binding.sampleSize2EditText.setText(filter.sampleSize.toString())
//                        2 -> binding.sampleSize3EditText.setText(filter.sampleSize.toString())
//                    }
//                }
//            }


        createFilterAdapter = sharedViewModel.createFilterModel.createFilterAdapter
        createFilterAdapter.shouldEditFilterRule = this::shouldEditFilterRule
        createFilterAdapter.shouldDeleteFilterRule = this::shouldDeleteFilterRule

        sharedViewModel.createFilterRuleModel.createFilterAdapter = createFilterAdapter

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        //binding.recyclerView.adapter = createFilterAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.addRuleButton.setOnClickListener {
            val bundle = Bundle()
            sharedViewModel.createNewFilterRule()

            findNavController().navigate(R.id.action_navigate_to_SelectRuleDialogFragment, bundle)

//            SelectRuleDialogFragment().show(
//                childFragmentManager, SelectRuleDialogFragment.TAG)

            //SelectedRuleDialog( activity!!, sharedViewModel, null, null, this )
        }

//        binding.sampleSize1EditText.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
//            binding.sampleSize2EditText.setText("")
//            binding.sampleSize3EditText.setText("")
//        }
//
//        binding.sampleSize2EditText.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
//            binding.sampleSize1EditText.setText("")
//            binding.sampleSize3EditText.setText("")
//        }
//
//        binding.sampleSize3EditText.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
//            binding.sampleSize1EditText.setText("")
//            binding.sampleSize2EditText.setText("")
//        }

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
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()

                return@setOnClickListener
            }

            sharedViewModel.addFilter()
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateFilterFragment.value.toString() + ": " + this.javaClass.simpleName
        //val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter.uuid )
       // createFilterAdapter.updateFilterRules(filterRules)
    }

    private var selectedFilterRule: Rule? = null
    private var previousSelectedFilterRule: Rule? = null

    fun shouldEditFilterRule( filterRule: Rule )
    {
       // SelectRuleDialog( activity!!, study_uuid, filter.uuid, filterRule,this )
    }

    fun shouldDeleteFilterRule( filterRule: Rule, previousRule: Rule? )
    {
        selectedFilterRule = filterRule
        previousSelectedFilterRule = previousRule
        ConfirmationDialog( activity!!, resources.getString(R.string.please_confirm),
            "Are you sure you want to delete this Filter Rule?", "No", "Yes", 0, this )
    }

    override fun didSelectFirstButton(tag: Any?)
    {
    }

    override fun didSelectSecondButton(tag: Any?)
    {
        selectedFilterRule?.let {rule->
            previousSelectedFilterRule?.let{previousRule->
                rule.filterOperator?.rule?.let{operatorRule->
                    // attach operatorrule to previous filteroperator
                    previousRule.filterOperator?.let{previousFilterOperator->
                        previousFilterOperator.rule = operatorRule
                        sharedViewModel.createFilterModel.currentFilter?.value?.let{filter->
                            createFilterAdapter.updateRules(filter.rule)
                        }
                    }

                }
            }?:run{
                //filter
                sharedViewModel.createFilterModel.currentFilter?.value?.let{filter->
                    filter.rule = null
                    createFilterAdapter.updateRules(null)
                }
            }
            // look at filter operator
            // if operator has a rule,

           // DAO.filterRuleDAO.deleteFilterRule( it )
            // val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter.uuid )
            // createFilterAdapter.updateFilterRules( filterRules )
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}