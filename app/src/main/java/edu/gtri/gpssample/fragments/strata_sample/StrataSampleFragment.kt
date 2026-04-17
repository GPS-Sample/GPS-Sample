/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.strata_sample

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.databinding.FragmentStrataSampleBinding
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Strata
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.dialogs.AddStrataDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class StrataSampleFragment : Fragment()
{
    private lateinit var study: Study
    private var _binding: FragmentStrataSampleBinding? = null
    private val binding get() = _binding!!
    private lateinit var strataSampleAdapter: StrataSampleAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentStrataSampleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        strataSampleAdapter = StrataSampleAdapter(activity!!)
        strataSampleAdapter.didSelectStrata = this::didSelectStrata
        strataSampleAdapter.didSelectField = this::didSelectField
        strataSampleAdapter.didSelectRule = this::didSelectRule
        strataSampleAdapter.didSelectFilter = this::didSelectFilter
        strataSampleAdapter.shouldAddStrata = this::shouldAddStrata
        strataSampleAdapter.shouldAddField = this::shouldAddField
        strataSampleAdapter.shouldAddRule = this::shouldAddRule
        strataSampleAdapter.shouldAddFilter = this::shouldAddFilter

        sharedViewModel.createStudyModel.currentStudy?.value?.let{ study->
            this.study = study
            strataSampleAdapter.updateStudy( study )
        }

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = sharedViewModel
        }

        binding.expandableListView.setAdapter(strataSampleAdapter)
        binding.expandableListView.setChildDivider(getResources().getDrawable(R.color.clear))

        binding.saveButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.StrataSampleFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun shouldAddStrata()
    {
        val strata = Strata(study.uuid,"", 0, SampleType.NumberHouseholds )

        AddStrataDialog(requireActivity(), strata, false ) { buttonPressed ->
            if (buttonPressed == AddStrataDialog.ButtonPress.Save)
            {
                study.stratas.add( strata )
                strataSampleAdapter.updateStudy( study )
            }
        }
    }

    private fun shouldAddField()
    {
        val field = Field( null, study.fields.size+1,"", FieldType.Text, false, false, false, false, false, false, null, null )
        sharedViewModel.createFieldModel.setCurrentField( field )
        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment )
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
                sharedViewModel.createRuleModel.createNewPrimaryRule(study)
                findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment )
            }
        }
    }

    private fun shouldAddFilter()
    {
        val bundle = Bundle()
        sharedViewModel.createStudyModel.currentStudy?.value?.let{study ->
            if(study.primaryRules.isEmpty())
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

    private fun didSelectStrata( strata: Strata )
    {
        AddStrataDialog(requireActivity(), strata, true ) { buttonPressed ->
            if (buttonPressed == AddStrataDialog.ButtonPress.Save)
            {
                strataSampleAdapter.updateStudy( study )
            }
            else if (buttonPressed == AddStrataDialog.ButtonPress.Delete)
            {
                study.stratas.remove( strata )
                strataSampleAdapter.updateStudy( study )
            }
        }
    }

    private fun didSelectField( field: Field )
    {
        sharedViewModel.createFieldModel.setCurrentField(field)
        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment )
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