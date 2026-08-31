/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.createrule

import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateRuleBinding
import edu.gtri.gpssample.fragments.add_household.CheckboxOptionAdapter
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableDatePickerDialogHost
import edu.gtri.gpssample.ui.compose.ComposableNotificationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableTimePickerDialogHost
import edu.gtri.gpssample.utils.DateUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

class CreateRuleFragment : Fragment()
{ private var _binding: FragmentCreateRuleBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var composableDatePickerDialogHost: ComposableDatePickerDialogHost
    private lateinit var composableTimePickerDialogHost: ComposableTimePickerDialogHost
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost
    private lateinit var composableNotificationDialogHost: ComposableNotificationDialogHost
    private var fieldList = ArrayList<Field>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        sharedViewModel.createRuleModel.fragment = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateRuleBinding.inflate(inflater, container, false)

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
            createRuleFragment = this@CreateRuleFragment
            this.executePendingBindings()
        }

        composableDatePickerDialogHost = ComposableDatePickerDialogHost()
        composableTimePickerDialogHost = ComposableTimePickerDialogHost()
        composableConfirmationDialogHost = ComposableConfirmationDialogHost()
        composableNotificationDialogHost = ComposableNotificationDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableTimePickerDialogHost.Content()
            composableDatePickerDialogHost.Content()
            composableConfirmationDialogHost.Content()
            composableNotificationDialogHost.Content()
        }

        binding.ruleTip.setOnClickListener {
            composableNotificationDialogHost.show(title = resources.getString(R.string.info), message = resources.getString(R.string.rule_hint))
        }

        sharedViewModel.currentConfiguration?.value?.let { config ->
            sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                sharedViewModel.createRuleModel.currentRule?.value?.let { rule ->
                    fieldList = sharedViewModel.createStudyModel.fieldList

                    // for an existing rule, figure out which field was selected

                    getField( rule.fieldUuid )?.let { field->
                        for (i in 0..fieldList.size-1)
                        {
                            if (fieldList[i].uuid == field.uuid)
                            {
                                sharedViewModel.createRuleModel.ruleFieldPosition.value = i

                                if (field.type == FieldType.Text || field.type == FieldType.Number)
                                {
                                    binding.textValueEditText.visibility = View.VISIBLE
                                    binding.dropdownValueSpinner.visibility = View.GONE
                                    binding.dateValueTextView.visibility = View.GONE
                                    binding.checkboxValueRecyclerView.visibility = View.GONE
                                }
                                else if (field.type == FieldType.Dropdown)
                                {
                                    binding.textValueEditText.visibility = View.GONE
                                    binding.dropdownValueSpinner.visibility = View.VISIBLE
                                    binding.dateValueTextView.visibility = View.GONE
                                    binding.checkboxValueRecyclerView.visibility = View.GONE

                                    val items = ArrayList<String>()

                                    for (j in 0..field.fieldOptions.size-1)
                                    {
                                        val fieldOption = field.fieldOptions[j]
                                        items.add( fieldOption.name )

                                        if (rule.value == fieldOption.name)
                                        {
                                            sharedViewModel.createRuleModel.dropdownPosition.value = j
                                        }
                                    }

                                    binding.dropdownValueSpinner.adapter = ArrayAdapter<String>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, items )
                                }
                                else if (field.type == FieldType.Date)
                                {
                                    binding.textValueEditText.visibility = View.GONE
                                    binding.dropdownValueSpinner.visibility = View.GONE
                                    binding.dateValueTextView.visibility = View.VISIBLE
                                    binding.checkboxValueRecyclerView.visibility = View.GONE

                                    rule.value.toLongOrNull()?.let { unixTime ->
                                        if (field.date && !field.time)
                                        {
                                            binding.dateValueTextView.setText( DateUtils.dateString( Date( unixTime ), config.dateFormat ))
                                        }
                                        else if (field.time && !field.date)
                                        {
                                            binding.dateValueTextView.setText( DateUtils.timeString( Date( unixTime ), config.timeFormat ))
                                        }
                                        else
                                        {
                                            binding.dateValueTextView.setText( DateUtils.dateTimeString( Date( unixTime ), config.dateFormat, config.timeFormat))
                                        }
                                    }
                                }
                                else if (field.type == FieldType.Checkbox)
                                {
                                    binding.textValueEditText.visibility = View.GONE
                                    binding.dropdownValueSpinner.visibility = View.GONE
                                    binding.dateValueTextView.visibility = View.GONE
                                    binding.checkboxValueRecyclerView.visibility = View.VISIBLE

                                    binding.checkboxValueRecyclerView.adapter = CheckboxOptionAdapter( true, rule.fieldDataOptions )
                                }

                                break
                            }
                        }
                    }

                    if (rule.uuid.isEmpty())
                    {
                        val field = study.fields[0]
                        if (field.type == FieldType.Checkbox)
                        {
                            rule.operator = Operator.Contains
                        }
                    }

                    rule.operator?.let { operator ->
                        sharedViewModel.createRuleModel.ruleOperationPosition.value = OperatorConverter.toIndex( operator )
                    }

                    binding.checkboxValueRecyclerView.itemAnimator = DefaultItemAnimator()
                    binding.checkboxValueRecyclerView.layoutManager = LinearLayoutManager(context)
                    binding.checkboxValueRecyclerView.recycledViewPool.setMaxRecycledViews(0, 0 )

                    binding.fieldSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
                    {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
                        {
                            val field = fieldList[position]
                            rule.fieldUuid = field.uuid
                            setKeyboardInputType( field )
                            rule.operator = Operator.Equal
                            sharedViewModel.createRuleModel.ruleOperationPosition.value = OperatorConverter.toIndex( Operator.Equal )

                            if (field.type == FieldType.Text || field.type == FieldType.Number)
                            {
                                binding.textValueEditText.visibility = View.VISIBLE
                                binding.dropdownValueSpinner.visibility = View.GONE
                                binding.dateValueTextView.visibility = View.GONE
                                binding.checkboxValueRecyclerView.visibility = View.GONE
                            }
                            else if (field.type == FieldType.Dropdown)
                            {
                                binding.textValueEditText.visibility = View.GONE
                                binding.dropdownValueSpinner.visibility = View.VISIBLE
                                binding.dateValueTextView.visibility = View.GONE
                                binding.checkboxValueRecyclerView.visibility = View.GONE

                                val items = ArrayList<String>()

                                for (j in 0..field.fieldOptions.size-1)
                                {
                                    val fieldOption = field.fieldOptions[j]
                                    items.add( fieldOption.name )

                                    if (rule.value == fieldOption.name)
                                    {
                                        sharedViewModel.createRuleModel.dropdownPosition.value = j
                                    }
                                }

                                binding.dropdownValueSpinner.adapter = ArrayAdapter<String>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, items )
                            }
                            else if (field.type == FieldType.Date)
                            {
                                binding.textValueEditText.visibility = View.GONE
                                binding.dropdownValueSpinner.visibility = View.GONE
                                binding.dateValueTextView.visibility = View.VISIBLE
                                binding.checkboxValueRecyclerView.visibility = View.GONE
                            }
                            else if (field.type == FieldType.Checkbox)
                            {
                                binding.textValueEditText.visibility = View.GONE
                                binding.dropdownValueSpinner.visibility = View.GONE
                                binding.dateValueTextView.visibility = View.GONE
                                binding.checkboxValueRecyclerView.visibility = View.VISIBLE

                                if (rule.fieldDataOptions.isEmpty())
                                {
                                    for (fieldOption in field.fieldOptions)
                                    {
                                        val fieldDataOption = FieldDataOption( fieldOption.name, false )
                                        rule.fieldDataOptions.add( fieldDataOption )
                                    }
                                }

                                rule.operator = Operator.Contains
                                sharedViewModel.createRuleModel.ruleOperationPosition.value = OperatorConverter.toIndex( Operator.Contains )

                                binding.checkboxValueRecyclerView.adapter = CheckboxOptionAdapter( true, rule.fieldDataOptions )
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>)
                        {
                        }
                    }

                    binding.dateValueTextView.setOnClickListener {
                        getField( rule.fieldUuid )?.let { field ->
                            val date = Date()
                            if (field.time && !field.date)
                            {
                                composableTimePickerDialogHost.show(date = date) { date ->
                                    date?.let {
                                        sharedViewModel.currentConfiguration?.value?.let { config ->
                                            sharedViewModel.createRuleModel.currentRule?.value?.let { rule ->
                                                val unixTime = date.time
                                                rule.value = unixTime.toString()

                                                if (field.date && field.time)
                                                {
                                                    binding.dateValueTextView.setText( DateUtils.dateTimeString( Date( unixTime ), config.dateFormat, config.timeFormat))
                                                }
                                                else if (field.time)
                                                {
                                                    binding.dateValueTextView.setText( DateUtils.timeString( Date( unixTime ), config.timeFormat))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else
                            {
                                composableDatePickerDialogHost.show(date = date) { date ->
                                    date?.let {
                                        sharedViewModel.currentConfiguration?.value?.let { config ->
                                            sharedViewModel.createRuleModel.currentRule?.value?.let { rule ->
                                                val unixTime = date.time
                                                rule.value = unixTime.toString()

                                                binding.dateValueTextView.setText( DateUtils.dateString( Date( unixTime ), config.dateFormat ))

                                                if (field.time)
                                                {
                                                    composableTimePickerDialogHost.show(date = date) { date ->
                                                        date?.let {
                                                            sharedViewModel.currentConfiguration?.value?.let { config ->
                                                                sharedViewModel.createRuleModel.currentRule?.value?.let { rule ->
                                                                    val unixTime = date.time
                                                                    rule.value = unixTime.toString()

                                                                    if (field.date && field.time)
                                                                    {
                                                                        binding.dateValueTextView.setText( DateUtils.dateTimeString( Date( unixTime ), config.dateFormat, config.timeFormat))
                                                                    }
                                                                    else if (field.time)
                                                                    {
                                                                        binding.dateValueTextView.setText( DateUtils.timeString( Date( unixTime ), config.timeFormat))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    binding.dropdownValueSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
                    {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
                        {
                            getField( rule.fieldUuid )?.let { field ->
                                val fieldOption = field.fieldOptions[position]
                                rule.value = fieldOption.name
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>)
                        {
                        }
                    }

                    binding.deleteImageView.setOnClickListener {
                        composableConfirmationDialogHost.show(
                            title = resources.getString(R.string.please_confirm),
                            message = resources.getString(R.string.delete_rule_message),
                            leftButtonText = resources.getString(R.string.no),
                            rightButtonText = resources.getString(R.string.yes),
                            destructive = true
                        ) { selection ->
                            if (selection == resources.getString(R.string.yes)) {
                                sharedViewModel.createRuleModel.deleteSelectedRule(study)
                                findNavController().popBackStack()
                            }
                        }
                    }

                    binding.cancelButton.setOnClickListener {
                        findNavController().popBackStack()
                    }

                    binding.saveButton.setOnClickListener {
                        if (rule.name.isEmpty())
                        {
                            Toast.makeText(requireActivity().applicationContext, context?.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        rule.operator?.let { operator ->
                            getField( rule.fieldUuid )?.let { field ->
                                when (field.type)
                                {
                                    FieldType.Text,
                                    FieldType.Checkbox ->
                                    {
                                        when (operator)
                                        {
                                            Operator.LessThan,
                                            Operator.GreaterThan,
                                            Operator.LessThanOrEqual,
                                            Operator.GreaterThanOrEqual ->
                                            {
                                                Toast.makeText(requireActivity().applicationContext, context?.getString(R.string.invalid_operator), Toast.LENGTH_SHORT).show()
                                                return@setOnClickListener
                                            }
                                            else -> {}
                                        }

                                        if (field.type == FieldType.Checkbox)
                                        {
                                            rule.value = ""

                                            for (fieldDataOption in rule.fieldDataOptions)
                                            {
                                                if (fieldDataOption.value)
                                                {
                                                    if (rule.value.isEmpty())
                                                    {
                                                        rule.value = fieldDataOption.name
                                                    }
                                                    else
                                                    {
                                                        rule.value += ",${fieldDataOption.name}"
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    FieldType.Number,
                                    FieldType.Date ->
                                    {
                                        if (operator == Operator.Contains)
                                        {
                                            Toast.makeText(requireActivity().applicationContext, context?.getString(R.string.invalid_operator), Toast.LENGTH_SHORT).show()
                                            return@setOnClickListener
                                        }
                                    }

                                    FieldType.Dropdown ->
                                    {
                                        when (operator)
                                        {
                                            Operator.Contains,
                                            Operator.LessThan,
                                            Operator.GreaterThan,
                                            Operator.LessThanOrEqual,
                                            Operator.GreaterThanOrEqual ->
                                            {
                                                Toast.makeText(requireActivity().applicationContext, context?.getString(R.string.invalid_operator), Toast.LENGTH_SHORT).show()
                                                return@setOnClickListener
                                            }
                                            else -> {}
                                        }
                                    }

                                    else -> {}
                                }
                            }
                        }

                        if (rule.isSubsetRule)
                        {
                            sharedViewModel.addSubsetRule()
                        }
                        else
                        {
                            sharedViewModel.addPrimaryRule()
                        }

                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.CreateRuleFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun getField( uuid: String ) : Field?
    {
        sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
            for (field in study.fields)
            {
                if (field.uuid == uuid)
                {
                    return field
                }
                else
                {
                    field.fields?.let { blockFields ->
                        for (blockField in blockFields)
                        {
                            if (blockField.uuid == uuid)
                            {
                                return blockField
                            }
                        }
                    }
                }
            }
        }

        return null
    }

    fun setKeyboardInputType( field: Field )
    {
        if (field.type == FieldType.Number)
        {
            if (field.integerOnly)
            {
                binding.textValueEditText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            else
            {
                binding.textValueEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
        }
        else
        {
            binding.textValueEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
    }

    override fun onDestroyView()
    {
        sharedViewModel.createRuleModel.fragment = null
        _binding = null

        super.onDestroyView()
    }
}