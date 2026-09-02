/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.create_study

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.constants.SamplingMethod
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldOption
import edu.gtri.gpssample.databinding.FragmentCreateStudyBinding
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableNotificationDialogHost
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.UUID

enum class DeleteMode(val value : Int)
{
    deleteStudyTag (1),
}

class CreateStudyFragment : Fragment()
{
    private lateinit var study: Study
    private var _binding: FragmentCreateStudyBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var composableNotificationDialogHost: ComposableNotificationDialogHost
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm

        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("default", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean( Keys.kDeveloperMode.value, false ))
        {
            setHasOptionsMenu(true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateStudyBinding.inflate(inflater, container, false)
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
            createStudyFragment = this@CreateStudyFragment
        }

        composableNotificationDialogHost = ComposableNotificationDialogHost()
        composableConfirmationDialogHost = ComposableConfirmationDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableNotificationDialogHost.Content()
            composableConfirmationDialogHost.Content()
        }

        ArrayAdapter.createFromResource(
            requireActivity(),
            R.array.sampling_methods,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.samplingMethodSpinner.adapter = adapter
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
            this.study = study
        } ?: run {
            binding.deleteImageView.visibility = View.GONE
        }

        sharedViewModel.createStudyModel.samplingMethod.observe( viewLifecycleOwner, androidx.lifecycle.Observer { samplingMethod ->
            when(study.samplingMethod)
            {
                SamplingMethod.SimpleRandom -> {
                    binding.sampleSizeTextView.text = resources.getString(R.string.simple_random_sampling_label)
                }
                SamplingMethod.Cluster -> {
                    binding.sampleSizeTextView.text = resources.getString(R.string.cluster_sampling_label)
                }
                else -> {
                }
            }
        })

        binding.samplingMethodTip.setOnClickListener {
            var samplingHint = resources.getString( R.string.sampling_hint_1_11 ) + "\n\n"
            samplingHint += resources.getString( R.string.sampling_hint_2_11 ) + "\n"
            samplingHint += resources.getString( R.string.sampling_hint_3_11 ) + "\n\n"
            samplingHint += resources.getString( R.string.sampling_hint_4_11 ) + "\n"
            samplingHint += resources.getString( R.string.sampling_hint_5_11 ) + "\n\n"
            samplingHint += resources.getString( R.string.sampling_hint_6_11 ) + "\n"
            samplingHint += resources.getString( R.string.sampling_hint_7_11 ) + "\n\n"
            samplingHint += resources.getString( R.string.sampling_hint_8_11 ) + "\n"
            samplingHint += resources.getString( R.string.sampling_hint_9_11 ) + "\n\n"
            samplingHint += resources.getString( R.string.sampling_hint_10_11 ) + "\n"
            samplingHint += resources.getString( R.string.sampling_hint_11_11 )

            composableNotificationDialogHost.show(title = resources.getString(R.string.info), message = samplingHint )
        }

        binding.deleteImageView.setOnClickListener {
            composableConfirmationDialogHost.show(
                title = resources.getString(R.string.please_confirm),
                message = resources.getString(R.string.delete_study_message),
                leftButtonText = resources.getString(R.string.no),
                rightButtonText = resources.getString(R.string.yes),
                destructive = true
            ) { selection ->
                if (selection == resources.getString(R.string.yes)) {
                    sharedViewModel.deleteCurrentStudy()
                    findNavController().popBackStack()
                }
            }
        }

        binding.primarySampleButton.setOnClickListener {
            if (study.samplingMethod == SamplingMethod.Strata)
            {
                findNavController().navigate( R.id.action_navigate_to_StrataSampleFragment )
            }
            else
            {
                findNavController().navigate( R.id.action_navigate_to_PrimarySampleFragment )
            }
        }

        binding.saveButton.setOnClickListener {
            if (study.name.isEmpty())
            {
                Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (study.sampleSize == 0)
            {
                Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.sample_size_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            study.version = UUID.randomUUID().toString()

            sharedViewModel.addStudy()

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.CreateStudyFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_auto_generate_study, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_autogen_study ->
            {
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    if (config.studies.isEmpty())
                    {
                        composableConfirmationDialogHost.show(
                            title = resources.getString(R.string.please_confirm),
                            message = "Auto generate the study?",
                            leftButtonText = resources.getString(R.string.no),
                            rightButtonText = resources.getString(R.string.yes),
                            destructive = true
                        ) { selection ->
                            if (selection == resources.getString(R.string.yes)) {
                                val studyName = "Test-Study"
                                val study = Study( studyName, SamplingMethod.Cluster, 10000, SampleType.NumberHouseholds )

                                val noteField = Field( null, 1, "Note", FieldType.Note, false, false, false, false, false, false, null, null,study.uuid)
                                val textField = Field( null, 2, "Text", FieldType.Text, false, false, false, false, false, false, null, null,study.uuid)
                                val numberField = Field( null, 3, "Number", FieldType.Number, false, false, true, false, false, false, null, null,study.uuid)
                                val dateField = Field( null, 4, "Date", FieldType.Date, false, false, false, false, true, false, null, null,study.uuid)
                                val checkBoxField = Field( null, 5, "Checkbox", FieldType.Checkbox, false, false, false, false, false, false, null, null,study.uuid)
                                val dropDownField = Field( null, 6, "Dropdown", FieldType.Dropdown, false, false, false, false, false, false, null, null,study.uuid)

                                checkBoxField.fieldOptions.add( FieldOption("CB 1" ))
                                checkBoxField.fieldOptions.add( FieldOption("CB 2" ))
                                checkBoxField.fieldOptions.add( FieldOption("CB 3" ))

                                dropDownField.fieldOptions.add( FieldOption("DD 1" ))
                                dropDownField.fieldOptions.add( FieldOption("DD 2" ))
                                dropDownField.fieldOptions.add( FieldOption("DD 3" ))

                                study.fields.add( noteField )
                                study.fields.add( textField )
                                study.fields.add( numberField )
                                study.fields.add( dateField )
                                study.fields.add( checkBoxField )
                                study.fields.add( dropDownField )

                                config.studies.add( study )

                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView()
    {
        _binding = null

        super.onDestroyView()
    }
}