/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.subset_sample

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentSubsetSampleBinding
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class SubsetSampleFragment : Fragment()
{
    private lateinit var study: Study
    private var _binding: FragmentSubsetSampleBinding? = null
    private val binding get() = _binding!!
    private lateinit var subsetSampleAdapter: SubsetSampleAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentSubsetSampleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        subsetSampleAdapter = SubsetSampleAdapter(activity!!)
        subsetSampleAdapter.didSelectRule = this::didSelectRule
        subsetSampleAdapter.didSelectFilter = this::didSelectFilter
        subsetSampleAdapter.shouldAddRule = this::shouldAddRule
        subsetSampleAdapter.shouldAddFilter = this::shouldAddFilter

        sharedViewModel.createStudyModel.currentStudy?.value?.let { study->
            this.study = study
            subsetSampleAdapter.updateStudy( study )
        }

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = sharedViewModel
        }

        binding.expandableListView.setAdapter(subsetSampleAdapter)
        binding.expandableListView.setChildDivider(getResources().getDrawable(R.color.clear))

        binding.saveButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.SubsetSampleFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun shouldAddRule()
    {
        sharedViewModel.createStudyModel.currentStudy?.value?.let{study ->
            if(study.fields.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.create_field_rule_message), Toast.LENGTH_SHORT).show()
            }
            else
            {
                sharedViewModel.createRuleModel.createNewRule(study)
                findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment )
            }
        }
    }

    private fun shouldAddFilter()
    {
        val bundle = Bundle()
        sharedViewModel.createStudyModel.currentStudy?.value?.let{study ->
            if(study.rules.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.create_rule_filter_message), Toast.LENGTH_SHORT).show()
            }else
            {
                sharedViewModel.createFilterModel.createNewFilter()
                sharedViewModel.createFilterModel.createFilterAdapter.updateRules(null)
                findNavController().navigate( R.id.action_navigate_to_CreateFilterFragment, bundle )
            }
        }
    }

    private fun didSelectRule( rule: Rule )
    {
        val bundle = Bundle()
        sharedViewModel.setSelectedRule(rule)
        findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment, bundle )
    }

    private fun didSelectFilter( filter: Filter )
    {
        val bundle = Bundle()
        sharedViewModel.createFilterModel.setSelectedFilter(filter)
        findNavController().navigate( R.id.action_navigate_to_CreateFilterFragment, bundle )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}